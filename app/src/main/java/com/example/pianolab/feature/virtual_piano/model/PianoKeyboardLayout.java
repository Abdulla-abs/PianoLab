package com.example.pianolab.feature.virtual_piano.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Computes 88-key geometry from slice aspect ratios and scale. */
public final class PianoKeyboardLayout {
    private static final float SRC_WHITE_W = 70f;
    private static final float SRC_WHITE_H = 340f;
    private static final float SRC_BLACK_W = 42f;
    private static final float SRC_BLACK_H = 210f;

    private final List<PianoKeyboardKey> keys = new ArrayList<>(88);
    private final List<PianoKeyboardKey> whiteKeys = new ArrayList<>(52);
    private final List<PianoKeyboardKey> blackKeys = new ArrayList<>(36);
    private float contentWidth;
    private float whiteKeyWidth;
    private float whiteKeyHeight;
    private float keyGap;

    public PianoKeyboardLayout() {
        for (int midi = PianoKeyboardKey.MIDI_MIN; midi <= PianoKeyboardKey.MIDI_MAX; midi++) {
            PianoKeyboardKey key = new PianoKeyboardKey(midi);
            keys.add(key);
            if (key.black) {
                blackKeys.add(key);
            } else {
                whiteKeys.add(key);
            }
        }
    }

    public List<PianoKeyboardKey> getKeys() {
        return Collections.unmodifiableList(keys);
    }

    public List<PianoKeyboardKey> getWhiteKeys() {
        return Collections.unmodifiableList(whiteKeys);
    }

    public List<PianoKeyboardKey> getBlackKeys() {
        return Collections.unmodifiableList(blackKeys);
    }

    public float getContentWidth() {
        return contentWidth;
    }

    public float getWhiteKeyWidth() {
        return whiteKeyWidth;
    }

    public float getWhiteKeyHeight() {
        return whiteKeyHeight;
    }

    public float getKeyGap() {
        return keyGap;
    }

    public float getOctaveWidth() {
        return whiteKeyWidth * 7f + keyGap * 7f;
    }

    public PianoKeyboardKey findByMidi(int midi) {
        int index = midi - PianoKeyboardKey.MIDI_MIN;
        if (index < 0 || index >= keys.size()) {
            return null;
        }
        return keys.get(index);
    }

    public void layout(float viewportHeight, float keyScale, float gapPx) {
        keyGap = Math.max(0f, gapPx);
        whiteKeyHeight = Math.max(1f, viewportHeight);
        whiteKeyWidth = whiteKeyHeight * (SRC_WHITE_W / SRC_WHITE_H) * Math.max(0.4f, keyScale);

        float blackKeyWidth = whiteKeyWidth * (SRC_BLACK_W / SRC_WHITE_W);
        float blackKeyHeight = whiteKeyHeight * (SRC_BLACK_H / SRC_WHITE_H);

        float x = 0f;
        float lastWhiteLeft = -1f;

        for (PianoKeyboardKey key : keys) {
            if (!key.black) {
                key.bounds.left = x;
                key.bounds.top = 0f;
                key.bounds.right = x + whiteKeyWidth;
                key.bounds.bottom = whiteKeyHeight;
                lastWhiteLeft = x;
                x += whiteKeyWidth + keyGap;
            } else {
                float left = lastWhiteLeft + whiteKeyWidth - blackKeyWidth * 0.5f + keyGap * 0.5f;
                key.bounds.left = left;
                key.bounds.top = 0f;
                key.bounds.right = left + blackKeyWidth;
                key.bounds.bottom = blackKeyHeight;
            }
        }

        contentWidth = Math.max(0f, x - keyGap);
    }
}
