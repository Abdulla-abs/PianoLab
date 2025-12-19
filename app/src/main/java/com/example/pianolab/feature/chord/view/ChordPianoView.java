package com.example.pianolab.feature.chord.view;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.Region;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

import androidx.core.content.ContextCompat;

import com.example.pianolab.R;
import com.example.pianolab.feature.virtual_piano.engine.PianoPaintEngine;
import com.example.pianolab.feature.virtual_piano.engine.PianoSoundEngine;
import com.example.pianolab.utils.VirtualPianoHelper;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class ChordPianoView extends View {
    private static final String TAG = "ChordPianoView";
    private Drawable dOctave;
    private Drawable dThreeKeys;
    private Drawable dFiveKeys; // 新增：F2-B2 部分

    // Range: F2 (key 21) to E5 (key 56)
    private static final int START_KEY_INDEX = 21;
    private static final int END_KEY_INDEX = 56;

    // Split points
    private static final int SECTION_1_END = 27; // B2
    private static final int SECTION_2_END = 51; // B4 (covers C3-B4)

    private int contentWidthPx = 0;
    private int contentHeightPx = 0;

    private int swOct = 0;
    private int swThree = 0;
    private int swFive = 0; // 新增

    // Selection logic
    private LinkedList<String> selectedKeys = new LinkedList<>();
    private String lastPlayedKey = null;

    // Rendering
    private Map<String, Path> keyPrototypeMap = new HashMap<>();
    private Map<String, Path> keyTransformedMap = new HashMap<>();
    private Map<String, Region> keyRegionMap = new HashMap<>();
    private List<String> allKeyNames = new ArrayList<>();

    private Paint fallbackPaintOctave;

    private Path keyPathBlackTransformed;
    private Region keyRegionWhite, keyRegionBlack;
    private static final int REGION_PAD_PX = 2;

    private Paint keyHighlightWhitePaint, keyHighlightBlackPaint;

    private boolean showPitchNames = true;
    private final Map<String, String> keyNoteNameMap = new HashMap<>();
    private Paint noteTextPaint;

    private PianoSoundEngine soundEngine;
    private final PianoPaintEngine paintEngine;

    private OnKeyToggledListener onKeyToggledListener;

    public interface OnKeyToggledListener {
        void onKeyToggled(String keyName);
    }

    public void setOnKeyToggledListener(OnKeyToggledListener listener) {
        this.onKeyToggledListener = listener;
    }

    private static final int DEFAULT_OCTAVE_W = 375;
    private static final int DEFAULT_OCTAVE_H = 323;

    public ChordPianoView(Context context) { this(context, null); }
    public ChordPianoView(Context context, AttributeSet attrs) { this(context, attrs, 0); }
    public ChordPianoView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);

        try { dOctave = ContextCompat.getDrawable(context, R.drawable.piano_keys); } catch (Exception e) { Log.e(TAG, "load piano_keys failed", e); dOctave = null; }
        try { dThreeKeys = ContextCompat.getDrawable(context, R.drawable.piano_three_keys); } catch (Exception e) { Log.e(TAG, "load piano_three_keys failed", e); dThreeKeys = null; }
        try { dFiveKeys = ContextCompat.getDrawable(context, R.drawable.piano_five_keys); } catch (Exception e) { Log.e(TAG, "load piano_five_keys failed", e); dFiveKeys = null; }

        fallbackPaintOctave = new Paint(Paint.ANTI_ALIAS_FLAG);
        fallbackPaintOctave.setColor(Color.parseColor("#e0e0e0"));

        soundEngine = new PianoSoundEngine(context);
        paintEngine = new PianoPaintEngine();

        noteTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        noteTextPaint.setColor(0xFF1E88E5);
        noteTextPaint.setStyle(Paint.Style.FILL);
        noteTextPaint.setTextAlign(Paint.Align.CENTER);

        initKeyHitTest();
    }

    @SuppressLint("ResourceType")
    private void initKeyHitTest() {
        keyHighlightWhitePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        keyHighlightWhitePaint.setStyle(Paint.Style.FILL);
        keyHighlightWhitePaint.setColor(0x99FF9800);
        keyHighlightBlackPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        keyHighlightBlackPaint.setStyle(Paint.Style.FILL);
        keyHighlightBlackPaint.setColor(0x995400FF);

        keyPrototypeMap.clear();
        keyTransformedMap.clear();
        keyRegionMap.clear();
        allKeyNames.clear();

        keyPathBlackTransformed = null;

        // Load templates
        Map<String, Path> namedKeys  = VirtualPianoHelper.loadNamedPathsFromVector(getContext(), R.drawable.piano_keys, true);
        Map<String, Path> namedThreeKeys = VirtualPianoHelper.loadNamedPathsFromVector(getContext(), R.drawable.piano_three_keys, true);
        Map<String, Path> namedFiveKeys = VirtualPianoHelper.loadNamedPathsFromVector(getContext(), R.drawable.piano_five_keys, true);

        for (int i = START_KEY_INDEX; i <= END_KEY_INDEX; i++) {
            String relName;
            Map<String, Path> sourceMap;

            // Calculate template index (1-12)
            // C=1, C#=2 ... F=6 ... B=12
            int templateIndex = (i - 4) % 12 + 1;

            if (i <= SECTION_1_END) {
                // F2 - B2 (Keys 21-27) -> Use piano_five_keys
                // Template indices will be 6(F) to 12(B)
                relName = "key" + templateIndex;
                sourceMap = namedFiveKeys;
            } else if (i <= SECTION_2_END) {
                // C3 - B4 (Keys 28-51) -> Use piano_keys (Full Octaves)
                relName = "key" + templateIndex;
                sourceMap = namedKeys;
            } else {
                // C5 - E5 (Keys 52-56) -> Use piano_three_keys
                // Template indices will be 1(C) to 5(E)
                relName = "key" + templateIndex;
                sourceMap = namedThreeKeys;
            }

            String[] suffs = new String[] { "_white", "_black", "_black_part2" };
            for (String s : suffs) {
                String relFull = relName + s;
                Path tmpl = sourceMap.get(relFull);
                if (tmpl == null) continue;

                String absName = "key" + i + s;
                if (!keyPrototypeMap.containsKey(absName)) {
                    Path copy = new Path();
                    copy.addPath(tmpl);
                    keyPrototypeMap.put(absName, copy);
                }
            }
        }

        // Initialize containers
        for (int i = START_KEY_INDEX; i <= END_KEY_INDEX; i++) {
            String whiteName = "key" + i + "_white";
            String blackName = "key" + i + "_black";
            String blackPart2Name = "key" + i + "_black_part2";

            if (keyPrototypeMap.containsKey(whiteName)) {
                allKeyNames.add(whiteName);
                keyTransformedMap.put(whiteName, new Path());
                keyRegionMap.put(whiteName, new Region());
            }
            if (keyPrototypeMap.containsKey(blackName)) {
                allKeyNames.add(blackName);
                keyTransformedMap.put(blackName, new Path());
                keyRegionMap.put(blackName, new Region());
            }
            if (keyPrototypeMap.containsKey(blackPart2Name)) {
                allKeyNames.add(blackPart2Name);
                keyTransformedMap.put(blackPart2Name, new Path());
                keyRegionMap.put(blackPart2Name, new Region());
            }
        }

        ensureNoteLabelMap();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int availableH = MeasureSpec.getSize(heightMeasureSpec);
        contentHeightPx = availableH;

        int wOct = (dOctave != null) ? VirtualPianoHelper.safeIntrinsicWidth(dOctave, DEFAULT_OCTAVE_W) : DEFAULT_OCTAVE_W;
        int hOct = (dOctave != null) ? VirtualPianoHelper.safeIntrinsicHeight(dOctave, DEFAULT_OCTAVE_H) : DEFAULT_OCTAVE_H;

        // Estimate widths based on white key count ratio if drawable not loaded
        // Full octave = 7 white keys
        // Five keys (F-B) = 4 white keys -> 4/7
        // Three keys (C-E) = 3 white keys -> 3/7

        int defaultFiveW = (int)(DEFAULT_OCTAVE_W * 4f / 7f);
        int wFive = (dFiveKeys != null) ? VirtualPianoHelper.safeIntrinsicWidth(dFiveKeys, defaultFiveW) : defaultFiveW;

        int defaultThreeW = (int)(DEFAULT_OCTAVE_W * 3f / 7f);
        int wThree = (dThreeKeys != null) ? VirtualPianoHelper.safeIntrinsicWidth(dThreeKeys, defaultThreeW) : defaultThreeW;

        float scale = (float) contentHeightPx / (float) Math.max(hOct, 1);

        swOct = Math.max(1, Math.round(wOct * scale));
        swFive = Math.max(1, Math.round(wFive * scale));
        swThree = Math.max(1, Math.round(wThree * scale));

        // Total Width: 1x FiveKeys + 2x FullOctave + 1x ThreeKeys
        contentWidthPx = swFive + (swOct * 2) + swThree;

        if (dFiveKeys != null) dFiveKeys.setBounds(0, 0, swFive, contentHeightPx);
        if (dOctave != null) dOctave.setBounds(0, 0, swOct, contentHeightPx); // Bounds will be shifted in onDraw
        if (dThreeKeys != null) dThreeKeys.setBounds(0, 0, swThree, contentHeightPx);

        setMeasuredDimension(contentWidthPx, contentHeightPx);

        updateKeyTransforms(contentHeightPx);
    }

    private void updateKeyTransforms(int destHeight) {
        if (keyPrototypeMap.isEmpty()) return;

        Region fullClip = new Region(0, 0, Math.max(1, contentWidthPx), Math.max(1, contentHeightPx));

        if (keyPathBlackTransformed == null) keyPathBlackTransformed = new Path();
        keyPathBlackTransformed.reset();
        if (keyRegionBlack == null) keyRegionBlack = new Region();
        keyRegionBlack.setEmpty();

        if (keyRegionWhite == null) keyRegionWhite = new Region();
        keyRegionWhite.setEmpty();

        final float octaveVW = DEFAULT_OCTAVE_W;
        final float octaveVH = DEFAULT_OCTAVE_H;

        // Viewport widths for partials (proportional to white keys)
        float fiveVW = octaveVW * 4f / 7f;
        float threeVW = octaveVW * 3f / 7f;

        // Adjust if intrinsics are available
        if (dFiveKeys != null) {
            int iw = dFiveKeys.getIntrinsicWidth();
            int ih = dFiveKeys.getIntrinsicHeight();
            if (iw > 0 && ih > 0) fiveVW = octaveVH * ((float) iw / ih);
        }
        if (dThreeKeys != null) {
            int iw = dThreeKeys.getIntrinsicWidth();
            int ih = dThreeKeys.getIntrinsicHeight();
            if (iw > 0 && ih > 0) threeVW = octaveVH * ((float) iw / ih);
        }

        for (Map.Entry<String, Path> entry : keyPrototypeMap.entrySet()) {
            String name = entry.getKey();
            Path proto = entry.getValue();
            if (proto == null) continue;

            int idx = -1;
            try {
                int us = name.indexOf('_');
                if (name.startsWith("key") && us > 3) {
                    idx = Integer.parseInt(name.substring(3, us));
                }
            } catch (Exception ignore) { idx = -1; }

            if (idx < START_KEY_INDEX || idx > END_KEY_INDEX) continue;

            float absOffsetX;
            float destW;
            float vw, vh;

            if (idx <= SECTION_1_END) {
                // F2-B2
                absOffsetX = 0;
                destW = swFive;
                vw = fiveVW;
                vh = octaveVH;
            } else if (idx <= SECTION_2_END) {
                // C3-B4 (2 Octaves)
                // Relative octave index: 0 for C3-B3, 1 for C4-B4
                int relativeOctave = (idx - 28) / 12;
                absOffsetX = swFive + (relativeOctave * swOct);
                destW = swOct;
                vw = octaveVW;
                vh = octaveVH;
            } else {
                // C5-E5
                absOffsetX = swFive + (2 * swOct);
                destW = swThree;
                vw = threeVW;
                vh = octaveVH;
            }

            Matrix m = new Matrix();
            float sx = destW / Math.max(1f, vw);
            float sy = (float) destHeight / Math.max(1f, vh);
            m.setScale(sx, sy);
            m.postTranslate(absOffsetX, 0);

            Path transformed = keyTransformedMap.get(name);
            if (transformed == null) {
                transformed = new Path();
                keyTransformedMap.put(name, transformed);
            }
            transformed.reset();
            transformed.addPath(proto, m);

            Region region = keyRegionMap.get(name);
            if (region == null) {
                region = new Region();
                keyRegionMap.put(name, region);
            }
            region.setEmpty();

            boolean ok = false;
            try {
                region.setPath(transformed, fullClip);
                if (!region.isEmpty()) ok = true;
            } catch (Exception e) { ok = false; }

            if (!ok) {
                android.graphics.RectF rf = new android.graphics.RectF();
                try { transformed.computeBounds(rf, true); } catch (Exception e) { rf.set(0,0,0,0); }
                int[] dirs = VirtualPianoHelper.calculate_4_direction(rf.left, rf.top, rf.right, rf.bottom, REGION_PAD_PX);
                Region boundsClip = new Region(Math.max(0, dirs[0]), Math.max(0, dirs[1]),
                        Math.min(contentWidthPx, dirs[2]), Math.min(contentHeightPx, dirs[3]));
                try {
                    region.setPath(transformed, boundsClip);
                } catch (Exception e2) {
                    region.set(boundsClip);
                }
            }

            if (name.endsWith("_white")) {
                keyRegionWhite.op(region, Region.Op.UNION);
            } else if (name.endsWith("_black") && !name.endsWith("_black_part2")) {
                keyPathBlackTransformed.addPath(transformed);
                keyRegionBlack.op(region, Region.Op.UNION);
            }
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        // 1. Draw F2-B2 (Five Keys)
        if (dFiveKeys != null) {
            dFiveKeys.setBounds(0, 0, swFive, contentHeightPx);
            dFiveKeys.draw(canvas);
        } else {
            canvas.drawRect(0, 0, swFive, contentHeightPx, fallbackPaintOctave);
        }

        // 2. Draw C3-B4 (2 Full Octaves)
        if (dOctave != null) {
            for (int i = 0; i < 2; i++) {
                int left = swFive + (i * swOct);
                dOctave.setBounds(left, 0, left + swOct, contentHeightPx);
                dOctave.draw(canvas);
            }
        } else {
            canvas.drawRect(swFive, 0, swFive + swOct * 2, contentHeightPx, fallbackPaintOctave);
        }

        // 3. Draw C5-E5 (Three Keys)
        int leftThree = swFive + (swOct * 2);
        if (dThreeKeys != null) {
            dThreeKeys.setBounds(leftThree, 0, leftThree + swThree, contentHeightPx);
            dThreeKeys.draw(canvas);
        } else {
            canvas.drawRect(leftThree, 0, leftThree + swThree, contentHeightPx, fallbackPaintOctave);
        }

        paintEngine.drawHighlights(
                canvas,
                new HashSet<>(selectedKeys),
                keyTransformedMap,
                null,
                null,
                keyPathBlackTransformed,
                null,
                keyHighlightWhitePaint,
                keyHighlightBlackPaint
        );

        paintEngine.drawNoteLabels(
                canvas,
                showPitchNames,
                keyNoteNameMap,
                keyRegionMap,
                contentHeightPx
        );
    }

    private String hitTestKeyAt(int x, int y) {
        // Same logic as PianoView
        if (allKeyNames != null && keyRegionMap != null && !allKeyNames.isEmpty()) {
            for (String name : allKeyNames) {
                if (name.endsWith("_black") && !name.endsWith("_black_part2")) {
                    Region r = keyRegionMap.get(name);
                    if (r != null && r.contains(x, y)) return name;
                }
            }
            for (String name : allKeyNames) {
                if (name.endsWith("_black_part2")) {
                    Region r = keyRegionMap.get(name);
                    if (r != null && r.contains(x, y)) return name;
                }
            }
            for (String name : allKeyNames) {
                if (name.endsWith("_white")) {
                    Region r = keyRegionMap.get(name);
                    if (r != null && r.contains(x, y)) return name;
                }
            }
        }
        return null;
    }

    @Override
    public boolean performClick() {
        return super.performClick();
    }

    @Override
    public boolean onTouchEvent(android.view.MotionEvent ev) {
        if (ev.getAction() == android.view.MotionEvent.ACTION_DOWN) {
            performClick();
            int x = Math.round(ev.getX());
            int y = Math.round(ev.getY());
            String hit = hitTestKeyAt(x, y);

            if (hit != null) {
                toggleKey(hit);
                return true;
            }
        }
        return super.onTouchEvent(ev);
    }

    private void toggleKey(String keyName) {
        stopChordPlayback(); // Stop any running chord playback

        // Interruption logic: Stop the last played key sound if it exists
        if (lastPlayedKey != null) {
            if (soundEngine != null) {
                soundEngine.onKeyUp(lastPlayedKey, 0);
            }
        }

        if (selectedKeys.contains(keyName)) {
            selectedKeys.remove(keyName);
            // If we are deselecting the currently playing key, clear the tracker
            if (keyName.equals(lastPlayedKey)) {
                lastPlayedKey = null;
            }
        } else {
            selectedKeys.add(keyName);
            // Play new sound
            if (soundEngine != null) {
                soundEngine.onKeyDown(keyName, 0);
            }
            lastPlayedKey = keyName;
        }

        if (onKeyToggledListener != null) {
            onKeyToggledListener.onKeyToggled(keyName);
        }

        invalidate();
    }

    public void setSelectedKeys(List<String> keys) {
        stopChordPlayback(); // Stop any running chord playback
        this.selectedKeys.clear();
        if (keys != null) {
            this.selectedKeys.addAll(keys);
        }
        invalidate();
    }

    public void backout() {
        if (!selectedKeys.isEmpty()) {
            String removed = selectedKeys.removeLast();
            // Stop sound if it was the last played one
            if (removed.equals(lastPlayedKey)) {
                if (soundEngine != null) {
                    soundEngine.onKeyUp(lastPlayedKey, 0);
                }
                lastPlayedKey = null;
            }
            invalidate();
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if (soundEngine != null) {
            soundEngine.releaseAll();
        }
    }

    private void ensureNoteLabelMap() {
        if (!keyNoteNameMap.isEmpty()) return;
        String[] noteNames = {"C","C#","D","D#","E","F","F#","G","G#","A","A#","B"};
        for (int i = START_KEY_INDEX; i <= END_KEY_INDEX; i++) {
            int midi = i + 20;
            String name = noteNames[midi % 12];
            if (name.contains("#")) continue;
            int octave = midi / 12 - 1;
            keyNoteNameMap.put("key" + i + "_white", name + octave);
        }
    }

    // Public methods for external control if needed
    public void reset() {
        selectedKeys.clear();
        if (lastPlayedKey != null) {
            if (soundEngine != null) {
                soundEngine.onKeyUp(lastPlayedKey, 0);
            }
            lastPlayedKey = null;
        }
        invalidate();
    }

    public List<Integer> getSortedKeyIndices() {
        List<Integer> indices = new ArrayList<>();
        for (String key : selectedKeys) {
            int idx = getKeyIndex(key);
            if (idx != -1) indices.add(idx);
        }
        Collections.sort(indices);
        return indices;
    }

    private int getKeyIndex(String keyName) {
        try {
            int us = keyName.indexOf('_');
            if (keyName.startsWith("key") && us > 3) {
                return Integer.parseInt(keyName.substring(3, us));
            }
        } catch (Exception ignore) {}
        return -1;
    }

    public int getContentWidth() {
        return contentWidthPx;
    }

    public int getKeyCenterX(String keyName) {
        if (keyRegionMap == null || keyRegionMap.isEmpty()) {
            return Math.max(0, contentWidthPx / 2);
        }
        if (keyName != null) {
            Region region = keyRegionMap.get(keyName);
            if (region != null && !region.isEmpty()) {
                Rect bounds = region.getBounds();
                return bounds.left + (bounds.width() / 2);
            }
        }
        return Math.max(0, contentWidthPx / 2);
    }

    public void playChord(List<String> keyNames) {
        if (soundEngine != null) {
            soundEngine.playChord(keyNames);
        }
    }

    public void stopChordPlayback() {
        if (soundEngine != null) {
            soundEngine.stopChordPlayback();
        }
    }
}
