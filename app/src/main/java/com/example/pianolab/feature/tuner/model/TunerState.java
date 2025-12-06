package com.example.pianolab.feature.tuner.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class TunerState {
    private final boolean listening;
    private final boolean autoDetectEnabled;
    private final boolean noiseFilterEnabled;
    private final float referenceFrequency;
    private final String manualNote;
    private final float manualFrequency;
    private final String detectedNote;
    private final float detectedFrequency;
    private final float measuredFrequency;
    private final float deviationCents;
    private final List<Float> waveformSamples;

    private TunerState(boolean listening,
                       boolean autoDetectEnabled,
                       boolean noiseFilterEnabled,
                       float referenceFrequency,
                       String manualNote,
                       float manualFrequency,
                       String detectedNote,
                       float detectedFrequency,
                       float measuredFrequency,
                       float deviationCents,
                       List<Float> waveformSamples) {
        this.listening = listening;
        this.autoDetectEnabled = autoDetectEnabled;
        this.noiseFilterEnabled = noiseFilterEnabled;
        this.referenceFrequency = referenceFrequency;
        this.manualNote = manualNote;
        this.manualFrequency = manualFrequency;
        this.detectedNote = detectedNote;
        this.detectedFrequency = detectedFrequency;
        this.measuredFrequency = measuredFrequency;
        this.deviationCents = deviationCents;
        this.waveformSamples = waveformSamples;
    }

    public static TunerState idle() {
        return new TunerState(
                false,
                true,
                false,
                440f,
                "A4",
                440f,
                "--",
                0f,
                0f,
                0f,
                Collections.emptyList()
        );
    }

    public TunerState withListening(boolean listening) {
        return new TunerState(listening, autoDetectEnabled, noiseFilterEnabled, referenceFrequency, manualNote, manualFrequency, detectedNote, detectedFrequency, measuredFrequency, deviationCents, waveformSamples);
    }

    public TunerState withAutoDetect(boolean enabled) {
        return new TunerState(listening, enabled, noiseFilterEnabled, referenceFrequency, manualNote, manualFrequency, detectedNote, detectedFrequency, measuredFrequency, deviationCents, waveformSamples);
    }

    public TunerState withNoiseFilter(boolean enabled) {
        return new TunerState(listening, autoDetectEnabled, enabled, referenceFrequency, manualNote, manualFrequency, detectedNote, detectedFrequency, measuredFrequency, deviationCents, waveformSamples);
    }

    public TunerState withReferenceFrequency(float frequency) {
        return new TunerState(listening, autoDetectEnabled, noiseFilterEnabled, frequency, manualNote, manualFrequency, detectedNote, detectedFrequency, measuredFrequency, deviationCents, waveformSamples);
    }

    public TunerState withManualTarget(String note, float frequency) {
        return new TunerState(listening, autoDetectEnabled, noiseFilterEnabled, referenceFrequency, note, frequency, detectedNote, detectedFrequency, measuredFrequency, deviationCents, waveformSamples);
    }

    public TunerState withAutoDetection(String note, float frequency) {
        return new TunerState(listening, autoDetectEnabled, noiseFilterEnabled, referenceFrequency, manualNote, manualFrequency, note, frequency, measuredFrequency, deviationCents, waveformSamples);
    }

    public TunerState withMeasurement(float frequency) {
        return new TunerState(listening, autoDetectEnabled, noiseFilterEnabled, referenceFrequency, manualNote, manualFrequency, detectedNote, detectedFrequency, frequency, deviationCents, waveformSamples);
    }

    public TunerState withDeviation(float cents) {
        return new TunerState(listening, autoDetectEnabled, noiseFilterEnabled, referenceFrequency, manualNote, manualFrequency, detectedNote, detectedFrequency, measuredFrequency, cents, waveformSamples);
    }

    public TunerState withWaveform(List<Float> samples) {
        List<Float> immutable = samples == null ? Collections.emptyList() : Collections.unmodifiableList(new ArrayList<>(samples));
        return new TunerState(listening, autoDetectEnabled, noiseFilterEnabled, referenceFrequency, manualNote, manualFrequency, detectedNote, detectedFrequency, measuredFrequency, deviationCents, immutable);
    }

    public boolean isListening() {
        return listening;
    }

    public boolean isAutoDetectEnabled() {
        return autoDetectEnabled;
    }

    public boolean isNoiseFilterEnabled() {
        return noiseFilterEnabled;
    }

    public float getReferenceFrequency() {
        return referenceFrequency;
    }

    public String getManualNote() {
        return manualNote;
    }

    public float getManualFrequency() {
        return manualFrequency;
    }

    public String getDetectedNote() {
        return detectedNote;
    }

    public float getDetectedFrequency() {
        return detectedFrequency;
    }

    public float getMeasuredFrequency() {
        return measuredFrequency;
    }

    public float getDeviationCents() {
        return deviationCents;
    }

    public List<Float> getWaveformSamples() {
        return waveformSamples;
    }

    public String getDisplayNote() {
        return autoDetectEnabled ? detectedNote : manualNote;
    }

    public float getDisplayFrequency() {
        return autoDetectEnabled ? detectedFrequency : manualFrequency;
    }
}
