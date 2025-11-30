package com.example.pianolab.feature.virtual_piano.engine;

import android.content.Context;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.SoundPool;
import android.os.Build;
import android.os.SystemClock;
import android.util.Log;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

public class OldEngine {
    private static final String TAG = "OldEngine";
    private final Context context;
    private final Map<Integer, Long> loadQueuedNs = Collections.synchronizedMap(new HashMap<>()); // resId -> ns when load() was called
    private volatile boolean allPreloaded = false;

    // SoundPool & mappings
    private SoundPool soundPool;
    // resId -> soundId (SoundPool returned id)
    private final Map<Integer, Integer> resToSoundId = Collections.synchronizedMap(new HashMap<>());
    // soundId -> resId (用于 onLoadComplete 反查)
    private final Map<Integer, Integer> soundIdToRes = Collections.synchronizedMap(new HashMap<>());
    // resId -> ready(true/false)
    private final Map<Integer, Boolean> resReady = Collections.synchronizedMap(new HashMap<>());

    // pointerId -> streamId (playing instance)
    private final Map<Integer, Integer> pointerToStream = Collections.synchronizedMap(new HashMap<>());
    // pointerId -> start uptime millis
    private final Map<Integer, Long> startTimes = Collections.synchronizedMap(new HashMap<>());

    // pending plays when sample not ready: resId -> list of pointerId
    private final Map<Integer, List<Integer>> pendingPlays = Collections.synchronizedMap(new HashMap<>());

    // play executor: allow limited concurrency so chords don't serialize
    private final ExecutorService playExecutor;

    // set of pointerIds that have requested play and are waiting in playExecutor queue / runnable
    private final Set<Integer> pendingPlayPointers = ConcurrentHashMap.newKeySet();
    private final Set<Integer> cancelledBeforePlay = ConcurrentHashMap.newKeySet();

    private final ExecutorService loader = Executors.newSingleThreadExecutor();
    private volatile boolean released = false;

    public OldEngine(Context ctx) {
        this.context = ctx.getApplicationContext();
        Log.i(TAG, "OldEngine ctor");
        // 使用固定大小线程池（可并发 4 个 play 调用），线程为 daemon 以避免阻塞退出
        this.playExecutor = Executors.newFixedThreadPool(4, new ThreadFactory() {
            private int cnt = 0;
            @Override
            public Thread newThread(Runnable r) {
                Thread t = new Thread(r, "piano-play-" + (++cnt));
                t.setDaemon(true);
                return t;
            }
        });
        initSoundPool();
        // 后台预加载一段常用范围，非必须
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
                        .setAudioAttributes(attrs)
                        .setMaxStreams(maxStreams)
                        .build();
            } else {
                soundPool = new SoundPool(maxStreams, AudioManager.STREAM_MUSIC, 0);
            }

            soundPool.setOnLoadCompleteListener((sp, sampleId, status) -> {
                Integer res = soundIdToRes.get(sampleId);
                if (res != null) {
                    boolean ok = (status == 0);
                    resReady.put(res, ok);
                    if (ok) {
                        Log.d(TAG, "onLoadComplete res=" + res + " soundId=" + sampleId);
                    } else {
                        Log.w(TAG, "onLoadComplete failed res=" + res + " soundId=" + sampleId + " status=" + status);
                    }
                    // 如果有 pending plays queued for this res, dispatch them to playExecutor
                    List<Integer> list;
                    synchronized (pendingPlays) {
                        list = pendingPlays.remove(res);
                    }
                    if (list != null && !list.isEmpty() && ok) {
                        for (Integer pid : list) {
                            submitPlayRunnable(res, pid);
                        }
                    }
                } else {
                    Log.w(TAG, "onLoadComplete unknown sampleId=" + sampleId + " status=" + status);
                }
            });
        } catch (Exception e) {
            Log.w(TAG, "initSoundPool failed", e);
            soundPool = null;
        }
    }

    // 异步预加载 mapped kNNN 命名的资源，减少首次按键延迟
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

    public void onKeyDown(String keyName, int pointerId) {
        long tEntryNs = SystemClock.elapsedRealtimeNanos();
        if (keyName == null || released) return;

        final int resId = resolveResIdForKey(keyName);
        if (resId == 0) {
            Log.w(TAG, "onKeyDown no raw res for key: " + keyName);
            return;
        }

        Log.d(TAG, "onKeyDown entry key=" + keyName + " pid=" + pointerId + " res=" + resId + " resolveMs=" + ((SystemClock.elapsedRealtimeNanos() - tEntryNs) / 1000000L));

        Integer soundId = resToSoundId.get(resId);
        Boolean ready = resReady.get(resId);

        if (soundId == null) {
            ensureLoadedAsync(resId);
            synchronized (pendingPlays) {
                List<Integer> list = pendingPlays.get(resId);
                if (list == null) {
                    list = new ArrayList<>();
                    pendingPlays.put(resId, list);
                }
                list.add(pointerId);
            }
            Log.d(TAG, "onKeyDown queued pending play res=" + resId + " pid=" + pointerId);
            return;
        }

        if (ready == null || !ready) {
            synchronized (pendingPlays) {
                List<Integer> list = pendingPlays.get(resId);
                if (list == null) {
                    list = new ArrayList<>();
                    pendingPlays.put(resId, list);
                }
                list.add(pointerId);
            }
            Log.d(TAG, "onKeyDown pending until loaded res=" + resId + " pid=" + pointerId);
            return;
        }

        // ready == true：提交到 playExecutor 执行 play()，并可在执行前被取消（pendingPlayPointers）
        submitPlayRunnable(resId, pointerId);
    }

    // 提交实际 play() 的 runnable 到 playExecutor（并修复日志计时）
    private void submitPlayRunnable(final int resId, final int pointerId) {
        if (released || soundPool == null) return;
        pendingPlayPointers.add(pointerId);
        playExecutor.execute(() -> {
            if (released || soundPool == null) {
                pendingPlayPointers.remove(pointerId);
                return;
            }
            if (!pendingPlayPointers.contains(pointerId)) {
                return;
            }
            Integer sid = resToSoundId.get(resId);
            if (sid == null || !Boolean.TRUE.equals(resReady.get(resId))) {
                pendingPlayPointers.remove(pointerId);
                return;
            }
            try {
                long tCallNs = SystemClock.elapsedRealtimeNanos();
                int stream = soundPool.play(sid, 1f, 1f, 1, 0, 1f);
                long tReturnNs = SystemClock.elapsedRealtimeNanos();
                if (stream != 0) {
                    // put mapping first
                    pointerToStream.put(pointerId, stream);
                    startTimes.put(pointerId, SystemClock.uptimeMillis());

                    // 关键：如果用户在我们开始播放前已经释放（onKeyUp 时没有 stream），
                    // 那里会把 pointerId 放到 cancelledBeforePlay。这里检测并立即 stop。
                    if (cancelledBeforePlay.remove(pointerId)) {
                        try {
                            soundPool.stop(stream);
                            Log.d(TAG, "submitPlayRunnable: stopped late-started stream pid=" + pointerId + " stream=" + stream);
                        } catch (Exception e) {
                            Log.w(TAG, "submitPlayRunnable: stop failed for late-started stream", e);
                        } finally {
                            pointerToStream.remove(pointerId);
                            startTimes.remove(pointerId);
                        }
                    } else {
                        Log.d(TAG, "onKeyDown played res=" + resId + " soundId=" + sid + " stream=" + stream
                                + " call_delta_ms=" + ((tCallNs - tCallNs) / 1000000L)
                                + " return_delta_ms=" + ((tReturnNs - tCallNs) / 1000000L));
                    }
                } else {
                    Log.w(TAG, "onKeyDown play returned stream=0 res=" + resId + " soundId=" + sid);
                }
            } catch (Exception e) {
                Log.w(TAG, "onKeyDown play exception res=" + resId, e);
            } finally {
                pendingPlayPointers.remove(pointerId);
            }
        });
    }


    public void onKeyUp(String keyName, int pointerId) {
        if (released) return;
        Integer streamId = pointerToStream.get(pointerId);
        if (streamId == null) {
            // 如果 play 尚在队列中，取消该 pending 请求，避免按下后放开仍会触发播放
            if (pendingPlayPointers.remove(pointerId)) {
                Log.d(TAG, "onKeyUp cancelled pending play pid=" + pointerId);
            }
            // 标记为已在 UI 层释放但播放尚未写入 stream 的情况
            cancelledBeforePlay.add(pointerId);

            // 也从 pendingPlays（未加载时）移除
            synchronized (pendingPlays) {
                List<Integer> toRemoveKeys = new ArrayList<>();
                for (Map.Entry<Integer, List<Integer>> e : pendingPlays.entrySet()) {
                    List<Integer> list = e.getValue();
                    if (list != null && list.remove((Integer) pointerId)) {
                        if (list.isEmpty()) toRemoveKeys.add(e.getKey());
                    }
                }
                for (Integer k : toRemoveKeys) pendingPlays.remove(k);
            }
            return;
        }
        try {
            soundPool.stop(streamId);
            Log.d(TAG, "onKeyUp stopped stream pid=" + pointerId + " stream=" + streamId);
        } catch (Exception e) {
            Log.w(TAG, "onKeyUp stop failed stream=" + streamId, e);
        } finally {
            pointerToStream.remove(pointerId);
            startTimes.remove(pointerId);
        }
    }


    public void releaseAll() {
        released = true;

        // first shutdown playExecutor
        try {
            if (playExecutor != null) {
                playExecutor.shutdown();
                if (!playExecutor.awaitTermination(500, TimeUnit.MILLISECONDS)) {
                    playExecutor.shutdownNow();
                }
            }
        } catch (InterruptedException ignored) {
            Thread.currentThread().interrupt();
        } catch (Exception ignored) {}

        // shutdown loader
        try {
            if (loader != null) {
                loader.shutdown();
                if (!loader.awaitTermination(500, TimeUnit.MILLISECONDS)) {
                    loader.shutdownNow();
                }
            }
        } catch (InterruptedException ignored) {
            Thread.currentThread().interrupt();
        } catch (Exception ignored) {}

        try {
            if (soundPool != null) {
                soundPool.release();
                soundPool = null;
            }
        } catch (Exception ignored) {}

        // clear state
        resToSoundId.clear();
        soundIdToRes.clear();
        resReady.clear();
        pendingPlays.clear();
        pendingPlayPointers.clear();
        cancelledBeforePlay.clear();
        pointerToStream.clear();
        startTimes.clear();

        Log.d(TAG, "releaseAll");
    }

    /**
     * 异步测试：在后台确保两个 res 已 load/ready（最多等待 timeoutMs），然后在后台连续调用两次 play()
     * 并记录时间日志以便判断是否被底层序列化。不会阻塞 UI 线程。
     */
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

            // ensure loaded
            ensureLoadedAsync(resIdA);
            ensureLoadedAsync(resIdB);

            final long tEntryNs = SystemClock.elapsedRealtimeNanos();
            final long deadlineNs = tEntryNs + Math.max(0, timeoutMs) * 1000000L;
            boolean readyA = false, readyB = false;
            Integer sidA = null, sidB = null;

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
                } catch (InterruptedException ignored) { Thread.currentThread().interrupt(); }
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

            // perform two play() calls as close as possible and measure timestamps
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