package com.example.pianolab.feature.tuner.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Region;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

import androidx.core.content.ContextCompat;

import com.example.pianolab.R;
import com.example.pianolab.feature.virtual_piano.engine.PianoPaintEngine;
import com.example.pianolab.utils.MusicTheoryHelper;
import com.example.pianolab.utils.VirtualPianoHelper;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * 调音器专用简化钢琴视图
 * - 仅显示一个完整八度(12键)
 * - 不响应触摸
 * - 音名标签仅显示字母(C, D, E...)
 */
public class TunerPianoView extends View {
    private static final int DEFAULT_OCTAVE_W = 375;
    private static final int DEFAULT_OCTAVE_H = 323;
    private static final String TAG = "TunerPianoView";

    private final PianoPaintEngine paintEngine;
    private Drawable octaveDrawable;
    private Paint fallbackPaint;

    private final Map<String, Path> keyPrototypes = new HashMap<>();
    private final Map<String, Path> keyTransformed = new HashMap<>();
    private final Map<String, Region> keyRegions = new HashMap<>();
    private final Map<String, String> noteLabels = new HashMap<>();

    private final Set<String> highlightedKeys = new HashSet<>();
    private String currentNote = "--";

    private Paint highlightWhite;
    private Paint highlightBlack;

    private int contentWidth;
    private int contentHeight;

    private Path keyPathWhite1Transformed;
    private Path keyPathWhite2Transformed;
    private Path keyPathBlackTransformed;
    private Path keyPathBlackPart2Transformed;

    public TunerPianoView(Context context) {
        this(context, null);
    }

    public TunerPianoView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public TunerPianoView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);

        paintEngine = new PianoPaintEngine();
        octaveDrawable = ContextCompat.getDrawable(context, R.drawable.piano_keys);

        fallbackPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        fallbackPaint.setColor(0xFFE0E0E0);

        highlightWhite = new Paint(Paint.ANTI_ALIAS_FLAG);
        highlightWhite.setStyle(Paint.Style.FILL);
        highlightWhite.setColor(0x99FF9800);

        highlightBlack = new Paint(Paint.ANTI_ALIAS_FLAG);
        highlightBlack.setStyle(Paint.Style.FILL);
        highlightBlack.setColor(0x995400FF);

        initKeyData();
    }

    private void initKeyData() {
        Map<String, Path> namedKeys = VirtualPianoHelper.loadNamedPathsFromVector(
                getContext(), R.drawable.piano_keys, true);

        Set<Integer> white_key_idx = Set.of(0, 2, 4, 5, 7, 9, 11);
        for (int k = 1; k <= 12; k++) {
            String whiteName = "key" + k + "_white";
            String blackName = "key" + k + "_black";

            if (namedKeys.containsKey(whiteName)) {
                keyPrototypes.put(whiteName, new Path(namedKeys.get(whiteName)));
                keyTransformed.put(whiteName, new Path());
                keyRegions.put(whiteName, new Region());
                if (white_key_idx.contains(k-1)) {
                    noteLabels.put(whiteName, MusicTheoryHelper.SIMPLE_NOTE_NAME_WITH_SHARP[k - 1]);
                }
            }

            if (namedKeys.containsKey(blackName)) {
                keyPrototypes.put(blackName, new Path(namedKeys.get(blackName)));
                keyTransformed.put(blackName, new Path());
                keyRegions.put(blackName, new Region());
            }
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        contentHeight = MeasureSpec.getSize(heightMeasureSpec);

        int intrinsicW = (octaveDrawable != null)
                ? VirtualPianoHelper.safeIntrinsicWidth(octaveDrawable, DEFAULT_OCTAVE_W)
                : DEFAULT_OCTAVE_W;
        int intrinsicH = (octaveDrawable != null)
                ? VirtualPianoHelper.safeIntrinsicHeight(octaveDrawable, DEFAULT_OCTAVE_H)
                : DEFAULT_OCTAVE_H;

        float scale = (float) contentHeight / (float) Math.max(intrinsicH, 1);
        contentWidth = Math.max(1, Math.round(intrinsicW * scale));

        setMeasuredDimension(contentWidth, contentHeight);
        updateKeyTransforms();
    }

    private void updateKeyTransforms() {
        float scaleX = (float) contentWidth / DEFAULT_OCTAVE_W;
        float scaleY = (float) contentHeight / DEFAULT_OCTAVE_H;

        Matrix matrix = new Matrix();
        matrix.setScale(scaleX, scaleY);

        Region clip = new Region(0, 0, contentWidth, contentHeight);

        // 重置合并路径
        keyPathBlackTransformed = new Path();
        keyPathBlackPart2Transformed = new Path();

        for (Map.Entry<String, Path> entry : keyPrototypes.entrySet()) {
            String name = entry.getKey();
            Path proto = entry.getValue();

            Path transformed = keyTransformed.get(name);
            transformed.reset();
            transformed.addPath(proto, matrix);

            Region region = keyRegions.get(name);
            region.setEmpty();
            region.setPath(transformed, clip);

            // 收集特殊路径用于 drawHighlights
            if ("key1_white".equals(name)) {
                keyPathWhite1Transformed = new Path(transformed);
            } else if ("key3_white".equals(name)) {
                keyPathWhite2Transformed = new Path(transformed);
            } else if (name.endsWith("_black") && !name.endsWith("_black_part2")) {
                keyPathBlackTransformed.addPath(transformed);
            } else if (name.endsWith("_black_part2")) {
                keyPathBlackPart2Transformed.addPath(transformed);
            }
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        paintEngine.drawSingleOctave(
                canvas,
                octaveDrawable,
                fallbackPaint,
                contentWidth,
                contentHeight,
                highlightedKeys,
                keyTransformed,
                highlightWhite,
                highlightBlack
        );

        paintEngine.drawSimplifiedNoteLabels(
                canvas,
                noteLabels,
                keyRegions,
                contentHeight
        );

        paintEngine.drawHighlights(
                canvas,
                highlightedKeys,
                keyTransformed,
                keyPathWhite1Transformed,
                keyPathWhite2Transformed,
                keyPathBlackTransformed,
                keyPathBlackPart2Transformed,
                highlightWhite,
                highlightBlack
        );
    }

    /**
     * 设置当前检测到的音符(如 "C4", "D#5")
     */
    public void setDetectedNote(String note) {
        if (note == null || note.equals(currentNote) || note.equals("--")) return;
//        Log.d(TAG, "note is " + note);

        currentNote = note;
        highlightedKeys.clear();

        String noteName = note.replaceAll("\\d", "");
        boolean is_black = noteName.contains("#") || noteName.contains("b");
        int keyIndex = MusicTheoryHelper.NOTE_TO_INDEX.get(note.replaceAll("\\d+$", ""));

        String keyName = "key" + keyIndex + (is_black ? "_black" : "_white");
        if (keyTransformed.containsKey(keyName)) {
            highlightedKeys.add(keyName);
        }

        invalidate();
    }

    public int getContentWidth() {
        return contentWidth;
    }
}
