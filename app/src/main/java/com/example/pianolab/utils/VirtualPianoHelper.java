package com.example.pianolab.utils;

import android.content.Context;
import android.content.res.XmlResourceParser;
import android.graphics.Path;
import android.graphics.drawable.Drawable;
import android.util.Log;

import androidx.core.graphics.PathParser;

import org.xmlpull.v1.XmlPullParser;

import java.util.HashMap;
import java.util.Map;

public class VirtualPianoHelper {
    private static final String TAG = "VirtualPianoHelper";
    private static final String ANDROID_NS = "http://schemas.android.com/apk/res/android";


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

    public static Map<String, Path> loadNamedPathsFromVector(Context ctx, int resId, boolean normalizeOcPrefix) {
        Map<String, Path> result = new HashMap<>();
        XmlResourceParser parser = null;
        try {
            parser = ctx.getResources().getXml(resId);
            int et = parser.getEventType();
            while (et != XmlPullParser.END_DOCUMENT) {
                if (et == XmlPullParser.START_TAG) {
                    String tag = parser.getName();
                    if ("path".equals(tag) || "clip-path".equals(tag)) {
                        String name = parser.getAttributeValue(ANDROID_NS, "name");
                        String pathData = parser.getAttributeValue(ANDROID_NS, "pathData");
                        if (name != null && pathData != null) {
                            try {
                                Path p = PathParser.createPathFromPathData(pathData);
                                if (p != null) {
                                    // 保留首次解析到的同名 path（避免覆盖）
                                    if (!result.containsKey(name)) result.put(name, p);
                                    // 针对 piano_keys 的 ocN_ 前缀归一化（可选）
                                    if (normalizeOcPrefix) {
                                        String normalized = name.replaceFirst("^oc\\d+_", "");
                                        if (!normalized.equals(name) && !result.containsKey(normalized)) {
                                            Path copy = new Path();
                                            copy.addPath(p);
                                            result.put(normalized, copy);
                                        }
                                    }
                                }
                            } catch (Exception e) {
                                Log.w(TAG, "parse path failed name=" + name + " res=" + resId, e);
                            }
                        }
                    }
                }
                et = parser.next();
            }
        } catch (Exception e) {
            Log.w(TAG, "loadNamedPathsFromVector failed res=" + resId, e);
        } finally {
            if (parser != null) parser.close();
        }
        return result;
    }

    /** 便捷重载：默认不做 ocN_ 归一化 */
    public static Map<String, Path> loadNamedPathsFromVector(Context ctx, int resId) {
        return loadNamedPathsFromVector(ctx, resId, false);
    }


}
