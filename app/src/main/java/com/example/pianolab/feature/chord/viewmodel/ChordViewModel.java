package com.example.pianolab.feature.chord.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.pianolab.utils.ChordHelper;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

public class ChordViewModel extends ViewModel {

    private final MutableLiveData<List<String>> _selectedKeys = new MutableLiveData<>(new LinkedList<>());
    public LiveData<List<String>> selectedKeys = _selectedKeys;

    private final MutableLiveData<String> _chordText = new MutableLiveData<>("--");
    public LiveData<String> chordText = _chordText;

    private final LinkedList<String> currentKeys = new LinkedList<>();

    public void toggleKey(String keyName) {
        if (currentKeys.contains(keyName)) {
            currentKeys.remove(keyName);
        } else {
            currentKeys.add(keyName);
        }
        updateState();
    }

    public void reset() {
        currentKeys.clear();
        updateState();
    }

    public void backout() {
        if (!currentKeys.isEmpty()) {
            currentKeys.removeLast();
            updateState();
        }
    }

    public void generateChord(String chordName) {
        List<String> keys = ChordHelper.getChordKeys(chordName);
        currentKeys.clear();
        currentKeys.addAll(keys);
        _selectedKeys.setValue(new ArrayList<>(currentKeys));
        _chordText.setValue(chordName);

        // Play the chord sound
        // We need to expose a way to play sound from ViewModel or let Activity observe and play.
        // Since ViewModel shouldn't hold reference to View or SoundEngine directly usually,
        // we can use a LiveData event or similar.
        // But here, let's just add a LiveData for "playChordEvent".
        _playChordEvent.setValue(keys);
    }

    private final MutableLiveData<List<String>> _playChordEvent = new MutableLiveData<>();
    public LiveData<List<String>> playChordEvent = _playChordEvent;

    public void playCurrentChord() {
        if (!currentKeys.isEmpty()) {
            _playChordEvent.setValue(new ArrayList<>(currentKeys));
        }
    }

    private void updateState() {
        _selectedKeys.setValue(new ArrayList<>(currentKeys));
        identifyChord();
    }

    private void identifyChord() {
        if (currentKeys.isEmpty()) {
            _chordText.setValue("--");
            return;
        }

        List<Integer> indices = new ArrayList<>();
        for (String k : currentKeys) {
            int idx = ChordHelper.getKeyIndex(k);
            if (idx != -1) indices.add(idx);
        }
        Collections.sort(indices);

        if (indices.isEmpty()) {
            _chordText.setValue("--");
            return;
        }

        // Basic Chord Logic
        // Normalize to C=0...B=11
        List<Integer> notes = new ArrayList<>();
        for (int idx : indices) {
            // Key 4 is C1 (MIDI 24). Key 16 is C2 (MIDI 36).
            // MIDI note = idx + 20.
            // Pitch class = (idx + 20) % 12.
            int pitchClass = (idx + 20) % 12;
            if (!notes.contains(pitchClass)) {
                notes.add(pitchClass);
            }
        }
        Collections.sort(notes);

        String chordName = ChordHelper.recognizeChord(notes);
        _chordText.setValue(chordName);
    }


}
