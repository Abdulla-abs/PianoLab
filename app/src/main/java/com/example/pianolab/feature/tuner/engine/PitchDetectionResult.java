package com.example.pianolab.feature.tuner.engine;

public class PitchDetectionResult {
    private final float frequency;
    private final float probability;

    public PitchDetectionResult(float frequency, float probability) {
        this.frequency = frequency;
        this.probability = probability;
    }

    public float getFrequency() {
        return frequency;
    }

    public float getProbability() {
        return probability;
    }

    public boolean isReliable(float threshold) {
        return probability >= threshold && frequency > 0f;
    }
}

