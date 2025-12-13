package com.example.pianolab.feature.beat.engine;

import android.content.Context;
import android.media.AudioAttributes;
import android.media.AudioFormat;

import android.media.AudioTrack;
import android.util.Log;

import com.example.pianolab.R;
import com.example.pianolab.feature.beat.model.BeatSettings;
import com.example.pianolab.utils.BeatHelper;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.atomic.AtomicLong;


public class BeatEngineImpl implements BeatEngine {

    private final Object lock = new Object();
    private ScheduledExecutorService scheduler;
    private volatile boolean running = false;
    private volatile boolean accentEnabled = true;
    private volatile BeatListener listener;

    private volatile int bpm = 120;
    private volatile int beatsPerMeasure = 4;
    private volatile int specialRhythmId = 0;
    private volatile int baseBeat = 4;
    private volatile int toneType = BeatSettings.TONE_ELECTRONIC;
    private final AtomicLong absoluteBeatCounter = new AtomicLong(0L);
    private final AtomicLong lastPlayedBeat = new AtomicLong(-1L);

    public void setAccentEnabled(boolean accentEnabled) {
        this.accentEnabled = accentEnabled;
    }

    // 音频相关
    private final Context appContext;

    private static final int AUDIO_POOL_SIZE = 4;
    private AudioTrack[] weakTracks = new AudioTrack[AUDIO_POOL_SIZE];
    private AudioTrack[] strongTracks = new AudioTrack[AUDIO_POOL_SIZE];
    private AudioTrack countdownTrack; // 单例足够
    private int weakPlayIndex = 0;
    private int strongPlayIndex = 0;
    private volatile boolean generatedLoaded = false;
    private int sampleRate = 44100;
    // 起始时间戳（纳秒），用于计算每拍的精确预期时间
    private volatile long startTimeNs = 0L;

    // 音频路径的延迟补偿（纳秒），可通过 setAudioLatencyMs 调整，默认为 0
    private volatile long audioLatencyNs = 65_000_000L;

    private volatile double driftEmaMs = 0.0; // 指数移动平均的漂移，单位 ms
    private static final double DRIFT_EMA_ALPHA = 0.25; // EMA 平滑系数，越大响应越快但噪声更多
    private static final double LATENCY_ADJUST_ALPHA = 0.12;
    private static final double BASE_LATENCY_MS = 65.0; // 基础目标提前量（可调整或通过 setAudioLatencyMs 覆盖）
    private static final long MAX_LATENCY_MS = 300L; // 限制上限，避免过度调整



    // 已简化：不再使用 streaming 混音线程和事件队列，使用静态 AudioTrack 池 + scheduler

    // 预生成的短整型波形（样本为 -32768..32767）
    private byte[] weakPcmElectronic;
    private byte[] strongPcmElectronic;
    private byte[] countdownPcmElectronic;

    private byte[] weakPcmMechanical;
    private byte[] strongPcmMechanical;
    private byte[] countdownPcmMechanical;

    private byte[] weakPcmDrum;
    private byte[] strongPcmDrum;
    private byte[] countdownPcmDrum;

    private byte[] weakPcmMarimba;
    private byte[] strongPcmMarimba;
    private byte[] countdownPcmMarimba;

    private final Object audioLock = new Object();

    private enum WaveType {SINE, TRIANGLE, SQUARE}



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

    private byte[] loadWavPcm(int resId) {
        try (InputStream is = appContext.getResources().openRawResource(resId)) {
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            int nRead;
            byte[] data = new byte[1024];
            while ((nRead = is.read(data, 0, data.length)) != -1) {
                buffer.write(data, 0, nRead);
            }
            buffer.flush();
            byte[] allBytes = buffer.toByteArray();
            // Skip 44 bytes header
            if (allBytes.length > 44) {
                return Arrays.copyOfRange(allBytes, 44, allBytes.length);
            }
            return new byte[0];
        } catch (IOException e) {
            Log.e("BeatEngineImpl", "Error loading wav resource " + resId, e);
            return new byte[0];
        }
    }

    private AudioTrack createStaticAudioTrack(byte[] pcm,float volume) {
        try {
            int bufSize = pcm.length;
            AudioAttributes audioAttributes = new AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA) // 音频用途：媒体（对应旧的 STREAM_MUSIC）
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION) // 音频类型：音效
                    .build();

            AudioFormat audioFormat = new AudioFormat.Builder()
                    .setSampleRate(sampleRate)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .build();
            AudioTrack at = new AudioTrack.Builder()
                    .setAudioAttributes(audioAttributes)
                    .setAudioFormat(audioFormat)
                    .setBufferSizeInBytes(bufSize)
                    .setTransferMode(AudioTrack.MODE_STATIC)
                    .build();
            int written = at.write(pcm, 0, pcm.length);
            if (written > 0) {
                // 准备好但不自动播放
                at.setVolume(volume);
            }
            return at;
        } catch (Throwable t) {
            Log.w("BeatEngineImpl", "createStaticAudioTrack failed", t);
            return null;
        }
    }


    public BeatEngineImpl(Context context) {
        this.appContext = context.getApplicationContext();
        initAudioResources();
    }

    private void initAudioResources() {
        try {
            // 1. Generate Electronic Sounds
            short[] weakWaveShort = generatePcmShort(440.0, 40, WaveType.SINE);
            short[] strongWaveShort = generatePcmShort(660.0, 50, WaveType.TRIANGLE);
            short[] countdownWaveShort = generatePcmShort(440.0, 60, WaveType.SQUARE);

            weakPcmElectronic = shortsToBytes(weakWaveShort);
            strongPcmElectronic = shortsToBytes(strongWaveShort);
            countdownPcmElectronic = shortsToBytes(countdownWaveShort);

            // 2. Load Mechanical Sounds
            weakPcmMechanical = loadWavPcm(R.raw.metronome_click);
            strongPcmMechanical = loadWavPcm(R.raw.metronome_beat);
            countdownPcmMechanical = loadWavPcm(R.raw.countdown);

            // 3. Load Drum Sounds
            weakPcmDrum = loadWavPcm(R.raw.drum_weak);
            strongPcmDrum = loadWavPcm(R.raw.drum_strong);
            countdownPcmDrum = loadWavPcm(R.raw.drum_countdown);

            //4.load Marimba Sounds
            weakPcmMarimba = loadWavPcm(R.raw.marimba_weak);
            strongPcmMarimba = loadWavPcm(R.raw.marimba_strong);
            countdownPcmMarimba = loadWavPcm(R.raw.marimba_countdown);

            // 5. Initialize Tracks with default (Electronic)
            refreshAudioTracks();

            generatedLoaded = true;
            Log.d("BeatEngineImpl", "initAudioResources success.");
        } catch (Throwable t) {
            Log.e("BeatEngineImpl", "initAudioResources error", t);
            generatedLoaded = false;
        }
    }

    private void refreshAudioTracks() {
        synchronized (audioLock) {
            // Release old tracks
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

            // Select source
            byte[] weakSource;
            byte[] strongSource;
            byte[] countdownSource;

            if (toneType == BeatSettings.TONE_MECHANICAL) {
                weakSource = weakPcmMechanical;
                strongSource = strongPcmMechanical;
                countdownSource = countdownPcmMechanical;
            } else if (toneType == BeatSettings.TONE_DRUM) {
                weakSource = weakPcmDrum;
                strongSource = strongPcmDrum;
                countdownSource = countdownPcmDrum;
            } else if (toneType==BeatSettings.TONE_MARIMBA) {
                weakSource = weakPcmMarimba;
                strongSource = strongPcmMarimba;
                countdownSource = countdownPcmMarimba;
            } else {
                weakSource = weakPcmElectronic;
                strongSource = strongPcmElectronic;
                countdownSource = countdownPcmElectronic;
            }

            // Create new tracks
            for (int i = 0; i < AUDIO_POOL_SIZE; i++) {
                weakTracks[i] = createStaticAudioTrack(weakSource, BeatHelper.VOLUME_PARA[toneType][1]);
                strongTracks[i] = createStaticAudioTrack(strongSource, BeatHelper.VOLUME_PARA[toneType][2]);
            }
            countdownTrack = createStaticAudioTrack(countdownSource,BeatHelper.VOLUME_PARA[toneType][0]);
        }
    }

    @Override
    public void start(BeatSettings settings) {
        if (settings == null) settings = new BeatSettings();
        synchronized (lock) {
            bpm = settings.getBpm();
            beatsPerMeasure = settings.getBeatsPerMeasure();
            specialRhythmId = settings.getSpecialRhythmId();
            baseBeat = settings.getBaseBeat();

            // 强制检查 toneType，如果 settings 中的 toneType 与当前不同，或者 tracks 可能未初始化
            if (this.toneType != settings.getToneType()) {
                this.toneType = settings.getToneType();
                refreshAudioTracks();
            } else {
                // 即使 toneType 相同，也要确保 tracks 存在（防止意外释放或未初始化）
                // 简单检查 countdownTrack 是否为空作为标志
                synchronized (audioLock) {
                    if (countdownTrack == null) {
                        refreshAudioTracks();
                    }
                }
            }

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

                        // 只播放最新应到的那一拍，避免在一次 tick 内连发多拍（造成“抢拍”感）
                        long last = lastPlayedBeat.get();
                        if (targetBeat > last) {
                            long b = targetBeat;
                            int idx = (int) (b % Math.max(1, beatsPerMeasure));
                            long expectedTs = startTimeNs + b * periodNs;

                            playBeat(idx, specialRhythmId);

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

// 1) 过滤极端异常值（例如系统休眠/唤醒导致的大偏差）
                            if (Math.abs(driftMs) < 1000.0) { // 忽略超过 1s 的异常点
                                // 2) 更新 EMA（首次直接设置）
                                if (driftEmaMs == 0.0) {
                                    driftEmaMs = driftMs;
                                } else {
                                    driftEmaMs = DRIFT_EMA_ALPHA * driftMs + (1.0 - DRIFT_EMA_ALPHA) * driftEmaMs;
                                }

                                // 3) 计算期望的 latency：基础值 + EMA 漂移（正漂移 -> 实际晚于预期，需要增加提前量）
                                double desiredLatencyMs = BASE_LATENCY_MS + driftEmaMs;

                                // 4) 限制范围并平滑地把 desiredLatency 合并到 audioLatencyNs 上，避免突变
                                desiredLatencyMs = Math.max(0.0, Math.min((double) MAX_LATENCY_MS, desiredLatencyMs));
                                long desiredLatencyNs = (long) (desiredLatencyMs * 1_000_000.0);

                                // 平滑更新 audioLatencyNs（audioLatencyNs 是纳秒）
                                long currentLatencyNs = audioLatencyNs;
                                long newLatencyNs = (long) ((1.0 - LATENCY_ADJUST_ALPHA) * currentLatencyNs + LATENCY_ADJUST_ALPHA * desiredLatencyNs);
                                // 防止负值
                                audioLatencyNs = Math.max(0L, newLatencyNs);

                                Log.d("BeatEngineImpl", "driftEmaMs=" + String.format("%.3f", driftEmaMs)
                                        + " desiredLatencyMs=" + String.format("%.3f", desiredLatencyMs)
                                        + " audioLatencyMs=" + String.format("%.3f", audioLatencyNs / 1_000_000.0));
                            }

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
                    // Recreate if missing (should be handled by refreshAudioTracks but just in case)
                    byte[] source = (toneType == BeatSettings.TONE_MECHANICAL) ? strongPcmMechanical : strongPcmElectronic;
                    at = createStaticAudioTrack(source, BeatHelper.VOLUME_PARA[toneType][2]);
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
                    byte[] source = (toneType == BeatSettings.TONE_MECHANICAL) ? weakPcmMechanical : weakPcmElectronic;
                    at = createStaticAudioTrack(source,BeatHelper.VOLUME_PARA[toneType][1]);
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
    //使用静态��播放倒计时
    public void playCountdown() {
        synchronized (audioLock) {
            try {
                if (!generatedLoaded) return;

                if (countdownTrack == null) {
                    byte[] source = (toneType == BeatSettings.TONE_MECHANICAL) ? countdownPcmMechanical : countdownPcmElectronic;
                    countdownTrack = createStaticAudioTrack(source, BeatHelper.VOLUME_PARA[toneType][0]);
                }

                if (countdownTrack != null) {
                    try { countdownTrack.stop(); } catch (Throwable ignored) {}
                    try { countdownTrack.setPlaybackHeadPosition(0); } catch (Throwable ignored) {}
                    try {
                        countdownTrack.play();
                    } catch (Throwable t) {
                        Log.w("BeatEngineImpl", "playCountdown play failed", t);
                    }
                }
            } catch (Throwable t) {
                Log.w("BeatEngineImpl", "playCountdown error", t);
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

    @Override
    public void setSpecialRhythmId(int specialRhythmId) {
        this.specialRhythmId = specialRhythmId;
    }

    @Override
    public void setToneType(int toneType) {
        synchronized (lock) {
            if (this.toneType != toneType) {
                this.toneType = toneType;
                refreshAudioTracks();
            }
        }
    }

    private void playBeat(int idx, int rhythmId) {
        if (rhythmId == 0 || rhythmId == com.example.pianolab.R.drawable.none) {
            // 默认模式
            if (accentEnabled) {
                if (idx == 0) playStrongOnce();
                else playWeakOnce();
            } else {
                playWeakOnce();
            }
        } else {
            // 特殊节奏型模式
            playSpecialRhythm(idx, rhythmId);
        }
    }

    private void playSpecialRhythm(int idx, int rhythmId) {
        // 计算当前拍的时长（纳秒）
        long beatDurationNs = Math.max(1L, Math.round(60_000_000_000.0 / bpm));


        boolean isBase8 = (baseBeat == 8);
        if (isBase8) {
            if (idx % 2 != 0) {

                long absBeat = absoluteBeatCounter.get() - 1; // absoluteBeatCounter 已经在 ticker 里 +1 了
                if (absBeat % 2 != 0) {
                    return;
                }
                // 此时触发，时长为 2 * beatDurationNs
                beatDurationNs *= 2;
            } else {
                 long absBeat = absoluteBeatCounter.get() - 1;
                 if (absBeat % 2 != 0) return;
                 beatDurationNs *= 2;
            }
        }

        double[] rhythm_data = BeatHelper.RHYTHM_DATA.get(rhythmId);

        for (int i = 0; i < rhythm_data[0]; i++) {
            long delayNs = (long) (rhythm_data[i+1] * beatDurationNs);
            boolean isAccent = ((rhythm_data[i+6]>0)&&this.accentEnabled);

            // 立即播放第一个音（delay=0），其他音调度播放
            if (delayNs == 0) {
                if (isAccent) playStrongOnce();
                else playWeakOnce();
            } else {
                scheduler.schedule(() -> {
                    if (isAccent) playStrongOnce();
                    else playWeakOnce();
                }, delayNs, java.util.concurrent.TimeUnit.NANOSECONDS);
            }
        }
    }
}
