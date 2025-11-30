package com.example.pianolab.feature.virtual_piano.engine;

import android.content.Context;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.SoundPool;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.util.Log;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

public class PianoSoundEngine {
    private static final String TAG = "PianoSoundEngine";
    private final Context context;
    private final Map<Integer, Long> loadQueuedNs = Collections.synchronizedMap(new HashMap<>());
    private volatile boolean allPreloaded = false;

    private SoundPool soundPool;
    private final Map<Integer, Integer> resToSoundId = Collections.synchronizedMap(new HashMap<>());
    private final Map<Integer, Integer> soundIdToRes = Collections.synchronizedMap(new HashMap<>());
    private final Map<Integer, Boolean> resReady = Collections.synchronizedMap(new HashMap<>());
    private final Map<Integer, Integer> pointerToStream = Collections.synchronizedMap(new HashMap<>());
    private final Map<Integer, Long> startTimes = Collections.synchronizedMap(new HashMap<>());
    private final Map<Integer, List<Integer>> pendingPlays = Collections.synchronizedMap(new HashMap<>());

    private static final long SINGLE_NOTE_DISPATCH_DELAY_MS = 0L;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Object chordLock = new Object();
    private final Map<Integer, Integer> chordQueue = new LinkedHashMap<>();
    private boolean chordFlushScheduled = false;
    private final Runnable chordFlushRunnable = this::flushChordQueue;
    private final Map<Integer, Long> streamStartMs = Collections.synchronizedMap(new HashMap<>());
    private long chordFlushDelayMs = SINGLE_NOTE_DISPATCH_DELAY_MS;

    private final ExecutorService loader = Executors.newSingleThreadExecutor();
    private volatile boolean released = false;
    private static final long MIN_STREAM_DURATION_MS = 250L;


    private static final long CHORD_DISPATCH_WINDOW_MS = 15L;




    public PianoSoundEngine(Context ctx) {
        this.context = ctx.getApplicationContext();
        Log.i(TAG, "PianoSoundEngine ctor");
        initSoundPool();
        preloadMappedRangeAsync(21, 108);
    }

    private void initSoundPool() {
        final int maxStreams = 32;
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                AudioAttributes attrs = new AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build();
                soundPool = new SoundPool.Builder()
                        .setMaxStreams(maxStreams)
                        .setAudioAttributes(attrs)
                        .build();
            } else {
                soundPool = new SoundPool(maxStreams, AudioManager.STREAM_MUSIC, 0);
            }
            soundPool.setOnLoadCompleteListener((sp, sampleId, status) -> {
                Integer resId = soundIdToRes.get(sampleId);
                if (resId == null) return;
                boolean ok = status == 0;
                resReady.put(resId, ok);
                if (!ok) {
                    Log.w(TAG, "load failed res=" + resId + " sampleId=" + sampleId + " status=" + status);
                    return;
                }
                List<Integer> pointers;
                synchronized (pendingPlays) {
                    pointers = pendingPlays.remove(resId);
                }
                if (pointers != null) {
                    for (Integer pid : pointers) {
                        if (pid != null) {
                            enqueueChordPlay(resId, pid);
                        }
                    }
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "initSoundPool failed", e);
        }
    }

    private void preloadMappedRangeAsync(int fromInclusive, int toInclusive) {
        loader.execute(() -> {
            long batchStart = SystemClock.elapsedRealtimeNanos();
            int count = 0;
            for (int i = fromInclusive; i <= toInclusive && !released; i++) {
                String name = String.format("k%03d", i);
                int res = context.getResources().getIdentifier(name, "raw", context.getPackageName());
                if (res == 0) continue;
                ensureLoadedAsync(res);
                count++;
            }
            allPreloaded = true;
            long batchEnd = SystemClock.elapsedRealtimeNanos();
            Log.i(TAG, "preloadMappedRangeAsync finished count=" + count + " costMs=" + ((batchEnd - batchStart) / 1000000L));
        });
    }

    private int resolveResIdForKey(String keyName) {
        long tStart = SystemClock.elapsedRealtimeNanos();
        if (keyName == null) return 0;
        String pkg = context.getPackageName();

        try {
            if (keyName.startsWith("key")) {
                int us = keyName.indexOf('_');
                String numPart = (us > 0) ? keyName.substring(3, us) : keyName.substring(3);
                int idx = Integer.parseInt(numPart);
                int mapped = idx + 20;
                String mappedName = String.format("k%03d", mapped);
                int res = context.getResources().getIdentifier(mappedName, "raw", pkg);
                if (res != 0) {
                    Log.d(TAG, "resolveResIdForKey mapped key=" + keyName + " -> " + mappedName + " res=" + res + " costMs=" + ((SystemClock.elapsedRealtimeNanos() - tStart) / 1000000L));
                    return res;
                }
            }
        } catch (Exception ignored) { }

        int res = context.getResources().getIdentifier(keyName, "raw", pkg);
        if (res != 0) {
            Log.d(TAG, "resolveResIdForKey fallback fullName key=" + keyName + " res=" + res + " costMs=" + ((SystemClock.elapsedRealtimeNanos() - tStart) / 1000000L));
            return res;
        }

        try {
            if (keyName.startsWith("key")) {
                int us = keyName.indexOf('_');
                String numPart = (us > 0) ? keyName.substring(3, us) : keyName.substring(3);
                int idx = Integer.parseInt(numPart);
                String mappedName = String.format("k%03d", idx);
                res = context.getResources().getIdentifier(mappedName, "raw", pkg);
                if (res != 0) {
                    Log.d(TAG, "resolveResIdForKey fallback2 key=" + keyName + " -> " + mappedName + " res=" + res + " costMs=" + ((SystemClock.elapsedRealtimeNanos() - tStart) / 1000000L));
                    return res;
                }
            }
        } catch (Exception ignored) { }

        return 0;
    }

    private void ensureLoadedAsync(final int resId) {
        if (resId == 0 || released || soundPool == null) return;
        synchronized (resToSoundId) {
            if (resToSoundId.containsKey(resId)) return;
            loader.execute(() -> {
                if (released || soundPool == null) return;
                try {
                    int soundId = soundPool.load(context, resId, 1);
                    resToSoundId.put(resId, soundId);
                    soundIdToRes.put(soundId, resId);
                    resReady.put(resId, false);
                    loadQueuedNs.put(resId, SystemClock.elapsedRealtimeNanos());
                    Log.d(TAG, "ensureLoadedAsync queued load res=" + resId + " soundId=" + soundId);
                } catch (Exception e) {
                    Log.w(TAG, "ensureLoadedAsync load failed res=" + resId, e);
                }
            });
        }
    }
    private final ExecutorService playExecutor = Executors.newFixedThreadPool(4, new ThreadFactory() {
        private int idx = 0;
        @Override
        public Thread newThread(Runnable r) {
            Thread t = new Thread(r, "piano-chord-" + (++idx));
            t.setDaemon(true);
            return t;
        }
    });

    public void onKeyDown(String keyName, int pointerId) {
        long tEntryNs = SystemClock.elapsedRealtimeNanos();
        if (keyName == null || released) return;

        final int resId = resolveResIdForKey(keyName);
        if (resId == 0) {
            Log.w(TAG, "onKeyDown no raw res for key=" + keyName);
            return;
        }

        Log.d(TAG, "onKeyDown key=" + keyName + " pid=" + pointerId
                + " res=" + resId + " resolveMs=" + ((SystemClock.elapsedRealtimeNanos() - tEntryNs) / 1_000_000L));

        Integer soundId = resToSoundId.get(resId);
        Boolean ready = resReady.get(resId);

        if (soundId == null) {
            ensureLoadedAsync(resId);
            queuePendingPlay(resId, pointerId);
            return;
        }

        if (!Boolean.TRUE.equals(ready)) {
            queuePendingPlay(resId, pointerId);
            return;
        }

        enqueueChordPlay(resId, pointerId);
    }

    private void queuePendingPlay(int resId, int pointerId) {
        synchronized (pendingPlays) {
            List<Integer> list = pendingPlays.get(resId);
            if (list == null) {
                list = new ArrayList<>();
                pendingPlays.put(resId, list);
            }
            list.add(pointerId);
        }
    }


    private void playSample(final int resId, final int pointerId) {
        if (released || soundPool == null) return;
        Integer sid = resToSoundId.get(resId);
        if (sid == null || !Boolean.TRUE.equals(resReady.get(resId))) return;
        try {
            long tCallNs = SystemClock.elapsedRealtimeNanos();
            int stream = soundPool.play(sid, 1f, 1f, 1, 0, 1f);
            long tReturnNs = SystemClock.elapsedRealtimeNanos();
            if (stream != 0) {
                pointerToStream.put(pointerId, stream);
                streamStartMs.put(stream, SystemClock.elapsedRealtime());
                Log.d(TAG, "playSample success res=" + resId + " pid=" + pointerId
                        + " stream=" + stream + " callMs=" + ((tReturnNs - tCallNs) / 1_000_000L));
            } else {
                Log.w(TAG, "playSample failed to start res=" + resId + " pid=" + pointerId);
            }
        } catch (Exception e) {
            Log.w(TAG, "playSample exception res=" + resId, e);
        }
    }

    public void onKeyUp(String keyName, int pointerId) {
        if (released) return;

        if (removeFromChordQueue(pointerId)) {
            removePendingPointer(pointerId);
            return;
        }

        Integer streamId = pointerToStream.remove(pointerId);
        if (streamId == null) {
            removePendingPointer(pointerId);
            return;
        }

        Long startMs = streamStartMs.get(streamId);
        long elapsed = (startMs == null) ? MIN_STREAM_DURATION_MS : (SystemClock.elapsedRealtime() - startMs);
        long remaining = MIN_STREAM_DURATION_MS - elapsed;
        if (remaining > 0) {
            scheduleStop(streamId, remaining);
        } else {
            stopStream(streamId);
        }
    }
    private void enqueueChordPlay(int resId, int pointerId) {
        if (released || soundPool == null || playExecutor.isShutdown()) return;
        long delayToPost = -1L;
        synchronized (chordLock) {
            chordQueue.put(pointerId, resId);
            boolean multiTouch = chordQueue.size() > 1;
            if (!chordFlushScheduled) {
                chordFlushScheduled = true;
                chordFlushDelayMs = multiTouch ? CHORD_DISPATCH_WINDOW_MS : SINGLE_NOTE_DISPATCH_DELAY_MS;
                delayToPost = chordFlushDelayMs;
            } else if (multiTouch && chordFlushDelayMs != CHORD_DISPATCH_WINDOW_MS) {
                handler.removeCallbacks(chordFlushRunnable);
                chordFlushDelayMs = CHORD_DISPATCH_WINDOW_MS;
                delayToPost = chordFlushDelayMs;
            }
        }
        if (delayToPost >= 0) {
            handler.postDelayed(chordFlushRunnable, delayToPost);
        }
    }
    private void flushChordQueue() {
        Map<Integer, Integer> snapshot;
        synchronized (chordLock) {
            if (released || soundPool == null || chordQueue.isEmpty()) {
                chordQueue.clear();
                chordFlushScheduled = false;
                chordFlushDelayMs = SINGLE_NOTE_DISPATCH_DELAY_MS;
                return;
            }
            snapshot = new LinkedHashMap<>(chordQueue);
            chordQueue.clear();
            chordFlushScheduled = false;
            chordFlushDelayMs = SINGLE_NOTE_DISPATCH_DELAY_MS;
        }
        if (snapshot.isEmpty()) return;

        if (snapshot.size() == 1 || playExecutor.isShutdown()) {
            Map.Entry<Integer, Integer> entry = snapshot.entrySet().iterator().next();
            playSample(entry.getValue(), entry.getKey());
            return;
        }

        CountDownLatch startGate = new CountDownLatch(1);
        for (Map.Entry<Integer, Integer> entry : snapshot.entrySet()) {
            final int pid = entry.getKey();
            final int res = entry.getValue();
            try {
                playExecutor.execute(() -> {
                    try {
                        startGate.await();
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        return;
                    }
                    playSample(res, pid);
                });
            } catch (RejectedExecutionException ex) {
                playSample(res, pid);
            }
        }
        startGate.countDown();
    }
    private boolean removeFromChordQueue(int pointerId) {
        synchronized (chordLock) {
            boolean removed = chordQueue.remove(pointerId) != null;
            if (removed && chordQueue.isEmpty()) {
                handler.removeCallbacks(chordFlushRunnable);
                chordFlushScheduled = false;
            }
            return removed;
        }
    }

    private void removePendingPointer(int pointerId) {
        synchronized (pendingPlays) {
            List<Integer> toRemove = new ArrayList<>();
            for (Map.Entry<Integer, List<Integer>> e : pendingPlays.entrySet()) {
                List<Integer> list = e.getValue();
                if (list != null && list.remove((Integer) pointerId) && list.isEmpty()) {
                    toRemove.add(e.getKey());
                }
            }
            for (Integer k : toRemove) pendingPlays.remove(k);
        }
    }

    private void scheduleStop(final int streamId, long delayMs) {
        long safeDelay = Math.max(0L, delayMs);
        handler.postDelayed(() -> stopStream(streamId), safeDelay);
        Log.d(TAG, "scheduleStop stream=" + streamId + " delayMs=" + safeDelay);
    }
    private void stopStream(int streamId) {
        try {
            if (!released && soundPool != null) {
                soundPool.stop(streamId);
                Log.d(TAG, "stopStream stream=" + streamId);
            }
        } catch (Exception e) {
            Log.w(TAG, "stopStream failed stream=" + streamId, e);
        } finally {
            streamStartMs.remove(streamId);
        }
    }

    public void releaseAll() {
        released = true;
        handler.removeCallbacksAndMessages(null);
        synchronized (chordLock) {
            chordQueue.clear();
            chordFlushScheduled = false;
            chordFlushDelayMs = SINGLE_NOTE_DISPATCH_DELAY_MS;
        }

        try {
            playExecutor.shutdown();
            if (!playExecutor.awaitTermination(500, TimeUnit.MILLISECONDS)) {
                playExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (Exception ignored) { }

        try {
            loader.shutdown();
            if (!loader.awaitTermination(500, TimeUnit.MILLISECONDS)) {
                loader.shutdownNow();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (Exception ignored) { }

        try {
            if (soundPool != null) {
                soundPool.release();
                soundPool = null;
            }
        } catch (Exception ignored) { }

        resToSoundId.clear();
        soundIdToRes.clear();
        resReady.clear();
        pendingPlays.clear();
        pointerToStream.clear();
        streamStartMs.clear();

        Log.d(TAG, "releaseAll");
    }
    public void playSimultaneousTestAsync(final int resIdA, final int resIdB, final long timeoutMs) {
        loader.execute(() -> {
            if (released || soundPool == null) {
                Log.i(TAG, "playSimultaneousTestAsync: engine released or soundPool null");
                return;
            }
            if (resIdA == 0 || resIdB == 0) {
                Log.i(TAG, "playSimultaneousTestAsync: invalid res ids resA=" + resIdA + " resB=" + resIdB);
                return;
            }

            ensureLoadedAsync(resIdA);
            ensureLoadedAsync(resIdB);

            final long tEntryNs = SystemClock.elapsedRealtimeNanos();
            final long deadlineNs = tEntryNs + Math.max(0, timeoutMs) * 1000000L;
            boolean readyA;
            boolean readyB;
            Integer sidA;
            Integer sidB;

            while (SystemClock.elapsedRealtimeNanos() < deadlineNs) {
                if (released || soundPool == null) {
                    Log.i(TAG, "playSimultaneousTestAsync aborted");
                    return;
                }
                sidA = resToSoundId.get(resIdA);
                sidB = resToSoundId.get(resIdB);
                readyA = Boolean.TRUE.equals(resReady.get(resIdA));
                readyB = Boolean.TRUE.equals(resReady.get(resIdB));
                if (sidA != null && sidB != null && readyA && readyB) break;
                try {
                    Thread.sleep(10);
                } catch (InterruptedException ignored) {
                    Thread.currentThread().interrupt();
                }
            }

            sidA = resToSoundId.get(resIdA);
            sidB = resToSoundId.get(resIdB);
            readyA = Boolean.TRUE.equals(resReady.get(resIdA));
            readyB = Boolean.TRUE.equals(resReady.get(resIdB));

            if (sidA == null || sidB == null || !readyA || !readyB) {
                Log.i(TAG, "playSimultaneousTestAsync: one or both samples not ready sidA=" + sidA + " sidB=" + sidB
                        + " readyA=" + readyA + " readyB=" + readyB);
                return;
            }

            try {
                long tCall1Ns = SystemClock.elapsedRealtimeNanos();
                int stream1 = soundPool.play(sidA, 1f, 1f, 1, 0, 1f);
                long tReturn1Ns = SystemClock.elapsedRealtimeNanos();

                long tCall2Ns = SystemClock.elapsedRealtimeNanos();
                int stream2 = soundPool.play(sidB, 1f, 1f, 1, 0, 1f);
                long tReturn2Ns = SystemClock.elapsedRealtimeNanos();

                Log.i(TAG, "playSimultaneousTestAsync resA=" + resIdA + " sidA=" + sidA + " stream1=" + stream1
                        + " call1_offset_ms=" + ((tCall1Ns - tEntryNs) / 1000000L)
                        + " return1_delta_ms=" + ((tReturn1Ns - tCall1Ns) / 1000000L));
                Log.i(TAG, "playSimultaneousTestAsync resB=" + resIdB + " sidB=" + sidB + " stream2=" + stream2
                        + " call2_offset_ms=" + ((tCall2Ns - tEntryNs) / 1000000L)
                        + " return2_delta_ms=" + ((tReturn2Ns - tCall2Ns) / 1000000L)
                        + " inter_call_gap_ms=" + ((tCall2Ns - tReturn1Ns) / 1000000L));
            } catch (Exception e) {
                Log.w(TAG, "playSimultaneousTestAsync play exception", e);
            }
        });
    }
}