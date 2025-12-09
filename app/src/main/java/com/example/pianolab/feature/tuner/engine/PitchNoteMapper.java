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

}

