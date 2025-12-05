package com.example.pianolab.feature.tuner.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.pianolab.feature.tuner.model.TunerState;

public class TunerViewModel extends ViewModel {
    private final MutableLiveData<TunerState> tunerState = new MutableLiveData<>(TunerState.idle());

    public LiveData<TunerState> getTunerState() {
        return tunerState;
    }

    public void toggleListening() {
        TunerState current = tunerState.getValue();
        if (current == null) {
            current = TunerState.idle();
        }
        boolean newListening = !current.isListening();
        tunerState.setValue(current.withListening(newListening));
        if (newListening) {
            emitMockDetection();
        }
    }

    private void emitMockDetection() {
        TunerState current = tunerState.getValue();
        if (current == null) return;
        tunerState.setValue(current.withDetected("A4", 439.5f));
    }
}

