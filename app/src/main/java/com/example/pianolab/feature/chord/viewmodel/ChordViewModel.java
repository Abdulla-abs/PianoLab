package com.example.pianolab.feature.chord.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

public class ChordViewModel extends ViewModel {

    private final MutableLiveData<List<String>> _selectedKeys = new MutableLiveData<>(new LinkedList<>());
    public LiveData<List<String>> selectedKeys = _selectedKeys;

    private final MutableLiveData<String> _chordText = new MutableLiveData<>("");
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

    private void updateState() {
        _selectedKeys.setValue(new ArrayList<>(currentKeys));
        identifyChord();
    }

    private void identifyChord() {
        if (currentKeys.isEmpty()) {
            _chordText.setValue("");
            return;
        }

        List<Integer> indices = new ArrayList<>();
        for (String k : currentKeys) {
            int idx = getKeyIndex(k);
            if (idx != -1) indices.add(idx);
        }
        Collections.sort(indices);

        if (indices.isEmpty()) {
            _chordText.setValue("");
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

        String chordName = recognizeChord(notes);
        _chordText.setValue(chordName);
    }

    private String recognizeChord(List<Integer> notes) {
        if (notes.size() < 3) return ""; // Need at least 3 notes for a triad

        String[] noteNames = {"C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B"};

        // Try each note as root
        for (int i = 0; i < notes.size(); i++) {
            int root = notes.get(i);
            List<Integer> intervals = new ArrayList<>();
            for (int j = 0; j < notes.size(); j++) {
                int note = notes.get((i + j) % notes.size());
                int interval = (note - root + 12) % 12;
                intervals.add(interval);
            }
            Collections.sort(intervals);

            // Check intervals
            // Major: 0, 4, 7
            if (intervals.contains(0) && intervals.contains(4) && intervals.contains(7)) {
                return noteNames[root] + " Maj";
            }
            // Minor: 0, 3, 7
            if (intervals.contains(0) && intervals.contains(3) && intervals.contains(7)) {
                return noteNames[root] + " min";
            }
            // Diminished: 0, 3, 6
            if (intervals.contains(0) && intervals.contains(3) && intervals.contains(6)) {
                return noteNames[root] + " dim";
            }
            // Augmented: 0, 4, 8
            if (intervals.contains(0) && intervals.contains(4) && intervals.contains(8)) {
                return noteNames[root] + " aug";
            }
        }

        return "";
    }

    private int getKeyIndex(String keyName) {
        try {
            int us = keyName.indexOf('_');
            if (keyName.startsWith("key") && us > 3) {
                return Integer.parseInt(keyName.substring(3, us));
            }
        } catch (Exception ignore) {}
        return -1;
    }
}
