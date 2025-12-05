package com.example.pianolab.feature.tuner.model;

public class TunerState {
    private final String targetNote;
    private final float targetFrequency;
    private final String detectedNote;
    private final float detectedFrequency;
    private final boolean listening;

    public TunerState(String targetNote,
                      float targetFrequency,
                      String detectedNote,
                      float detectedFrequency,
                      boolean listening) {
        this.targetNote = targetNote;
        this.targetFrequency = targetFrequency;
        this.detectedNote = detectedNote;
        this.detectedFrequency = detectedFrequency;
        this.listening = listening;
    }

    public static TunerState idle() {
        return new TunerState("A4", 440f, "--", 0f, false);
    }

    public TunerState withListening(boolean listening) {
        return new TunerState(targetNote, targetFrequency, detectedNote, detectedFrequency, listening);
    }

    public TunerState withDetected(String note, float frequency) {
        return new TunerState(targetNote, targetFrequency, note, frequency, listening);
    }

    public String getTargetNote() {
        return targetNote;
    }

    public float getTargetFrequency() {
        return targetFrequency;
    }

    public String getDetectedNote() {
        return detectedNote;
    }

    public float getDetectedFrequency() {
        return detectedFrequency;
    }

    public boolean isListening() {
        return listening;
    }
}

