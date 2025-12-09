package com.example.pianolab.feature.tuner.engine;

import java.util.Arrays;

class NoiseFilter {
    private static final float GATE_THRESHOLD = 0.012f;
    private final float alpha;

    NoiseFilter(float sampleRate) {
        float cutoff = 1200f;
        float rc = 1f / (2f * (float) Math.PI * cutoff);
        float dt = 1f / sampleRate;
        this.alpha = dt / (rc + dt);
    }

    void apply(float[] buffer) {
        if (buffer == null || buffer.length == 0) {
            return;
        }
        float sum = 0f;
        for (float sample : buffer) {
            sum += sample * sample;
        }
        float rms = (float) Math.sqrt(sum / buffer.length);
        if (rms < GATE_THRESHOLD) {
            Arrays.fill(buffer, 0f);
            return;
        }
        float prev = 0f;
        for (int i = 0; i < buffer.length; i++) {
            prev += alpha * (buffer[i] - prev);
            buffer[i] = prev;
        }
    }
}