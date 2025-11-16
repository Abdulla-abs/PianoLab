package com.example.pianolab.feature.beat.engine;

import android.content.Context;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.util.Log;

import com.example.pianolab.feature.beat.model.BeatSettings;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

// 声音音量超参数：强拍比弱拍更响
//strongBeatVolume，weakBeatVolume
public class BeatEngineImpl implements BeatEngine {

    private final Object lock = new Object();
    private ScheduledExecutorService scheduler;
    private volatile boolean running = false;
    private volatile BeatListener listener;

    private volatile int bpm = 120;
    private volatile int beatsPerMeasure = 4;
    private final AtomicInteger beatIndex = new AtomicInteger(0);
    private final AtomicLong absoluteBeatCounter = new AtomicLong(0L);
    private final AtomicLong lastPlayedBeat = new AtomicLong(-1L);
    private Thread beatThread;

    // 音频相关
    private final Context appContext;
    //    private SoundPool soundPool;
    //    private int soundClickId = 0;
    //    private int soundBeatId = 0;
    //    private volatile boolean soundsLoaded = false;
    //    private final java.util.concurrent.atomic.AtomicInteger loadCount = new java.util.concurrent.atomic.AtomicInteger(0);
    // 使用程序合成的 PCM 声音，避免 SoundPool 在高 BPM/设备差异下表现不稳定
    // 使用 AudioTrack 池以避免对同一实例频繁重置带来的抖动
    private static final int AUDIO_POOL_SIZE = 4;
    private AudioTrack[] weakTracks = new AudioTrack[AUDIO_POOL_SIZE];
    private AudioTrack[] strongTracks = new AudioTrack[AUDIO_POOL_SIZE];
    private AudioTrack countdownTrack; // 单例足够
    private int weakPlayIndex = 0;
    private int strongPlayIndex = 0;
    private volatile boolean generatedLoaded = false;
    private int sampleRate = 44100;
    // 防止短时间内重复播放（例如调度重叠）导致的双响，记录上次播放时间（纳秒）
    private volatile long lastPlayNs = 0L;
    // 起始时间戳（纳秒），用于计算每拍的精确预期时间
    private volatile long startTimeNs = 0L;

    // 音频路径的延迟补偿（纳秒），可通过 setAudioLatencyMs 调整，默认为 0
    private volatile long audioLatencyNs = 0L;

    // 已简化：不再使用 streaming 混音线程和事件队列，使用静态 AudioTrack 池 + scheduler

    // 预生成的短整型波形（样本为 -32768..32767）
    private short[] weakWave; // 440Hz, 40ms
    private short[] strongWave; // 660Hz, 50ms
    private short[] countdownWave; // 440Hz, 60ms
    private int weakLen, strongLen, countdownLen;
    private final Object audioLock = new Object();

    private enum WaveType {SINE, TRIANGLE, SQUARE}

    private byte[] generatePcm(double freq, int durationMs, WaveType type) {
        int frames = (int) ((durationMs / 1000.0) * sampleRate);
        int totalSamples = frames; // mono
        byte[] pcm = new byte[totalSamples * 2]; // 16-bit little endian
        double twoPiF = 2.0 * Math.PI * freq;
        for (int i = 0; i < totalSamples; i++) {
            double t = i / (double) sampleRate;
            double value = 0.0;
            switch (type) {
                case SINE:
                    value = Math.sin(twoPiF * t);
                    break;
                case TRIANGLE:
                    // triangle wave from -1..1
                    double period = sampleRate / freq;
                    double pos = (i % (int) period) / period;
                    if (pos < 0.25) value = 4 * pos;
                    else if (pos < 0.75) value = 2 - 4 * pos;
                    else value = -4 + 4 * pos;
                    break;
                case SQUARE:
                    value = (Math.sin(twoPiF * t) >= 0) ? 1.0 : -1.0;
                    break;
            }
            // 轻微包络（线性淡出/淡入）以减少爆音
            double env = 1.0;
            int attack = Math.max(1, (int) (sampleRate * 0.005)); // 5ms 攻击
            int release = Math.max(1, (int) (sampleRate * 0.01)); // 10ms 释放
            if (i < attack) env = i / (double) attack;
            if (i >= totalSamples - release) env = (totalSamples - i) / (double) release;
            short samp = (short) Math.max(Short.MIN_VALUE, Math.min(Short.MAX_VALUE, (int) (value * env * Short.MAX_VALUE * 0.8)));
            pcm[i * 2] = (byte) (samp & 0xff);
            pcm[i * 2 + 1] = (byte) ((samp >> 8) & 0xff);
        }
        return pcm;
    }

    private short[] generatePcmShort(double freq, int durationMs, WaveType type) {
        int frames = (int) ((durationMs / 1000.0) * sampleRate);
        short[] out = new short[frames];
        double twoPiF = 2.0 * Math.PI * freq;
        for (int i = 0; i < frames; i++) {
            double t = i / (double) sampleRate;
            double value = 0.0;
            switch (type) {
                case SINE:
                    value = Math.sin(twoPiF * t);
                    break;
                case TRIANGLE: {
                    double period = sampleRate / freq;
                    double pos = (i % (int) period) / period;
                    if (pos < 0.25) value = 4 * pos;
                    else if (pos < 0.75) value = 2 - 4 * pos;
                    else value = -4 + 4 * pos;
                }
                break;
                case SQUARE:
                    value = (Math.sin(twoPiF * t) >= 0) ? 1.0 : -1.0;
                    break;
            }
            double env = 1.0;
            int attack = Math.max(1, (int) (sampleRate * 0.005));
            int release = Math.max(1, (int) (sampleRate * 0.01));
            if (i < attack) env = i / (double) attack;
            if (i >= frames - release) env = (frames - i) / (double) release;
            out[i] = (short) (Math.max(Short.MIN_VALUE, Math.min(Short.MAX_VALUE, (int) (value * env * Short.MAX_VALUE * 0.85))));
        }
        return out;
    }

    private byte[] shortsToBytes(short[] s) {
        byte[] b = new byte[s.length * 2];
        for (int i = 0; i < s.length; i++) {
            b[i * 2] = (byte) (s[i] & 0xff);
            b[i * 2 + 1] = (byte) ((s[i] >> 8) & 0xff);
        }
        return b;
    }

    private AudioTrack createStaticAudioTrack(byte[] pcm) {
        try {
            int bufSize = pcm.length;
            AudioTrack at = new AudioTrack(AudioManager.STREAM_MUSIC,
                    sampleRate,
                    AudioFormat.CHANNEL_OUT_MONO,
                    AudioFormat.ENCODING_PCM_16BIT,
                    bufSize,
                    AudioTrack.MODE_STATIC);
            int written = at.write(pcm, 0, pcm.length);
            if (written > 0) {
                // 准备好但不自动播放
                at.setVolume(1.0f);
            }
            return at;
        } catch (Throwable t) {
            Log.w("BeatEngineImpl", "createStaticAudioTrack failed", t);
            return null;
        }
    }

    private void initSoundPool() {
        // 旧的 SoundPool 实现已弃用，改为程序生成音频 initGeneratedAudio 已在构造时调用
    }

    public BeatEngineImpl(Context context) {
        this.appContext = context.getApplicationContext();
        initGeneratedAudio();
    }

    private void initGeneratedAudio() {
        try {
            weakWave = generatePcmShort(440.0, 40, WaveType.SINE);
            strongWave = generatePcmShort(660.0, 50, WaveType.TRIANGLE);
            countdownWave = generatePcmShort(440.0, 60, WaveType.SQUARE);
            weakLen = weakWave.length;
            strongLen = strongWave.length;
            countdownLen = countdownWave.length;
            for (int i = 0; i < AUDIO_POOL_SIZE; i++) {
                weakTracks[i] = createStaticAudioTrack(shortsToBytes(weakWave));
                strongTracks[i] = createStaticAudioTrack(shortsToBytes(strongWave));
            }
            countdownTrack = createStaticAudioTrack(shortsToBytes(countdownWave));
            generatedLoaded = true;
            Log.d("BeatEngineImpl", "initGeneratedAudio success. weakLen=" + weakLen + " strongLen=" + strongLen + " countdownLen=" + countdownLen);
        } catch (Throwable t) {
            Log.e("BeatEngineImpl", "initGeneratedAudio error", t);
            generatedLoaded = false;
        }
    }

    @Override
    public void start(BeatSettings settings) {
        if (settings == null) settings = new BeatSettings();
        synchronized (lock) {
            bpm = settings.getBpm();
            beatsPerMeasure = settings.getBeatsPerMeasure();
            if (running) {
                stop();
            }

            running = true;
            absoluteBeatCounter.set(0L);
            lastPlayedBeat.set(-1L);

            // 使用 ScheduledExecutorService 做单线程精简调度：短周期轮询（非阻塞）
            final long periodNs = Math.max(1L, Math.round(60_000_000_000.0 / bpm));
            final long tickNs = 4_000_000L; // 4ms tick，

            scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
                Thread t = new Thread(r, "BeatSchedulerThread");
                t.setDaemon(true);
                try {
                    android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_URGENT_AUDIO);
                } catch (Throwable ignored) {
                }
                return t;
            });

            // 轮询任务：非常快，不阻塞；基于时间基准计算要触发的拍（包含提前量 audioLatencyNs）并播放
            final java.util.concurrent.atomic.AtomicLong beatCounter = absoluteBeatCounter;
            final Runnable ticker = new Runnable() {
                private boolean first = true;

                @Override
                public void run() {
                    try {
                        long now = System.nanoTime();
                        if (first) {
                            // 首次运行时使用首次 tick 的时间作为基准
                            startTimeNs = now;
                            first = false;
                        }

                        // 计算截止时间（将音频链路延迟考虑在内）
                        long effectiveNow = now + audioLatencyNs;
                        long elapsed = effectiveNow - startTimeNs;
                        if (elapsed < 0) return; // 尚未到首次基准

                        long targetBeat = elapsed / periodNs; // floor，应该已触发的最大拍号

                        long last = lastPlayedBeat.get();
                        // 只播放最新应到的那一拍，避免在一次 tick 内连发多拍（造成“抢拍”感）
                        if (targetBeat > last) {
                            long b = targetBeat;
                            int idx = (int) (b % Math.max(1, beatsPerMeasure));
                            long expectedTs = startTimeNs + b * periodNs;
                            if (idx == 0) playStrongOnce();
                            else playWeakOnce();
                            BeatListener l = listener;
                            if (l != null) {
                                try {
                                    l.onBeat(idx, expectedTs);
                                } catch (Throwable ignored) {
                                }
                                if (idx == 0) {
                                    try {
                                        l.onMeasureStart(expectedTs);
                                    } catch (Throwable ignored) {
                                    }
                                }
                            }
                            // 记录 drift（实际触发时间 - 预期时间），单位 ms，便于后续校准
                            long nowNsForDrift = System.nanoTime();
                            long driftNs = nowNsForDrift - expectedTs;
                            double driftMs = driftNs / 1_000_000.0;
                            Log.d("BeatEngineImpl", "Beat drift idx=" + idx + " expectedNs=" + expectedTs + " nowNs=" + nowNsForDrift + " driftMs=" + String.format("%.3f", driftMs));
                             lastPlayedBeat.set(b);
                             // 使计数器跳到下一拍
                             beatCounter.set(b + 1);
                         }

                        // 如果目标拍远远领先 last（例如丢失很多拍），在下一次 tick 继续追赶
                    } catch (Throwable t) {
                        Log.e("BeatEngineImpl", "Ticker error", t);
                    }
                }
            };

            // 使用 scheduleWithFixedDelay 避免进程从 cached 恢复时出现突发大量执行的危险
            scheduler.scheduleWithFixedDelay(ticker, 0L, tickNs, java.util.concurrent.TimeUnit.NANOSECONDS);
        }
    }

    // 使用静态池播放强拍
    private void playStrongOnce() {
        synchronized (audioLock) {
            try {
                int idx = (strongPlayIndex++) % AUDIO_POOL_SIZE;
                AudioTrack at = strongTracks[idx];
                if (at == null) {
                    at = createStaticAudioTrack(shortsToBytes(strongWave));
                    strongTracks[idx] = at;
                }
                if (at != null) {
                    try {
                        at.stop();
                    } catch (Throwable ignored) {
                    }
                    try {
                        at.setPlaybackHeadPosition(0);
                    } catch (Throwable ignored) {
                    }
                    try {
                        at.play();
                    } catch (Throwable t) {
                        Log.w("BeatEngineImpl", "playStrongOnce play failed", t);
                    }
                }
            } catch (Throwable t) {
                Log.w("BeatEngineImpl", "playStrongOnce error", t);
            }
        }
    }

    // 使用静态池播放弱拍
    private void playWeakOnce() {
        synchronized (audioLock) {
            try {
                int idx = (weakPlayIndex++) % AUDIO_POOL_SIZE;
                AudioTrack at = weakTracks[idx];
                if (at == null) {
                    at = createStaticAudioTrack(shortsToBytes(weakWave));
                    weakTracks[idx] = at;
                }
                if (at != null) {
                    try {
                        at.stop();
                    } catch (Throwable ignored) {
                    }
                    try {
                        at.setPlaybackHeadPosition(0);
                    } catch (Throwable ignored) {
                    }
                    try {
                        at.play();
                    } catch (Throwable t) {
                        Log.w("BeatEngineImpl", "playWeakOnce play failed", t);
                    }
                }
            } catch (Throwable t) {
                Log.w("BeatEngineImpl", "playWeakOnce error", t);
            }
        }
    }

    @Override
    public void stop() {
        synchronized (lock) {
            running = false;
            // 关闭 scheduler
            if (scheduler != null) {
                try {
                    scheduler.shutdownNow();
                } catch (Throwable ignored) {
                }
                scheduler = null;
            }
            absoluteBeatCounter.set(0L);
            lastPlayedBeat.set(-1L);
            startTimeNs = 0L;
            // 停止并重置静态 AudioTrack 的播放头，但不释放资源以便 restart 时复用
            for (int i = 0; i < AUDIO_POOL_SIZE; i++) {
                if (weakTracks[i] != null) {
                    try {
                        weakTracks[i].stop();
                        weakTracks[i].setPlaybackHeadPosition(0);
                    } catch (Throwable ignored) {
                    }
                }
                if (strongTracks[i] != null) {
                    try {
                        strongTracks[i].stop();
                        strongTracks[i].setPlaybackHeadPosition(0);
                    } catch (Throwable ignored) {
                    }
                }
            }
            if (countdownTrack != null) {
                try {
                    countdownTrack.stop();
                    countdownTrack.setPlaybackHeadPosition(0);
                } catch (Throwable ignored) {
                }
            }
        }

        // 无需释放遗留的 streaming 线程资源
    }

    // 移除此处重复的、截断的 stop() 实现并替换为 release()，负责停止线程并释放音频资源
    @Override
    public void release() {
        // 先停止运行
        stop();
        listener = null;

        // 线程已由 stop() 关闭；无需在此再次 join 特定线程（实现已简化为使用 scheduler）
        synchronized (lock) {
            beatThread = null;
            // audioThread 已移除
        }

        // 释放静态音频资源
        for (int i = 0; i < AUDIO_POOL_SIZE; i++) {
            if (weakTracks[i] != null) {
                try { weakTracks[i].release(); } catch (Throwable ignored) {}
                weakTracks[i] = null;
            }
            if (strongTracks[i] != null) {
                try { strongTracks[i].release(); } catch (Throwable ignored) {}
                strongTracks[i] = null;
            }
        }
        if (countdownTrack != null) {
            try { countdownTrack.release(); } catch (Throwable ignored) {}
            countdownTrack = null;
        }
    }

    @Override
    public void setBpm(int bpm) {
        this.bpm = bpm;
    }

    @Override
    public void setBeatsPerMeasure(int beatsPerMeasure) {
        this.beatsPerMeasure = beatsPerMeasure;
    }

    @Override
    public void setListener(BeatListener listener) {
        this.listener = listener;
    }

    @Override
    public boolean isRunning() {
        return this.running;
    }
}
