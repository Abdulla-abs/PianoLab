package com.example.pianolab.utils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ChordHelper {


    public static String recognizeChord(List<Integer> notes) {
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

    public static int getKeyIndex(String keyName) {
        try {
            int us = keyName.indexOf('_');
            if (keyName.startsWith("key") && us > 3) {
                return Integer.parseInt(keyName.substring(3, us));
            }
        } catch (Exception ignore) {}
        return -1;
    }

    public static int getDiatonicDistance(int fromIndex, int toIndex) {
        int[] diatonicMap = {0, 0, 1, 1, 2, 3, 3, 4, 4, 5, 5, 6};

        int fromOctave = (fromIndex - 4) / 12;
        int fromNote = (fromIndex - 4) % 12;
        if (fromNote < 0) fromNote += 12;
        int fromDiatonic = fromOctave * 7 + diatonicMap[fromNote];

        int toOctave = (toIndex - 4) / 12;
        int toNote = (toIndex - 4) % 12;
        if (toNote < 0) toNote += 12;
        int toDiatonic = toOctave * 7 + diatonicMap[toNote];

        return toDiatonic - fromDiatonic;
    }
}
