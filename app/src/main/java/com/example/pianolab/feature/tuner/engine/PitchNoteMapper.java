package com.example.pianolab.feature.tuner.engine;

public final class PitchNoteMapper {
    private static final double A4_FREQUENCY = 440.0;
    private static final String[] NOTE_NAMES = {
            "C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B"
    };

    private PitchNoteMapper() {
    }

    public static String frequencyToNoteName(float frequency) {
        if (frequency <= 0) {
            return "--";
        }
        double semitonesFromA4 = 12.0 * Math.log(frequency / A4_FREQUENCY) / Math.log(2);
        int midiNumber = (int) Math.round(semitonesFromA4) + 69; // MIDI note for A4 is 69
        int octave = (midiNumber / 12) - 1;
        int noteIndex = Math.floorMod(midiNumber, 12);
        return NOTE_NAMES[noteIndex] + octave;
    }

    public static float centsOff(float frequency, float referenceFrequency) {
        if (frequency <= 0 || referenceFrequency <= 0) {
            return 0f;
        }
        double ratio = frequency / referenceFrequency;
        return (float) (1200.0 * Math.log(ratio) / Math.log(2));
    }
}

