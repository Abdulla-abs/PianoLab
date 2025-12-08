package com.example.pianolab.utils;

public class TunerHelper {

    public static float calculate_ref_freq(float measured, float referenceA4) {
        double ratio = measured / referenceA4;
        int steps = (int) Math.round(12d * (Math.log(ratio) / Math.log(2d)));
        return (float) (referenceA4 * Math.pow(2d, steps / 12d));
    }
}
