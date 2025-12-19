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

    public static List<String> getChordKeys(String chordName) {
        List<String> keys = new ArrayList<>();
        if (chordName == null || chordName.isEmpty()) return keys;

        // Parse Root, Accidental, Type
        // Example: "C#maj7", "Bbmin", "C7"
        String root = "";
        String accidental = "";
        String type = "";

        // 1. Extract Root (First char)
        root = chordName.substring(0, 1);
        String remainder = chordName.substring(1);

        // 2. Extract Accidental (# or b)
        if (remainder.startsWith("#") || remainder.startsWith("b")) {
            accidental = remainder.substring(0, 1);
            remainder = remainder.substring(1);
        }

        // 3. The rest is Type
        type = remainder;

        // Calculate Root Index (Base C4 is key 40)
        // C4 MIDI is 60. Our key index 40 corresponds to MIDI 60.
        // Let's map root note name to offset from C.
        int rootOffset = 0;
        switch (root) {
            case "C": rootOffset = 0; break;
            case "D": rootOffset = 2; break;
            case "E": rootOffset = 4; break;
            case "F": rootOffset = 5; break;
            case "G": rootOffset = 7; break;
            case "A": rootOffset = 9; break;
            case "B": rootOffset = 11; break;
        }

        if (accidental.equals("#")) rootOffset += 1;
        if (accidental.equals("b")) rootOffset -= 1;

        // Requirement: "Root note parsing rule: If user selects C, root is C4. If user selects Bb, root is Bb4."
        // C4 is key 40.
        // If user selects B (rootOffset 11), it is B4 (key 51).
        // If user selects Bb (rootOffset 10), it is Bb4 (key 50).
        // If user selects C (rootOffset 0), it is C4 (key 40).
        // So rootIndex = 40 + rootOffset is correct for C4 base.

        int rootIndex = 40 + rootOffset; // 40 is C4

        // Get Intervals based on Type
        int[] intervals = getIntervalsForType(type);

        for (int interval : intervals) {
            int noteIndex = rootIndex + interval;
            keys.add(getKeyName(noteIndex));
        }

        return keys;
    }

    private static int[] getIntervalsForType(String type) {
        switch (type) {
            case "maj": return new int[]{0, 4, 7};
            case "min": return new int[]{0, 3, 7};
            case "dim": return new int[]{0, 3, 6};
            case "aug": return new int[]{0, 4, 8};
            case "maj7": return new int[]{0, 4, 7, 11};
            case "min7": return new int[]{0, 3, 7, 10};
            case "7": return new int[]{0, 4, 7, 10};
            case "dim7": return new int[]{0, 3, 6, 9};
            case "m7b5": return new int[]{0, 3, 6, 10};
            default: return new int[]{0}; // Should not happen with fixed picker
        }
    }

    private static String getKeyName(int index) {
        // Determine if white or black
        // Key 4 is C1.
        // (index - 4) % 12 gives pitch class relative to C=0
        int pitchClass = (index - 4) % 12;
        if (pitchClass < 0) pitchClass += 12;

        boolean isBlack = (pitchClass == 1 || pitchClass == 3 || pitchClass == 6 || pitchClass == 8 || pitchClass == 10);

        // Special handling for split black keys if needed, but standard naming:
        // key{index}_white or key{index}_black
        // However, your resource naming convention seems to be:
        // key{index}_white for white keys
        // key{index}_black for black keys
        // Let's verify with a known key. C4 is key 40. 40-4=36. 36%12=0 (C). White. -> key40_white
        // C#4 is key 41. 41-4=37. 37%12=1 (C#). Black. -> key41_black

        // Note: Your black keys might have _part2 suffix in some XMLs, but usually the ID or tag used for logic is simpler.
        // Based on "piano_three_keys.xml" description: "key2_black", "key2_black_part2".
        // The logic in PianoView likely handles the click detection.
        // For highlighting, we usually need the base name.
        // Assuming the standard naming convention holds for the generated keys.

        if (isBlack) {
            return "key" + index + "_black";
        } else {
            return "key" + index + "_white";
        }
    }
}
