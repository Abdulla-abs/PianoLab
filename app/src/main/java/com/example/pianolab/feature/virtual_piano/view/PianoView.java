
package com.example.pianolab.feature.virtual_piano.view;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.XmlResourceParser;
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
import android.util.SparseArray;
import android.view.View;
import com.example.pianolab.utils.VirtualPianoHelper;

import androidx.core.content.ContextCompat;
import androidx.core.graphics.PathParser;

import com.example.pianolab.R;

import org.xmlpull.v1.XmlPullParser;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class PianoView extends View {
    private static final String TAG = "PianoView";
    private Drawable dStart;
    private Drawable dOctave;
    private Drawable dEnd;
    private int octaveCount = 7; // 7 个八度
    private int contentWidthPx = 0;
    private int contentHeightPx = 0;

    private SparseArray<String> pointerKeyMap = new SparseArray<>(); // pointerId -> key name
    private Map<String, Integer> keyRefCount = new HashMap<>();     // key name -> ref count
    private Set<String> activeKeys = new HashSet<>();

    // 保存测量得到的像素宽度，确保 onDraw 与 onMeasure 一致
    private int swStart = 0;
    private int swOct = 0;
    private int swEnd = 0;

    // fallback intrinsic sizes
    private static final int DEFAULT_OCTAVE_W = 375;
    private static final int DEFAULT_OCTAVE_H = 323;


    private Map<String, Path> keyPrototypeMap = new HashMap<>();
    private Map<String, Path> keyTransformedMap = new HashMap<>();
    private Map<String, Region> keyRegionMap = new HashMap<>();
    private List<String> allKeyNames = new ArrayList<>();
    private Paint fallbackPaintStart;
    private Paint fallbackPaintOctave;
    private Paint fallbackPaintEnd;
    private static final String ANDROID_NS = "http://schemas.android.com/apk/res/android";
    private List<Path> keyPathBlackParts = new ArrayList<>();
    // 原来的单一 keyPathBlack 保留用于绘制合并后的 path
    private Path keyPathBlack;
    private Path keyPathWhite1Transformed, keyPathWhite2Transformed, keyPathBlackTransformed;
    private Region keyRegionWhite1, keyRegionWhite2, keyRegionBlack;
    private static final int REGION_PAD_PX = 2; // 命中区域扩展像素，避免整数化丢失

    // 按键识别用的数据结构
    private Path keyPathWhite1, keyPathWhite2;
    private Paint keyHighlightWhitePaint, keyHighlightBlackPaint;

    private Path keyPathBlackPart2;
    private Path keyPathBlackPart2Transformed;
    private Region keyRegionBlackPart2;

    // piano_start 的 viewport 大小（从 xml 确认）

    private static final float PIANO_START_VIEWPORT_W = 104.34f;
    private static final float PIANO_START_VIEWPORT_H = 323.5f;

    public PianoView(Context context) { this(context, null); }
    public PianoView(Context context, AttributeSet attrs) { this(context, attrs, 0); }
    public PianoView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        try { dStart = ContextCompat.getDrawable(context, R.drawable.piano_start); } catch (Exception e) { Log.e(TAG, "load piano_start failed", e); dStart = null; }
        try { dOctave = ContextCompat.getDrawable(context, R.drawable.piano_keys); } catch (Exception e) { Log.e(TAG, "load piano_keys failed", e); dOctave = null; }
        try { dEnd = ContextCompat.getDrawable(context, R.drawable.piano_end); } catch (Exception e) { Log.e(TAG, "load piano_end failed", e); dEnd = null; }

        fallbackPaintStart = new Paint(Paint.ANTI_ALIAS_FLAG);
        fallbackPaintStart.setColor(Color.parseColor("#efefef"));
        fallbackPaintOctave = new Paint(Paint.ANTI_ALIAS_FLAG);
        fallbackPaintOctave.setColor(Color.parseColor("#e0e0e0"));
        fallbackPaintEnd = new Paint(Paint.ANTI_ALIAS_FLAG);
        fallbackPaintEnd.setColor(Color.parseColor("#dcdcdc"));
    }

    @SuppressLint("ResourceType")
    private void initKeyHitTest() {
        // 高亮画笔
        keyHighlightWhitePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        keyHighlightWhitePaint.setStyle(Paint.Style.FILL);
        keyHighlightWhitePaint.setColor(0x99FF9800); // 半透明橙色
        keyHighlightBlackPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        keyHighlightBlackPaint.setStyle(Paint.Style.FILL);
        keyHighlightBlackPaint.setColor(0x995400FF); // 半透明紫色

        // 清理旧数据（防止多次调用叠加）
        keyPrototypeMap.clear();
        keyTransformedMap.clear();
        keyRegionMap.clear();
        allKeyNames.clear();

        keyPathWhite1 = null;
        keyPathWhite2 = null;
        keyPathBlackParts.clear();
        keyPathBlack = null;
        keyPathBlackPart2 = null;
        keyPathWhite1Transformed = null;
        keyPathWhite2Transformed = null;
        keyPathBlackTransformed = null;
        keyPathBlackPart2Transformed = null;

        Map<String, Path> namedStart = VirtualPianoHelper.loadNamedPathsFromVector(getContext(), R.drawable.piano_start, false);
        Map<String, Path> namedKeys  = VirtualPianoHelper.loadNamedPathsFromVector(getContext(), R.drawable.piano_keys, true);
        Map<String, Path> namedEnd   = VirtualPianoHelper.loadNamedPathsFromVector(getContext(), R.drawable.piano_end, false);
        // 后续逻辑保持不变（把 namedStart/namedKeys/namedEnd 合并并生成 keyPrototypeMap...）
        // 把 piano_start 的绝对命名先放入原型 map（优先）
        for (Map.Entry<String, Path> e : namedStart.entrySet()) {
            Path copy = new Path();
            copy.addPath(e.getValue());
            keyPrototypeMap.put(e.getKey(), copy);
        }

        // piano_keys 模板扩展逻辑（不变）
        for (int oct = 0; oct < 7; oct++) {
            for (int k = 1; k <= 12; k++) {
                String relName = "key" + k;
                String[] suffs = new String[] { "_white", "_black", "_black_part2" };
                for (String s : suffs) {
                    String relFull = relName + s;
                    Path tmpl = namedKeys.get(relFull);
                    if (tmpl == null) continue;
                    int globalIdx = 3 + oct * 12 + k;
                    String absName = "key" + globalIdx + s;
                    if (!keyPrototypeMap.containsKey(absName)) {
                        Path copy = new Path();
                        copy.addPath(tmpl);
                        keyPrototypeMap.put(absName, copy);
                    }
                }
            }
        }

        for (Map.Entry<String, Path> e : namedEnd.entrySet()) {
            Path copy = new Path();
            copy.addPath(e.getValue());
            keyPrototypeMap.put(e.getKey(), copy);
        }

        // 创建 transformed/region 占位（保持原实现）
        for (int i = 1; i <= 88; i++) {
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


    }


    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int availableH = MeasureSpec.getSize(heightMeasureSpec);
        contentHeightPx = availableH;

        int wStart = (dStart != null) ? VirtualPianoHelper.safeIntrinsicWidth(dStart,DEFAULT_OCTAVE_W) : (DEFAULT_OCTAVE_W / 6);
        int hStart = (dStart != null) ? VirtualPianoHelper.safeIntrinsicHeight(dStart,DEFAULT_OCTAVE_H) : DEFAULT_OCTAVE_H;
        int wOct = (dOctave != null) ? VirtualPianoHelper.safeIntrinsicWidth(dOctave,DEFAULT_OCTAVE_W) : DEFAULT_OCTAVE_W;
        int hOct = (dOctave != null) ? VirtualPianoHelper.safeIntrinsicHeight(dOctave,DEFAULT_OCTAVE_H) : DEFAULT_OCTAVE_H;
        int wEnd = (dEnd != null) ? VirtualPianoHelper.safeIntrinsicWidth(dEnd,DEFAULT_OCTAVE_W) : (DEFAULT_OCTAVE_W / 6);
        int hEnd = (dEnd != null) ? VirtualPianoHelper.safeIntrinsicHeight(dEnd,DEFAULT_OCTAVE_H) : DEFAULT_OCTAVE_H;

        float scaleStart = (float) contentHeightPx / (float) Math.max(hStart, 1);
        float scaleOct = (float) contentHeightPx / (float) Math.max(hOct, 1);
        float scaleEnd = (float) contentHeightPx / (float) Math.max(hEnd, 1);
        float scale = (scaleStart + scaleOct + scaleEnd) / 3f;

        swStart = Math.max(1, Math.round(wStart * scale));
        swOct = Math.max(1, Math.round(wOct * scale));
        swEnd = Math.max(1, Math.round(wEnd * scale));

        contentWidthPx = swStart + swOct * octaveCount + swEnd;

        // 预先设定 drawable 的 bounds 高度为 contentHeightPx（保持一致）
        if (dStart != null) dStart.setBounds(0, 0, swStart, contentHeightPx);
        if (dOctave != null) dOctave.setBounds(0, 0, swOct, contentHeightPx);
        if (dEnd != null) dEnd.setBounds(0, 0, swEnd, contentHeightPx);

        setMeasuredDimension(contentWidthPx, contentHeightPx);

        // 先解析 path 再做变换（顺序很重要）
        initKeyHitTest();
        if (dStart != null) {
            updateKeyTransforms(0, swStart, contentHeightPx);
        }
    }



    private void removePressedKey(String name) {
        if (name == null) return;
        Integer c = keyRefCount.get(name);
        if (c == null) return;
        if (c <= 1) {
            keyRefCount.remove(name);
            activeKeys.remove(name);
        } else {
            keyRefCount.put(name, c - 1);
        }
    }

    private void addPressedKey(String name) {
        if (name == null) return;
        int c = keyRefCount.getOrDefault(name, 0);
        keyRefCount.put(name, c + 1);
        activeKeys.add(name);
    }

    private String hitTestKeyAt(int x, int y) {
        // 优先使用为 88 键构建的 region 映射进行精确命中（黑键优先）
        if (allKeyNames != null && keyRegionMap != null && !allKeyNames.isEmpty()) {
            // 先查普通黑键（_black，排除 _black_part2）
            for (String name : allKeyNames) {
                if (name.endsWith("_black") && !name.endsWith("_black_part2")) {
                    Region r = keyRegionMap.get(name);
                    boolean contains = (r != null && r.contains(x, y));
                    if (contains) {
                        Log.d(TAG, "hitTestKeyAt -> hit(black): " + name + " at(" + x + "," + y + ") regionEmpty=" + (r == null ? "null" : r.isEmpty()));
                        return name;
                    }
                }
            }
            // 再查黑键的 part2（若有）
            for (String name : allKeyNames) {
                if (name.endsWith("_black_part2")) {
                    Region r = keyRegionMap.get(name);
                    boolean contains = (r != null && r.contains(x, y));
                    if (contains) {
                        Log.d(TAG, "hitTestKeyAt -> hit(black_part2): " + name + " at(" + x + "," + y + ") regionEmpty=" + (r == null ? "null" : r.isEmpty()));
                        return name;
                    }
                }
            }
            // 最后查白键
            for (String name : allKeyNames) {
                if (name.endsWith("_white")) {
                    Region r = keyRegionMap.get(name);
                    boolean contains = (r != null && r.contains(x, y));
                    if (contains) {
                        Log.d(TAG, "hitTestKeyAt -> hit(white): " + name + " at(" + x + "," + y + ") regionEmpty=" + (r == null ? "null" : r.isEmpty()));
                        return name;
                    }
                }
            }

            // 没命中时，打印 summary（避免过于频繁，使用 info 级别）
            int nonEmptyBlack = 0, nonEmptyWhite = 0;
            for (String name : allKeyNames) {
                Region r = keyRegionMap.get(name);
                if (r == null || r.isEmpty()) continue;
                if (name.endsWith("_white")) nonEmptyWhite++;
                else if (name.endsWith("_black") || name.endsWith("_black_part2")) nonEmptyBlack++;
            }
            Log.i(TAG, "hitTestKeyAt no hit at(" + x + "," + y + "), activeKeys=" + activeKeys.size()
                    + " nonEmptyWhiteRegions=" + nonEmptyWhite + " nonEmptyBlackRegions=" + nonEmptyBlack);
        } else {
            Log.i(TAG, "hitTestKeyAt: allKeyNames/keyRegionMap not ready. allKeyNames=" + (allKeyNames == null ? "null" : allKeyNames.size())
                    + " keyRegionMap=" + (keyRegionMap == null ? "null" : keyRegionMap.size()));
        }

        // 回退兼容：如果上面没有填充 map（或未命中），保留原有单一字段检测以避免回归
        if (keyRegionBlack != null && keyRegionBlack.contains(x, y)) {
            Log.d(TAG, "hitTestKeyAt -> fallback hit key2_black at(" + x + "," + y + ") regionEmpty=" + keyRegionBlack.isEmpty());
            return "key2_black";
        }
        if (keyRegionBlackPart2 != null && keyRegionBlackPart2.contains(x, y)) {
            Log.d(TAG, "hitTestKeyAt -> fallback hit key2_black_part2 at(" + x + "," + y + ") regionEmpty=" + keyRegionBlackPart2.isEmpty());
            return "key2_black_part2";
        }
        if (keyRegionWhite1 != null && keyRegionWhite1.contains(x, y)) {
            Log.d(TAG, "hitTestKeyAt -> fallback hit key1_white at(" + x + "," + y + ") regionEmpty=" + keyRegionWhite1.isEmpty());
            return "key1_white";
        }
        if (keyRegionWhite2 != null && keyRegionWhite2.contains(x, y)) {
            Log.d(TAG, "hitTestKeyAt -> fallback hit key3_white at(" + x + "," + y + ") regionEmpty=" + keyRegionWhite2.isEmpty());
            return "key3_white";
        }
        return null;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        int x = 0;
        int h = getHeight();

        // start
        if (swStart <= 0) swStart = Math.max(1, contentWidthPx / (octaveCount * 7 + 3));
        if (dStart != null) {
            dStart.setBounds(x, 0, x + swStart, h);
            try { dStart.draw(canvas); } catch (Exception e) { Log.e(TAG, "draw dStart failed", e); }
        } else {
            canvas.drawRect(x, 0, x + swStart, h, fallbackPaintStart);
        }
        x += swStart;

        // octaves
        if (swOct <= 0) swOct = Math.max(1, (contentWidthPx - swStart - swEnd) / Math.max(1, octaveCount));
        for (int i = 0; i < octaveCount; i++) {
            if (dOctave != null) {
                dOctave.setBounds(x, 0, x + swOct, h);
                try { dOctave.draw(canvas); } catch (Exception e) { Log.e(TAG, "draw dOctave failed idx=" + i, e); }
            } else {
                canvas.drawRect(x, 0, x + swOct, h, fallbackPaintOctave);
            }
            x += swOct;
        }

        // end
        if (swEnd <= 0) swEnd = Math.max(1, contentWidthPx / (octaveCount * 7 + 3));
        if (dEnd != null) {
            dEnd.setBounds(x, 0, x + swEnd, h);
            try { dEnd.draw(canvas); } catch (Exception e) { Log.e(TAG, "draw dEnd failed", e); }
        } else {
            canvas.drawRect(x, 0, x + swEnd, h, fallbackPaintEnd);
        }

        if (!activeKeys.isEmpty()) {
            try {
                // debug snapshot
                StringBuilder sb = new StringBuilder();
                sb.append("onDraw highlight activeKeys=").append(activeKeys.size()).append(" list=").append(activeKeys);
                int foundTrans = 0, foundRegionNonEmpty = 0;
                int sampleLog = 0;
                for (String name : activeKeys) {
                    if (name == null) continue;
                    Path p = keyTransformedMap.get(name);
                    Region r = keyRegionMap.get(name);
                    boolean hasP = (p != null);
                    boolean hasR = (r != null && !r.isEmpty());
                    if (hasP) foundTrans++;
                    if (hasR) foundRegionNonEmpty++;
                    if (sampleLog < 20) {
                        sb.append("\n  ").append(name)
                                .append(" transformed=").append(hasP)
                                .append(" regionNonEmpty=").append(hasR);
                        if (hasP) {
                            try {
                                android.graphics.RectF rf = new android.graphics.RectF();
                                p.computeBounds(rf, true);
                                sb.append(" bounds=").append((int)rf.left).append(",").append((int)rf.top)
                                        .append(",").append((int)rf.right).append(",").append((int)rf.bottom);
                            } catch (Exception ignored) {}
                        }
                        sampleLog++;
                    }
                }
                sb.append(" transformedFound=").append(foundTrans).append(" regionFound=").append(foundRegionNonEmpty);
                Log.d(TAG, sb.toString());

                // 合并所有活跃白键路径（用于绘制白键高亮）
                Path whiteCombined = new Path();
                boolean hasWhite = false;

                // 合并活跃黑键用于在上层绘制（不用于差集）
                Path blackCombinedActive = new Path();
                boolean hasActiveBlack = false;

                for (String name : activeKeys) {
                    if (name == null) continue;
                    if (name.endsWith("_white")) {
                        Path p = keyTransformedMap.get(name);
                        if (p != null) {
                            whiteCombined.addPath(p);
                            hasWhite = true;
                        } else {
                            // 兼容旧字段回退
                            if ("key1_white".equals(name) && keyPathWhite1Transformed != null) {
                                whiteCombined.addPath(keyPathWhite1Transformed);
                                hasWhite = true;
                            } else if ("key3_white".equals(name) && keyPathWhite2Transformed != null) {
                                whiteCombined.addPath(keyPathWhite2Transformed);
                                hasWhite = true;
                            }
                        }
                    } else if (name.endsWith("_black") || name.endsWith("_black_part2")) {
                        Path p = keyTransformedMap.get(name);
                        if (p != null) {
                            blackCombinedActive.addPath(p);
                            hasActiveBlack = true;
                        } else {
                            if (!name.endsWith("_black_part2") && keyPathBlackTransformed != null) {
                                blackCombinedActive.addPath(keyPathBlackTransformed);
                                hasActiveBlack = true;
                            } else if (name.endsWith("_black_part2") && keyPathBlackPart2Transformed != null) {
                                blackCombinedActive.addPath(keyPathBlackPart2Transformed);
                                hasActiveBlack = true;
                            }
                        }
                    }
                }

                // 计算全局聚合黑键是否存在（用于挖去白键上的黑键形状）
                boolean hasAggregateBlack = false;
                if (keyPathBlackTransformed != null) {
                    try {
                        android.graphics.RectF rf = new android.graphics.RectF();
                        keyPathBlackTransformed.computeBounds(rf, true);
                        hasAggregateBlack = (rf.width() > 0 && rf.height() > 0);
                    } catch (Exception ignored) {
                        hasAggregateBlack = true; // 保守地认为存在
                    }
                }

                Log.d(TAG, "onDraw highlight computed hasWhite=" + hasWhite + " hasActiveBlack=" + hasActiveBlack + " hasAggregateBlack=" + hasAggregateBlack);

                // 白键高亮：优先用 (whiteCombined - keyPathBlackTransformed)（如果聚合黑键存在）
                if (hasWhite) {
                    if (hasAggregateBlack) {
                        Path drawPath = new Path(whiteCombined);
                        try {
                            drawPath.op(keyPathBlackTransformed, Path.Op.DIFFERENCE);
                            canvas.drawPath(drawPath, keyHighlightWhitePaint);
                        } catch (Exception e) {
                            Log.w(TAG, "aggregate white - aggregateBlack op failed, fallback to whiteCombined", e);
                            try {
                                canvas.drawPath(whiteCombined, keyHighlightWhitePaint);
                            } catch (Exception e2) {
                                Log.w(TAG, "draw fallback whiteCombined failed", e2);
                            }
                        }
                    } else {
                        try {
                            canvas.drawPath(whiteCombined, keyHighlightWhitePaint);
                        } catch (Exception e) {
                            Log.w(TAG, "draw whiteCombined failed", e);
                        }
                    }
                }

                // 绘制按下的黑键高亮（始终在上层）
                if (hasActiveBlack) {
                    try {
                        canvas.drawPath(blackCombinedActive, keyHighlightBlackPaint);
                    } catch (Exception e) {
                        Log.w(TAG, "draw blackCombinedActive failed", e);
                    }
                }

            } catch (Exception e) {
                Log.w(TAG, "highlight draw failed", e);
            }
        }
    }

    // java
    private void updateKeyTransforms(int offsetXForStartDrawable, int destWidth, int destHeight) {
        if (keyPrototypeMap.isEmpty()) return;

        final float startVW = PIANO_START_VIEWPORT_W;
        final float startVH = PIANO_START_VIEWPORT_H;
        final float octaveVW = DEFAULT_OCTAVE_W;
        final float octaveVH = DEFAULT_OCTAVE_H;
        final float endVW = PIANO_START_VIEWPORT_W;
        final float endVH = PIANO_START_VIEWPORT_H;

        Region fullClip = new Region(0, 0, Math.max(1, contentWidthPx), Math.max(1, contentHeightPx));

        if (keyPathBlackTransformed == null) keyPathBlackTransformed = new Path();
        keyPathBlackTransformed.reset();
        if (keyRegionBlack == null) keyRegionBlack = new Region();
        keyRegionBlack.setEmpty();

        keyPathWhite1Transformed = (keyPathWhite1 != null) ? new Path() : null;
        keyPathWhite2Transformed = (keyPathWhite2 != null) ? new Path() : null;
        keyPathBlackPart2Transformed = (keyPathBlackPart2 != null) ? new Path() : null;
        if (keyRegionWhite1 == null) keyRegionWhite1 = new Region();
        else keyRegionWhite1.setEmpty();
        if (keyRegionWhite2 == null) keyRegionWhite2 = new Region();
        else keyRegionWhite2.setEmpty();
        if (keyRegionBlackPart2 == null) keyRegionBlackPart2 = new Region();
        else keyRegionBlackPart2.setEmpty();

        for (Map.Entry<String, Path> entry : keyPrototypeMap.entrySet()) {
            String name = entry.getKey();
            Path proto = entry.getValue();
            if (proto == null) continue;

            int idx = -1;
            try {
                int us = name.indexOf('_');
                if (name.startsWith("key") && us > 3) {
                    idx = Integer.parseInt(name.substring(3, us));
                } else if (name.startsWith("key") && us > 0) {
                    idx = Integer.parseInt(name.substring(3, us));
                }
            } catch (Exception ignore) {
                idx = -1;
            }

            int section = 1;
            int absOffsetX = 0;
            int destW = swOct;
            float vw = octaveVW, vh = octaveVH;
            if (idx > 0 && idx <= 3) {
                section = 1;
                absOffsetX = offsetXForStartDrawable;
                destW = swStart;
                vw = startVW; vh = startVH;
            } else if (idx == 88) {
                section = 3;
                absOffsetX = swStart + swOct * octaveCount;
                destW = swEnd;
                vw = endVW; vh = endVH;
            } else if (idx > 3 && idx <= 87) {
                section = 2;
                int octaveIndex = (idx - 4) / 12;
                absOffsetX = swStart + octaveIndex * swOct;
                destW = swOct;
                vw = octaveVW; vh = octaveVH;
            } else {
                section = 1;
                absOffsetX = offsetXForStartDrawable;
                destW = swStart;
                vw = startVW; vh = startVH;
            }

            // **局部修复**：仅针对最后一个白键 key88_white 基于 proto bounds 计算 matrix，其他键不变
            Matrix m = new Matrix();
            if (idx == 88 && name.endsWith("_white")) {
                android.graphics.RectF protoRf = new android.graphics.RectF();
                boolean haveProtoBounds = false;
                try {
                    proto.computeBounds(protoRf, true);
                    if (protoRf.width() > 0.5f && protoRf.height() > 0.5f) haveProtoBounds = true;
                } catch (Exception ignored) { haveProtoBounds = false; }

                if (haveProtoBounds) {
                    float sx = (float) destW / Math.max(1f, protoRf.width());
                    float sy = (float) contentHeightPx / Math.max(1f, protoRf.height());
                    m.setScale(sx, sy);
                    // 将 proto 的左上角映射到目标位置，保证完整覆盖 destW 区域
                    m.postTranslate(absOffsetX - protoRf.left * sx, -protoRf.top * sy);
                } else {
                    float sx = (float) destW / Math.max(1f, vw);
                    float sy = (float) contentHeightPx / Math.max(1f, vh);
                    m.setScale(sx, sy);
                    m.postTranslate(absOffsetX, 0);
                }
            } else {
                float sx = (float) destW / Math.max(1f, vw);
                float sy = (float) contentHeightPx / Math.max(1f, vh);
                m.setScale(sx, sy);
                m.postTranslate(absOffsetX, 0);
            }

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
            } catch (Exception e) {
                ok = false;
            }
            if (!ok) {
                android.graphics.RectF rf = new android.graphics.RectF();
                try {
                    transformed.computeBounds(rf, true);
                } catch (Exception e) {
                    rf.set(0,0,0,0);
                }
                int[] dirs = VirtualPianoHelper.calculate_4_direction(rf.left, rf.top, rf.right, rf.bottom, REGION_PAD_PX);
                int left = Math.max(0, dirs[0]);
                int top = Math.max(0, dirs[1]);
                int right = Math.min(Math.max(1, contentWidthPx), dirs[2]);
                int bottom = Math.min(Math.max(1, contentHeightPx), dirs[3]);
                Region boundsClip = new Region(left, top, right, bottom);
                try {
                    region.setPath(transformed, boundsClip);
                    if (region.isEmpty()) {
                        int pad = Math.max(1, REGION_PAD_PX * 4);
                        boundsClip.set(Math.max(0, left - pad), Math.max(0, top - pad),
                                Math.min(contentWidthPx, right + pad), Math.min(contentHeightPx, bottom + pad));
                        region.setPath(transformed, boundsClip);
                    }
                } catch (Exception e2) {
                    region.set(boundsClip);
                }
            }

            if ("key1_white".equals(name)) {
                keyPathWhite1Transformed = transformed;
                keyRegionWhite1 = region;
            } else if ("key3_white".equals(name)) {
                keyPathWhite2Transformed = transformed;
                keyRegionWhite2 = region;
            }

            if (name.endsWith("_black") && !name.endsWith("_black_part2")) {
                keyPathBlackTransformed.addPath(transformed);
                keyRegionBlack.op(region, Region.Op.UNION);
            }

            if ("key2_black_part2".equals(name)) {
                keyPathBlackPart2Transformed = transformed;
                keyRegionBlackPart2 = region;
            }
        }

        for (String name : keyTransformedMap.keySet()) {
            if (keyTransformedMap.get(name) == null) keyTransformedMap.put(name, new Path());
            if (!keyRegionMap.containsKey(name)) keyRegionMap.put(name, new Region());
        }

        // debug 输出（保持原实现）
        int protoCount = keyPrototypeMap.size();
        int transformedCount = 0;
        int regionFilledCount = 0;
        int protoWhite = 0, protoBlack = 0;
        int transWhite = 0, transBlack = 0;
        List<String> missingTrans = new ArrayList<>();
        for (String name : keyPrototypeMap.keySet()) {
            if (name.endsWith("_white")) protoWhite++;
            if (name.endsWith("_black") || name.endsWith("_black_part2")) protoBlack++;
            if (keyTransformedMap.containsKey(name)) {
                transformedCount++;
                Path p = keyTransformedMap.get(name);
                if (p != null) {
                    if (name.endsWith("_white")) transWhite++;
                    if (name.endsWith("_black") || name.endsWith("_black_part2")) transBlack++;
                }
            } else {
                missingTrans.add(name);
            }
            Region r = keyRegionMap.get(name);
            if (r != null && !r.isEmpty()) regionFilledCount++;
        }

        String sampleMissing = missingTrans.isEmpty() ? "[]" : missingTrans.subList(0, Math.min(30, missingTrans.size())).toString();
        StringBuilder stats = new StringBuilder();
        stats.append("updateKeyTransforms: proto=").append(protoCount)
                .append(" (white=").append(protoWhite).append(" black=").append(protoBlack).append(")")
                .append(" transformedCount=").append(transformedCount)
                .append(" (white=").append(transWhite).append(" black=").append(transBlack).append(")")
                .append(" regionNonEmpty=").append(regionFilledCount)
                .append(" keyTransformedMap.size=").append(keyTransformedMap.size())
                .append(" keyRegionMap.size=").append(keyRegionMap.size())
                .append(" sampleMissingTrans=").append(sampleMissing);
        Log.i(TAG, stats.toString());

        try {
            if (keyPathBlackTransformed != null) {
                android.graphics.RectF rf = new android.graphics.RectF();
                keyPathBlackTransformed.computeBounds(rf, true);
                Log.d(TAG, "keyPathBlackTransformed bounds: " + (int)rf.left + "," + (int)rf.top + "," + (int)rf.right + "," + (int)rf.bottom);
            }
        } catch (Exception ignored) {}
    }

    @Override
    public boolean onTouchEvent(android.view.MotionEvent ev) {
        int action = ev.getActionMasked();
        int index = ev.getActionIndex();

        switch (action) {
            case android.view.MotionEvent.ACTION_DOWN:
            case android.view.MotionEvent.ACTION_POINTER_DOWN: {
                int pid = ev.getPointerId(index);
                int px = Math.round(ev.getX(index));
                int py = Math.round(ev.getY(index));
                String hit = hitTestKeyAt(px, py);
                if (hit != null) {
                    pointerKeyMap.put(pid, hit);
                    addPressedKey(hit);
                    Log.i(TAG, "key pressed (down): " + hit + " pid=" + pid);
                    invalidate();
                    return true; // 捕获事件以便接收后续 MOVE/UP
                }
                break;
            }
            case android.view.MotionEvent.ACTION_MOVE: {
                boolean changed = false;
                for (int i = 0; i < ev.getPointerCount(); i++) {
                    int pid = ev.getPointerId(i);
                    int px = Math.round(ev.getX(i));
                    int py = Math.round(ev.getY(i));
                    String prev = pointerKeyMap.get(pid);
                    String now = hitTestKeyAt(px, py);
                    if ((prev == null && now == null) || (prev != null && prev.equals(now))) {
                        continue;
                    }
                    // 发生变化：释放之前的，登记现在的
                    if (prev != null) {
                        removePressedKey(prev);
                        pointerKeyMap.remove(pid);
                    }
                    if (now != null) {
                        pointerKeyMap.put(pid, now);
                        addPressedKey(now);
                    }
                    changed = true;
                }
                if (changed) {
                    invalidate();
                    return true;
                }
                break;
            }
            case android.view.MotionEvent.ACTION_UP:
            case android.view.MotionEvent.ACTION_POINTER_UP: {
                int pid = ev.getPointerId(index);
                String prev = pointerKeyMap.get(pid);
                if (prev != null) {
                    removePressedKey(prev);
                    pointerKeyMap.remove(pid);
                    Log.i(TAG, "key released (up): " + prev + " pid=" + pid);
                    invalidate();
                    return true;
                }
                break;
            }
            case android.view.MotionEvent.ACTION_CANCEL: {
                // 清理所有按键
                pointerKeyMap.clear();
                keyRefCount.clear();
                activeKeys.clear();
                invalidate();
                return true;
            }
        }
        return super.onTouchEvent(ev);
    }


    public int getContentWidth() {
        return contentWidthPx;
    }



    //TEST
    public void debugValidateInitKeyHitTest() {
        // piano pattern: in 12-key template, 白键位置 (1-based): 1,3,5,6,8,10,12 ; 黑键位置: 2,4,7,9,11
        Set<Integer> whitePositions = new HashSet<>();
        int[] whites = new int[] {1,3,5,6,8,10,12};
        for (int w : whites) whitePositions.add(w);
        Set<Integer> blackPositions = new HashSet<>();
        int[] blacks = new int[] {2,4,7,9,11};
        for (int b : blacks) blackPositions.add(b);

        List<String> expected = new ArrayList<>();
        // piano_start 的绝对三个键（保留）
        expected.add("key1_white");
        expected.add("key2_black");
        // key2_black_part2 为可选，仅当解析到时再验证；保留在报告中单独说明
        expected.add("key3_white");

        // piano_keys 模板 -> 7 octaves * 12 keys, global index 4..87 (3 + oct*12 + k)
        for (int oct = 0; oct < 7; oct++) {
            for (int k = 1; k <= 12; k++) {
                int global = 3 + oct * 12 + k; // 4..87
                if (whitePositions.contains(k)) {
                    expected.add("key" + global + "_white");
                }
                if (blackPositions.contains(k)) {
                    expected.add("key" + global + "_black");
                    // 不把 _black_part2 当作强制期望（视资源而定）
                }
            }
        }
        // piano_end 的绝对键
        expected.add("key88_white");

        // 检查 prototype 存在性
        List<String> missingProtos = new ArrayList<>();
        for (String name : expected) {
            if (!keyPrototypeMap.containsKey(name)) {
                missingProtos.add(name);
            }
        }

        // 检查 transformed/region 占位（init 后是否创建）
        List<String> missingTransformed = new ArrayList<>();
        List<String> missingRegions = new ArrayList<>();
        for (Map.Entry<String, Path> e : keyPrototypeMap.entrySet()) {
            String name = e.getKey();
            if (!keyTransformedMap.containsKey(name)) missingTransformed.add(name);
            if (!keyRegionMap.containsKey(name)) missingRegions.add(name);
        }

        Log.i(TAG, "debugValidateInitKeyHitTest: prototypes=" + keyPrototypeMap.size()
                + " transformed=" + keyTransformedMap.size()
                + " regions=" + keyRegionMap.size()
                + " expectedTotal=" + expected.size());

        if (!missingProtos.isEmpty()) {
            Log.w(TAG, "Missing prototypes (" + missingProtos.size() + "): " + missingProtos.subList(0, Math.min(80, missingProtos.size())));
        } else {
            Log.i(TAG, "All expected prototypes present (based on piano layout).");
        }

        // 特别检查是否解析到任何 _black_part2（因为我们把它当可选）
        List<String> foundBlackPart2 = new ArrayList<>();
        for (String name : keyPrototypeMap.keySet()) {
            if (name.endsWith("_black_part2")) foundBlackPart2.add(name);
        }
        Log.i(TAG, "Found _black_part2 prototypes count=" + foundBlackPart2.size() + " sample=" + (foundBlackPart2.isEmpty() ? "[]" : foundBlackPart2.subList(0, Math.min(20, foundBlackPart2.size()))));

        if (!missingTransformed.isEmpty()) {
            Log.w(TAG, "Missing transformed placeholders (" + missingTransformed.size() + "), sample: " + missingTransformed.subList(0, Math.min(20, missingTransformed.size())));
        }
        if (!missingRegions.isEmpty()) {
            Log.w(TAG, "Missing region placeholders (" + missingRegions.size() + "), sample: " + missingRegions.subList(0, Math.min(20, missingRegions.size())));
        }

        // 输出解析到的 prototype 名称集合，便于进一步比对资源命名
        Log.i(TAG, "Parsed prototype names (sample, up to 100): " + new ArrayList<>(keyPrototypeMap.keySet()).subList(0, Math.min(100, keyPrototypeMap.size())));
    }
}