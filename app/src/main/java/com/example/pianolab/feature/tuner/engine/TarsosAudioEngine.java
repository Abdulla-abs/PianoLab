package com.example.pianolab.feature.tuner.engine;

import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.util.Log;

import androidx.annotation.MainThread;
import androidx.annotation.NonNull;

import java.util.Arrays;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

import be.tarsos.dsp.AudioEvent;
import be.tarsos.dsp.AudioProcessor;
import be.tarsos.dsp.AudioDispatcher;
import be.tarsos.dsp.io.android.AudioDispatcherFactory;
import be.tarsos.dsp.pitch.PitchDetectionHandler;
import be.tarsos.dsp.pitch.PitchProcessor;
import be.tarsos.dsp.util.fft.FFT;

public class TarsosAudioEngine {
    private static final String TAG = "TarsosAudioEngine";
    public interface Listener {
        @MainThread
        void onPitch(@NonNull PitchDetectionResult result);

        @MainThread
        void onSpectrum(@NonNull float[] magnitudes);
        @MainThread
        void onWaveform(@NonNull float[] samples);

        @MainThread
        void onError(@NonNull Throwable throwable);
    }

    private static final long SPECTRUM_INTERVAL_MS = 75L;

    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final Listener listener;
    private final float sampleRate;
    private final int bufferSize;
    private final AtomicBoolean running = new AtomicBoolean(false);
    private final AudioProcessor spectrumProcessor;
    private AudioDispatcher dispatcher;
    private long lastSpectrumDispatch = 0L;


    public TarsosAudioEngine(float sampleRate, int bufferSize, Listener listener) {
        this.sampleRate = sampleRate;
        this.bufferSize = bufferSize;
        this.listener = listener;
        this.spectrumProcessor = createSpectrumProcessor();
    }

    public void start() {
        if (running.getAndSet(true)) {
            Log.d(TAG, "start ignored: already running");
            return;
        }
        Log.d(TAG, "start requested");
        executor.execute(() -> {
            try {
                dispatcher = AudioDispatcherFactory.fromDefaultMicrophone((int) sampleRate, bufferSize, 0);
                Log.d(TAG, "microphone opened");
                dispatcher.addAudioProcessor(spectrumProcessor);
                PitchProcessor processor = new PitchProcessor(
                        PitchProcessor.PitchEstimationAlgorithm.YIN,
                        sampleRate,
                        bufferSize,
                        pitchDetectionHandler
                );
                dispatcher.addAudioProcessor(processor);
                dispatcher.run();
            } catch (Throwable t) {
                Log.e(TAG, "dispatcher failed", t);
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

    private AudioProcessor createSpectrumProcessor() {
        return new AudioProcessor() {
            private final FFT fft = new FFT(bufferSize);
            private final float[] fftBuffer = new float[bufferSize * 2];
            private final float[] magnitudes = new float[bufferSize];

            @Override
            public boolean process(AudioEvent event) {
                long now = SystemClock.elapsedRealtime();
                if (now - lastSpectrumDispatch < SPECTRUM_INTERVAL_MS) {
                    return true;
                }
                lastSpectrumDispatch = now;
                float[] buffer = event.getFloatBuffer();
                float[] waveform = Arrays.copyOf(buffer, Math.min(buffer.length, bufferSize));
                notifyWaveform(waveform);
                Arrays.fill(fftBuffer, 0f);
                System.arraycopy(buffer, 0, fftBuffer, 0, Math.min(buffer.length, bufferSize));
                fft.forwardTransform(fftBuffer);
                fft.modulus(fftBuffer, magnitudes);
                float[] half = Arrays.copyOf(magnitudes, bufferSize / 2);
                notifySpectrum(half);
                return true;
            }

            @Override
            public void processingFinished() { }
        };
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

    private void notifySpectrum(@NonNull float[] magnitudes) {
        if (listener == null) {
            return;
        }
        mainHandler.post(() -> listener.onSpectrum(magnitudes));
    }
    private void notifyWaveform(@NonNull float[] samples) {
        if (listener == null) {
            return;
        }
        mainHandler.post(() -> listener.onWaveform(samples));
    }

    private void notifyError(@NonNull Throwable throwable) {
        Log.e(TAG, "notifyError", throwable);
        if (listener == null) {
            return;
        }
        mainHandler.post(() -> listener.onError(throwable));
    }
}
