package com.example.pianolab.feature.virtual_piano.model;

import androidx.annotation.DrawableRes;

import com.example.pianolab.R;

/** Chromatic pitch class for one-octave key slice assets. */
public enum PianoPitch {
    C(R.drawable.piano_key_c, R.drawable.piano_key_c_pressed),
    C_SHARP(R.drawable.piano_key_c_sharp, R.drawable.piano_key_c_sharp_pressed),
    D(R.drawable.piano_key_d, R.drawable.piano_key_d_pressed),
    D_SHARP(R.drawable.piano_key_d_sharp, R.drawable.piano_key_d_sharp_pressed),
    E(R.drawable.piano_key_e, R.drawable.piano_key_e_pressed),
    F(R.drawable.piano_key_f, R.drawable.piano_key_f_pressed),
    F_SHARP(R.drawable.piano_key_f_sharp, R.drawable.piano_key_f_sharp_pressed),
    G(R.drawable.piano_key_g, R.drawable.piano_key_g_pressed),
    G_SHARP(R.drawable.piano_key_g_sharp, R.drawable.piano_key_g_sharp_pressed),
    A(R.drawable.piano_key_a, R.drawable.piano_key_a_pressed),
    A_SHARP(R.drawable.piano_key_a_sharp, R.drawable.piano_key_a_sharp_pressed),
    B(R.drawable.piano_key_b, R.drawable.piano_key_b_pressed);

    private static final String[] MIDI_NAMES = {
        "C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B"
    };

    public final int normalRes;
    public final int pressedRes;

    PianoPitch(@DrawableRes int normalRes, @DrawableRes int pressedRes) {
        this.normalRes = normalRes;
        this.pressedRes = pressedRes;
    }

    public static PianoPitch fromMidi(int midi) {
        return values()[midi % 12];
    }

    public static String labelForMidi(int midi) {
        return MIDI_NAMES[midi % 12] + (midi / 12 - 1);
    }

    public boolean isBlack() {
        return this == C_SHARP
                || this == D_SHARP
                || this == F_SHARP
                || this == G_SHARP
                || this == A_SHARP;
    }

    public String soundKeyName(int pianoKeyIndex) {
        return "key" + pianoKeyIndex + (isBlack() ? "_black" : "_white");
    }
}
