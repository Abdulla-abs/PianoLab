package com.example.pianolab.feature.beat.engine;

import android.content.Context;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.SoundPool;
import android.os.Build;
import android.util.Log;

import com.example.pianolab.R;
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

    // 音频相关
    private final Context appContext;
    private SoundPool soundPool;
    private int soundClickId = 0;
    private int soundBeatId = 0;
    private volatile boolean soundsLoaded = false;

    public BeatEngineImpl(Context context) {
        this.appContext = context.getApplicationContext();
        initSoundPool();
    }

    private void initSoundPool() {
        try {
            if (soundPool != null) return;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                AudioAttributes attrs = new AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build();
                soundPool = new SoundPool.Builder()
                        .setMaxStreams(2)
                        .setAudioAttributes(attrs)
                        .build();
            } else {
                soundPool = new SoundPool(2, AudioManager.STREAM_MUSIC, 0);
            }

            soundPool.setOnLoadCompleteListener((sp, sampleId, status) -> {
                if (status == 0) {
                    soundsLoaded = true;
                }
            });

            // 加载资源（假设资源已放入 res/raw）
            soundClickId = soundPool.load(appContext, R.raw.metronome_click, 1);
            soundBeatId = soundPool.load(appContext, R.raw.metronome_beat, 1);
        } catch (Throwable t) {
            Log.e("BeatEngineImpl", "initSoundPool error", t);
        }
    }

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

                    // 播放音效：重拍用 metronome_beat，其他拍用 metronome_click
                    try {
                        if (soundsLoaded && soundPool != null) {
                            int playId = (idx == 0) ? soundBeatId : soundClickId;
                            if (playId != 0) {
                                soundPool.play(playId, 1f, 1f, 1, 0, 1f);
                            }
                        }
                    } catch (Throwable t) {
                        Log.w("BeatEngineImpl", "sound play failed", t);
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
        try {
            if (soundPool != null) {
                soundPool.release();
                soundPool = null;
            }
        } catch (Throwable ignored) {
    }
    }
}
