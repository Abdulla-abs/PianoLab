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

    public void onNoiseFilterChanged(boolean enabled) {
        tunerState.setValue(safeState().withNoiseFilter(enabled));
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
        if (current.isAutoDetectEnabled()) {
            float referenceFreq = TunerHelper.calculate_ref_freq(measured, current.getReferenceFrequency());
            String note = PitchNoteMapper.frequencyToNoteName(measured);
            tunerState.setValue(current
                    .withAutoDetection(note, referenceFreq)
                    .withMeasurement(measured)
                    .withDeviation(calcDeviation(measured, referenceFreq)));
        } else {
            tunerState.setValue(current
                    .withMeasurement(measured)
                    .withDeviation(calcDeviation(measured, current.getManualFrequency())));
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
    }

    private float calcDeviation(float measured, float reference) {
        return PitchNoteMapper.centsOff(measured, reference);
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
