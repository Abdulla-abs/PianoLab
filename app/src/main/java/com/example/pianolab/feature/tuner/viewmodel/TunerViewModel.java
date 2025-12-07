package com.example.pianolab.feature.tuner.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.pianolab.feature.tuner.engine.PitchNoteMapper;
import com.example.pianolab.feature.tuner.engine.TarsosAudioEngine;
import com.example.pianolab.feature.tuner.model.TunerState;

public class TunerViewModel extends AndroidViewModel implements TarsosAudioEngine.Listener {
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


    public TunerViewModel(@NonNull Application application) {
        super(application);
        audioEngine = new TarsosAudioEngine(SAMPLE_RATE, BUFFER_SIZE, this);
    }

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
        if (!result.isReliable(PROBABILITY_THRESHOLD)) {
            return;
        }
        float measured = result.getFrequency();
        TunerState current = safeState();
        if (current.isAutoDetectEnabled()) {
            String note = PitchNoteMapper.frequencyToNoteName(measured);
            tunerState.setValue(current
                    .withAutoDetection(note, measured)
                    .withMeasurement(measured)
                    .withDeviation(calcDeviation(measured, measured))
            );
        } else {
            tunerState.setValue(current
                    .withMeasurement(measured)
                    .withDeviation(calcDeviation(measured, current.getManualFrequency()))
            );
        }
    }

    @Override
    public void onError(@NonNull Throwable throwable) {
        // TODO: surface error to UI once design is available
        toggleListening();
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
