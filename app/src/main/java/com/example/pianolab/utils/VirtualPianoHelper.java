package com.example.pianolab.utils;

import android.graphics.drawable.Drawable;
import android.util.Log;

public class VirtualPianoHelper {
    private static final String TAG = "VirtualPianoHelper";


    public static int safeIntrinsicWidth(Drawable d,int DEFAULT_OCTAVE_W) {
        try {
            int w = d.getIntrinsicWidth();
            return (w > 0) ? w : DEFAULT_OCTAVE_W;
        } catch (Exception e) {
            Log.w(TAG, "safeIntrinsicWidth failed", e);
            return DEFAULT_OCTAVE_W;
        }
    }

    public static int safeIntrinsicHeight(Drawable d,int DEFAULT_OCTAVE_H) {
        try {
            int h = d.getIntrinsicHeight();
            return (h > 0) ? h : DEFAULT_OCTAVE_H;
        } catch (Exception e) {
            Log.w(TAG, "safeIntrinsicHeight failed", e);
            return DEFAULT_OCTAVE_H;
        }
    }

    public static int[] calculate_4_direction(float left,float top,float right,float bottom,int REGION_PAD_PX){
        int [] directions = new int[4];
        directions[0] = (int) Math.floor(left) - REGION_PAD_PX;
        directions[1] = (int) Math.floor(top) - REGION_PAD_PX;
        directions[2] = (int) Math.ceil(right) + REGION_PAD_PX;
        directions[3] = (int) Math.ceil(bottom) + REGION_PAD_PX;
        return directions;

    }

}
