package com.example.pianolab.feature.tuner.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.pianolab.feature.tuner.model.TunerState;

import java.util.ArrayList;
import java.util.List;

public class TunerViewModel extends ViewModel {
    private final MutableLiveData<TunerState> tunerState = new MutableLiveData<>(TunerState.idle());
    private boolean referencePlaying = false;
    private String manualTargetNote = "A4";
    private float manualTargetFrequency = 440f;
    private boolean listening;


    public LiveData<TunerState> getTunerState() {
        return tunerState;
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
        tunerState.setValue(current.withListening(newListening));
        if (newListening) {
            emitMockDetection();
        }
    }

    public void toggleReferenceTone() {
        referencePlaying = !referencePlaying;
        // TODO: connect to actual audio playback
    }

    public boolean isReferencePlaying() {
        return referencePlaying;
    }

    private void emitMockDetection() {
        TunerState current = safeState();
        float measured = 439.5f;
        float deviation = -3.2f;
        List<Float> waveform = createMockWaveform();
        if (current.isAutoDetectEnabled()) {
            tunerState.setValue(current
                    .withAutoDetection("A4", 440f)
                    .withMeasurement(measured)
                    .withDeviation(deviation)
                    .withWaveform(waveform));
        } else {
            tunerState.setValue(current
                    .withMeasurement(measured)
                    .withDeviation(deviation)
                    .withWaveform(waveform));
        }
    }

    private List<Float> createMockWaveform() {
        List<Float> samples = new ArrayList<>();
        for (int i = 0; i < 128; i++) {
            samples.add((float) Math.sin(i / 8f));
        }
        return samples;
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
