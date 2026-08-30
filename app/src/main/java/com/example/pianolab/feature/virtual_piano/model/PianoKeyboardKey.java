package com.example.pianolab.feature.virtual_piano.model;

import android.graphics.RectF;

/** Layout + identity for one piano key (88-key range). */
public final class PianoKeyboardKey {
    public static final int MIDI_MIN = 21;
    public static final int MIDI_MAX = 108;

    public final int midi;
    public final int pianoKeyIndex;
    public final PianoPitch pitch;
    public final boolean black;
    public final String label;
    public final String soundKeyName;
    public final RectF bounds = new RectF();

    public PianoKeyboardKey(int midi) {
        this.midi = midi;
        this.pianoKeyIndex = midi - 20;
        this.pitch = PianoPitch.fromMidi(midi);
        this.black = pitch.isBlack();
        this.label = PianoPitch.labelForMidi(midi);
        this.soundKeyName = pitch.soundKeyName(pianoKeyIndex);
    }

    public boolean contains(float x, float y) {
        return bounds.contains(x, y);
    }

    public float centerX() {
        return bounds.centerX();
    }
}
