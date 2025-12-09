package com.example.pianolab.feature.tuner.viewmodel;

import android.app.Application;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.pianolab.feature.tuner.engine.PitchNoteMapper;
import com.example.pianolab.feature.tuner.engine.TarsosAudioEngine;
import com.example.pianolab.feature.tuner.model.TunerState;
import com.example.pianolab.utils.TunerHelper;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class TunerViewModel extends AndroidViewModel implements TarsosAudioEngine.Listener {
    private static final String TAG = "TunerViewModel";
    private static final float DEFAULT_REFERENCE = 440f;
    private static final float ALT_REFERENCE = 442f;
    private static final float PROBABILITY_THRESHOLD = 0.85f;
    private static final float SAMPLE_RATE = 44100f;
    private static final int BUFFER_SIZE = 2048;
    private static final int STABLE_FRAME_COUNT = 5;
    private static final int DELAY_FRAME_COUNT = 3;
    private static final float STABLE_DEVIATION_CENTS = 10f;
    private static final float AMPLITUDE_DROP_RATIO = 0.4f;

    private static final int QUICK_STOP_FRAME_COUNT = 3;  // 约 3 帧 ≈ 225ms
    private int validPitchCount = 0;

    private final List<Float> stableFrequencies = new ArrayList<>();
    private int delayCounter = 0;
    private float lastRMS = 0f;



    private final MutableLiveData<TunerState> tunerState = new MutableLiveData<>(TunerState.idle());
    private final TarsosAudioEngine audioEngine;
    private boolean referencePlaying = false;
    private String manualTargetNote = "A4";
    private float manualTargetFrequency = DEFAULT_REFERENCE;
    private boolean listening;
    private boolean frequencyMode = true;


    public TunerViewModel(@NonNull Application application) {
        super(application);
        audioEngine = new TarsosAudioEngine(SAMPLE_RATE, BUFFER_SIZE, this);
    }

    public LiveData<TunerState> getTunerState() {
        return tunerState;
    }

    public void setWaveDisplayMode(boolean frequencyMode) {
        this.frequencyMode = frequencyMode;
        tunerState.setValue(safeState());
    }

    public boolean isFrequencyMode() {
        return frequencyMode;
    }

    public void onAutoDetectChanged(boolean enabled) {
        TunerState current = safeState();
        tunerState.setValue(current.withAutoDetect(enabled));
        if (!enabled) {
            tunerState.setValue(tunerState.getValue().withManualTarget(manualTargetNote, manualTargetFrequency));
        }
    }

    public void onAutoStopChanged(boolean enabled) {
        tunerState.setValue(safeState().withAutoStop(enabled));
        if (enabled && safeState().isListening()) {
            resetAutoStopState();
        }
    }

    public void onReferenceStandardChanged(boolean use442) {
        float freq = use442 ? 442f : 440f;
        tunerState.setValue(safeState().withReferenceFrequency(freq));
        if (!safeState().isAutoDetectEnabled()) {
            setManualTarget(manualTargetNote, freq);
        }
    }

    public void setManualTarget(String note, float frequency) {
        this.manualTargetNote = note;
        this.manualTargetFrequency = frequency;
        tunerState.setValue(safeState().withManualTarget(note, frequency));
    }

    public void toggleListening() {
        TunerState current = safeState();
        boolean newListening = !current.isListening();
        Log.d(TAG, "toggleListening -> " + newListening);
        tunerState.setValue(current.withListening(newListening));
        listening = newListening;
        if (newListening) {
            resetAutoStopState();
            audioEngine.start();
        } else {
            audioEngine.stop();
        }
    }

    public void toggleReferenceTone() {
        referencePlaying = !referencePlaying;
        // TODO: connect to actual audio playback
    }

    public boolean isReferencePlaying() {
        return referencePlaying;
    }

    @Override
    public void onCleared() {
        super.onCleared();
        audioEngine.release();
    }

    @Override
    public void onPitch(@NonNull com.example.pianolab.feature.tuner.engine.PitchDetectionResult result) {
        if (!safeState().isListening() || !result.isReliable(PROBABILITY_THRESHOLD)) {
            return;
        }

        float measured = result.getFrequency();
        if (measured <= 0f) {
            return;
        }

        TunerState current = safeState();
        float targetFreq;

        if (current.isAutoDetectEnabled()) {
            float detectedNoteFreq = TunerHelper.calculate_ref_freq(measured, current.getReferenceFrequency());
            String note = PitchNoteMapper.frequencyToNoteName(measured);
            targetFreq = detectedNoteFreq;
            tunerState.setValue(current
                    .withAutoDetection(note, detectedNoteFreq)
                    .withMeasurement(measured)
                    .withDeviation(TunerHelper.calculate_deviation_cent(measured, detectedNoteFreq)));
        } else {
            targetFreq = current.getManualFrequency();
            tunerState.setValue(current
                    .withMeasurement(measured)
                    .withDeviation(TunerHelper.calculate_deviation_cent(measured, targetFreq)));
        }

        if (current.isAutoStopEnabled()) {
            processQuickStop();
        }
    }

    @Override
    public void onError(@NonNull Throwable throwable) {
        Log.e(TAG, "Audio engine error", throwable);
        TunerState current = safeState();
        listening = false;
        tunerState.postValue(current.withListening(false));
    }

    @Override
    public void onSpectrum(@NonNull float[] magnitudes) {
        if (!safeState().isListening()) {
            return;
        }
        List<Float> data = new ArrayList<>(magnitudes.length);
        for (float value : magnitudes) {
            data.add(value);
        }
        tunerState.setValue(safeState().withSpectrum(data));
    }

    @Override
    public void onWaveform(@NonNull float[] samples) {
        if (!safeState().isListening()) {
            return;
        }
        List<Float> waveform = new ArrayList<>(samples.length);
        for (float sample : samples) {
            waveform.add(sample);
        }
        tunerState.setValue(safeState().withWaveform(waveform));

        if (safeState().isAutoStopEnabled()) {
            float rms = TunerHelper.calculate_RMS(samples);
            if (lastRMS > 0f && rms < lastRMS * AMPLITUDE_DROP_RATIO) {
                Log.d(TAG, "Amplitude drop detected, reset quick-stop");
                resetAutoStopState();
            }
            lastRMS = rms;
        }
    }

    private void processAutoStop(float measured, float targetFreq) {
        float deviation = Math.abs(TunerHelper.calculate_deviation_cent(measured, targetFreq));

        if (deviation <= STABLE_DEVIATION_CENTS) {
            stableFrequencies.add(measured);
            if (stableFrequencies.size() >= STABLE_FRAME_COUNT) {
                delayCounter++;
                if (delayCounter >= DELAY_FRAME_COUNT) {
                    Log.d(TAG, "Auto-stop triggered: stopping listening");
                    toggleListening();
                }
            }
        } else {
            resetAutoStopState();
        }
    }

    private void processQuickStop() {
        validPitchCount++;
        if (validPitchCount >= QUICK_STOP_FRAME_COUNT) {
            Log.d(TAG, "Quick-stop triggered after " + validPitchCount + " frames");
            toggleListening();
        }
    }

    private void resetAutoStopState() {
        validPitchCount = 0;
        lastRMS = 0f;
    }

    private TunerState safeState() {
        TunerState current = tunerState.getValue();
        if (current == null) {
            current = TunerState.idle();
            tunerState.setValue(current);
        }
        return current;
    }

    public boolean isListening() { return listening; }
    public void setListening(boolean listening) { this.listening = listening; }
}
