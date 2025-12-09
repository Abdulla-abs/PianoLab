package com.example.pianolab.utils;

import java.util.ArrayList;
import java.util.List;

public class TunerHelper {

    public static float calculate_ref_freq(float measured, float referenceA4) {
        double ratio = measured / referenceA4;
        int steps = (int) Math.round(12d * (Math.log(ratio) / Math.log(2d)));
        return (float) (referenceA4 * Math.pow(2d, steps / 12d));
    }
    public static float calculate_deviation_cent(float frequency, float referenceFrequency) {
        if (frequency <= 0 || referenceFrequency <= 0) {
            return 0f;
        }
        double ratio = frequency / referenceFrequency;
        return (float) (1200.0 * Math.log(ratio) / Math.log(2));
    }

    public static float calculate_RMS(float[] samples) {
        float sum = 0f;
        for (float sample : samples) {
            sum += sample * sample;
        }
        return (float) Math.sqrt(sum / samples.length);
    }

    public static float calculate_Median(List<Float> values) {
        if (values.isEmpty()) return 0f;
        List<Float> sorted = new ArrayList<>(values);
        java.util.Collections.sort(sorted);
        int mid = sorted.size() / 2;
        return sorted.size() % 2 == 0
                ? (sorted.get(mid - 1) + sorted.get(mid)) / 2f
                : sorted.get(mid);
    }
}
