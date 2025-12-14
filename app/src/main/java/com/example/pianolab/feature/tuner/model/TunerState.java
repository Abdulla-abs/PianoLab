package com.example.pianolab.feature.tuner.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class TunerState {
    private final boolean listening;
    private final boolean autoDetectEnabled;
    private final boolean autoStopEnabled;
    private final float standardFrequency;
    private final String manualNote;
    private final float manualFrequency;
    private final String detectedNote;
    private final float detectedFrequency;
    private final float measuredFrequency;
    private final float deviationCents;
    private final List<Float> waveformSamples;
    private final List<Float> spectrumMagnitudes;

    private TunerState(boolean listening,
                       boolean autoDetectEnabled,
                       boolean autoStopEnabled,
                       float standardFrequency,
                       String manualNote,
                       float manualFrequency,
                       String detectedNote,
                       float detectedFrequency,
                       float measuredFrequency,
                       float deviationCents,
                       List<Float> waveformSamples,
                       List<Float> spectrumMagnitudes) {
        this.listening = listening;
        this.autoDetectEnabled = autoDetectEnabled;
        this.autoStopEnabled = autoStopEnabled;
        this.standardFrequency = standardFrequency;
        this.manualNote = manualNote;
        this.manualFrequency = manualFrequency;
        this.detectedNote = detectedNote;
        this.detectedFrequency = detectedFrequency;
        this.measuredFrequency = measuredFrequency;
        this.deviationCents = deviationCents;
        this.waveformSamples = waveformSamples;
        this.spectrumMagnitudes = spectrumMagnitudes;
    }

    public static TunerState idle() {
        return new TunerState(
                false,
                true,
                true,
                440f,
                "A4",
                440f,
                "--",
                0f,
                0f,
                0f,
                Collections.emptyList(),
                Collections.emptyList()
        );
    }

    public TunerState withListening(boolean listening) {
        return new TunerState(listening, autoDetectEnabled, autoStopEnabled, standardFrequency, manualNote, manualFrequency, detectedNote, detectedFrequency, measuredFrequency, deviationCents, waveformSamples, spectrumMagnitudes);
    }

    public TunerState withAutoDetect(boolean enabled) {
        return new TunerState(listening, enabled, autoStopEnabled, standardFrequency, manualNote, manualFrequency, detectedNote, detectedFrequency, measuredFrequency, deviationCents, waveformSamples, spectrumMagnitudes);
    }

    public TunerState withAutoStop(boolean enabled) {
        return new TunerState(listening, autoDetectEnabled, enabled, standardFrequency, manualNote, manualFrequency, detectedNote, detectedFrequency, measuredFrequency, deviationCents, waveformSamples, spectrumMagnitudes);
    }

    public TunerState withstandardFrequency(float frequency) {
        return new TunerState(listening, autoDetectEnabled, autoStopEnabled, frequency, manualNote, manualFrequency, detectedNote, detectedFrequency, measuredFrequency, deviationCents, waveformSamples, spectrumMagnitudes);
    }

    public TunerState withManualTarget(String note, float frequency) {
        return new TunerState(listening, autoDetectEnabled, autoStopEnabled, standardFrequency, note, frequency, detectedNote, detectedFrequency, measuredFrequency, deviationCents, waveformSamples, spectrumMagnitudes);
    }

    public TunerState withAutoDetection(String note, float frequency) {
        return new TunerState(listening, autoDetectEnabled, autoStopEnabled, standardFrequency, manualNote, manualFrequency, note, frequency, measuredFrequency, deviationCents, waveformSamples, spectrumMagnitudes);
    }

    public TunerState withMeasurement(float frequency) {
        return new TunerState(listening, autoDetectEnabled, autoStopEnabled, standardFrequency, manualNote, manualFrequency, detectedNote, detectedFrequency, frequency, deviationCents, waveformSamples, spectrumMagnitudes);
    }

    public TunerState withDeviation(float cents) {
        return new TunerState(listening, autoDetectEnabled, autoStopEnabled, standardFrequency, manualNote, manualFrequency, detectedNote, detectedFrequency, measuredFrequency, cents, waveformSamples, spectrumMagnitudes);
    }

    public TunerState withWaveform(List<Float> samples) {
        List<Float> immutable = samples == null ? Collections.emptyList() : Collections.unmodifiableList(new ArrayList<>(samples));
        return new TunerState(listening, autoDetectEnabled, autoStopEnabled, standardFrequency, manualNote, manualFrequency, detectedNote, detectedFrequency, measuredFrequency, deviationCents, immutable, spectrumMagnitudes);
    }

    public TunerState withSpectrum(List<Float> magnitudes) {
        List<Float> immutable = magnitudes == null ? Collections.emptyList() : Collections.unmodifiableList(new ArrayList<>(magnitudes));
        return new TunerState(listening, autoDetectEnabled, autoStopEnabled, standardFrequency, manualNote, manualFrequency, detectedNote, detectedFrequency, measuredFrequency, deviationCents, waveformSamples, immutable);
    }

    public boolean isListening() {
        return listening;
    }

    public boolean isAutoDetectEnabled() {
        return autoDetectEnabled;
    }

    public boolean isAutoStopEnabled() {
        return autoStopEnabled;
    }

    public float getstandardFrequency() {
        return standardFrequency;
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

    public List<Float> getSpectrumMagnitudes() {
        return spectrumMagnitudes;
    }

    public List<Float> getWaveformSamplesForMode(boolean frequencyMode) {
        return frequencyMode ? spectrumMagnitudes : waveformSamples;
    }

    public String getDisplayNote() {
        String note = autoDetectEnabled && detectedNote != null && !detectedNote.isEmpty() ? detectedNote : manualNote;
        if (note == null) return "--";

        // 格式化音符显示：将 A#4 转换为 #A4，将 Bb4 转换为 bB4
        if (note.length() >= 3) {
            if (note.contains("#")) {
                int sharpIndex = note.indexOf('#');
                if (sharpIndex > 0) {
                    String noteName = note.substring(0, sharpIndex);
                    String octave = note.substring(sharpIndex + 1);
                    return "#" + noteName + octave;
                }
            } else if (note.contains("b")) {
                int flatIndex = note.indexOf('b');
                if (flatIndex > 0) {
                    String noteName = note.substring(0, flatIndex);
                    String octave = note.substring(flatIndex + 1);
                    return "b" + noteName + octave;
                }
            }
        }

        return note;
    }

    public float getDisplayFrequency() {
        if (autoDetectEnabled && detectedFrequency > 0f) {
            return detectedFrequency;
        }
        return manualFrequency;
    }
}
