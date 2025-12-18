package com.example.pianolab.utils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ChordHelper {


    public static String recognizeChord(List<Integer> notes) {
        if (notes.isEmpty()) return "--";
        // Allow 2 notes for omitted 5th chords (e.g. C-B for Cmaj7(no5)) or power chords (C-G)
        // But standard triads need 3. Let's be flexible.
        // If it's just 1 note, it's just a note, not a chord usually, but let's return "Error" or note name?
        // Requirement says: "When user selects keys that cannot form a chord... display 'Error'".
        // A single note is technically not a chord. Two notes can imply a chord.
        // Let's stick to: if we can't find a match, return "Error".

        String[] noteNames = {"C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B"};

        // Try each note as root
        for (int i = 0; i < notes.size(); i++) {
            int root = notes.get(i);
            List<Integer> intervals = new ArrayList<>();
            for (int j = 0; j < notes.size(); j++) {
                int note = notes.get(j); // Use original list, order doesn't matter for set of intervals
                int interval = (note - root + 12) % 12;
                if (!intervals.contains(interval)) {
                    intervals.add(interval);
                }
            }
            Collections.sort(intervals);

            // --- Triads (3 notes) ---
            // Major: 0, 4, 7
            if (match(intervals, 0, 4, 7)) return noteNames[root] + " Maj";
            // Minor: 0, 3, 7
            if (match(intervals, 0, 3, 7)) return noteNames[root] + " min";
            // Diminished: 0, 3, 6
            if (match(intervals, 0, 3, 6)) return noteNames[root] + " dim";
            // Augmented: 0, 4, 8
            if (match(intervals, 0, 4, 8)) return noteNames[root] + " aug";
            // Sus2: 0, 2, 7
            if (match(intervals, 0, 2, 7)) return noteNames[root] + " sus2";
            // Sus4: 0, 5, 7
            if (match(intervals, 0, 5, 7)) return noteNames[root] + " sus4";

            // --- 7th Chords (4 notes) ---
            // Major 7: 0, 4, 7, 11
            if (match(intervals, 0, 4, 7, 11)) return noteNames[root] + " Maj7";
            // Dominant 7: 0, 4, 7, 10
            if (match(intervals, 0, 4, 7, 10)) return noteNames[root] + " 7";
            // Minor 7: 0, 3, 7, 10
            if (match(intervals, 0, 3, 7, 10)) return noteNames[root] + " m7";
            // Minor Major 7: 0, 3, 7, 11
            if (match(intervals, 0, 3, 7, 11)) return noteNames[root] + " mM7";
            // Half-Diminished 7 (m7b5): 0, 3, 6, 10
            if (match(intervals, 0, 3, 6, 10)) return noteNames[root] + " m7b5";
            // Diminished 7: 0, 3, 6, 9
            if (match(intervals, 0, 3, 6, 9)) return noteNames[root] + " dim7";
            // Augmented 7: 0, 4, 8, 10
            if (match(intervals, 0, 4, 8, 10)) return noteNames[root] + " aug7";
            // Augmented Major 7: 0, 4, 8, 11
            if (match(intervals, 0, 4, 8, 11)) return noteNames[root] + " augM7";
            // 6th: 0, 4, 7, 9
            if (match(intervals, 0, 4, 7, 9)) return noteNames[root] + " 6";
            // Minor 6th: 0, 3, 7, 9
            if (match(intervals, 0, 3, 7, 9)) return noteNames[root] + " m6";


            // --- Omitted 5th Cases (Common in 7th chords) ---
            // Major 7 (no 5): 0, 4, 11
            if (match(intervals, 0, 4, 11)) return noteNames[root] + " Maj7";
            // Dominant 7 (no 5): 0, 4, 10
            if (match(intervals, 0, 4, 10)) return noteNames[root] + " 7";
            // Minor 7 (no 5): 0, 3, 10
            if (match(intervals, 0, 3, 10)) return noteNames[root] + " m7";
            // Minor Major 7 (no 5): 0, 3, 11
            if (match(intervals, 0, 3, 11)) return noteNames[root] + " mM7";

            // Note: Diminished chords usually rely heavily on the b5 (6 semitones), so omitting it makes them ambiguous.
            // e.g. C dim7 (no 5) -> C, Eb, A -> 0, 3, 9. This is also A dim (A, C, Eb).
            // Context matters, but strictly interval-wise, 0,3,9 is a diminished triad inversion.
            // We'll stick to the most common omitted 5th cases for 7th chords.
        }

        return "错误";
    }

    private static boolean match(List<Integer> intervals, int... target) {
        if (intervals.size() != target.length) return false;
        for (int t : target) {
            if (!intervals.contains(t)) return false;
        }
        return true;
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
