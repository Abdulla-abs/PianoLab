package com.example.pianolab.feature.beat.engine;

import android.util.Log;

import com.example.pianolab.feature.beat.model.BeatSettings;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;


public class BeatEngineImpl implements BeatEngine {

    private final Object lock = new Object();
    private ScheduledExecutorService scheduler;
    private volatile boolean running = false;
    private volatile BeatListener listener;

    private volatile int bpm = 120;
    private volatile int beatsPerMeasure = 4;
    private final AtomicInteger beatIndex = new AtomicInteger(0);

    @Override
    public void start(BeatSettings settings) {
        if (settings == null) settings = new BeatSettings();
        synchronized (lock) {
            bpm = settings.getBpm();
            beatsPerMeasure = settings.getBeatsPerMeasure();
            if (running) {
                // restart with new settings
                stop();
            }
            scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
                Thread t = new Thread(r, "BeatEngineThread");
                t.setDaemon(true);
                return t;
            });

            long periodNs = 60_000_000_000L / bpm; // 每拍间隔（纳秒），移除冗余强转
            // 初始 beatIndex 从 -1 开始，这样第一次 runnable 时会变为 0
            beatIndex.set(-1);

            running = true;

            scheduler.scheduleWithFixedDelay(() -> {
                try {
                    //此处为匿名类的语法糖
                    int idx = beatIndex.updateAndGet(i -> (i + 1) % Math.max(1, beatsPerMeasure));

                    long ts = System.nanoTime();
                    BeatListener l = listener;
                    if (l != null) {
                        l.onBeat(idx, ts);
                        if (idx == 0) {
                            l.onMeasureStart(ts);
                        }
                    }
                } catch (Throwable t) {
                    // 使用 Log 记录异常，避免直接 printStackTrace
                    Log.e("BeatEngineImpl", "Unexpected error in beat task", t);
                }
            }, 0, periodNs, TimeUnit.NANOSECONDS); // 初始延迟0，任务结束后延迟periodNs再执行
        }
    }

    @Override
    public void stop() {
        synchronized (lock) {
            if (scheduler != null) {
                try {
                    scheduler.shutdownNow();
                } catch (Throwable ignored) {
                }
                scheduler = null;
            }
            running = false;
            beatIndex.set(0);
        }
    }

    @Override
    public void setBpm(int bpm) {
        if (bpm < BeatSettings.MIN_BPM) bpm = BeatSettings.MIN_BPM;
        if (bpm > BeatSettings.MAX_BPM) bpm = BeatSettings.MAX_BPM;
        synchronized (lock) {
            this.bpm = bpm;
            if (running) {
                // 重新启动以应用新的间隔
                BeatSettings s = new BeatSettings(this.bpm, this.beatsPerMeasure);
                start(s);
            }
        }
    }

    @Override
    public void setBeatsPerMeasure(int beatsPerMeasure) {
        if (beatsPerMeasure <= 0) beatsPerMeasure = 4;
        synchronized (lock) {
            this.beatsPerMeasure = beatsPerMeasure;
            // 不重启调度线程，只更新计数，下一次调度会使用新的值
        }
    }

    @Override
    public void setListener(BeatListener listener) {
        this.listener = listener;
    }

    @Override
    public boolean isRunning() {
        return running;
    }

    @Override
    public void release() {
        stop();
        listener = null;
    }
}
