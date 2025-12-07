package com.example.pianolab.feature.tuner.engine;

import android.os.Handler;
import android.os.Looper;

import androidx.annotation.MainThread;
import androidx.annotation.NonNull;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

import be.tarsos.dsp.AudioDispatcher;
import be.tarsos.dsp.io.android.AudioDispatcherFactory;
import be.tarsos.dsp.pitch.PitchDetectionHandler;
import be.tarsos.dsp.pitch.PitchProcessor;

public class TarsosAudioEngine {
    public interface Listener {
        @MainThread
        void onPitch(@NonNull PitchDetectionResult result);

        @MainThread
        void onError(@NonNull Throwable throwable);
    }

    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final Listener listener;
    private final float sampleRate;
    private final int bufferSize;
    private final AtomicBoolean running = new AtomicBoolean(false);
    private AudioDispatcher dispatcher;

    public TarsosAudioEngine(float sampleRate, int bufferSize, Listener listener) {
        this.sampleRate = sampleRate;
        this.bufferSize = bufferSize;
        this.listener = listener;
    }

    public void start() {
        if (running.getAndSet(true)) {
            return;
        }
        executor.execute(() -> {
            try {
                dispatcher = AudioDispatcherFactory.fromDefaultMicrophone((int) sampleRate, bufferSize, 0);
                PitchProcessor processor = new PitchProcessor(
                        PitchProcessor.PitchEstimationAlgorithm.YIN,
                        sampleRate,
                        bufferSize,
                        pitchDetectionHandler
                );
                dispatcher.addAudioProcessor(processor);
                dispatcher.run();
            } catch (Throwable t) {
                running.set(false);
                notifyError(t);
            } finally {
                dispatcher = null;
            }
        });
    }

    public void stop() {
        executor.execute(this::stopInternal);
    }

    public void release() {
        stop();
        executor.shutdownNow();
    }

    private void stopInternal() {
        if (!running.getAndSet(false)) {
            return;
        }
        if (dispatcher != null) {
            dispatcher.stop();
            dispatcher = null;
        }
    }

    private final PitchDetectionHandler pitchDetectionHandler = (result, event) -> {
        PitchDetectionResult wrapped = new PitchDetectionResult(result.getPitch(), result.getProbability());
        notifyPitch(wrapped);
    };

    private void notifyPitch(@NonNull PitchDetectionResult result) {
        if (listener == null) {
            return;
        }
        mainHandler.post(() -> listener.onPitch(result));
    }

    private void notifyError(@NonNull Throwable throwable) {
        if (listener == null) {
            return;
        }
        mainHandler.post(() -> listener.onError(throwable));
    }
}
