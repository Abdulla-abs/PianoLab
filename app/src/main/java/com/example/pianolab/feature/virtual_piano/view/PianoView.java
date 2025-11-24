
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
import android.view.View;

import androidx.core.content.ContextCompat;
import androidx.core.graphics.PathParser;

import com.example.pianolab.R;

import org.xmlpull.v1.XmlPullParser;

import java.util.ArrayList;
import java.util.List;

public class PianoView extends View {
    private static final String TAG = "PianoView";
    private Drawable dStart;
    private Drawable dOctave;
    private Drawable dEnd;
    private int octaveCount = 7; // 7 个八度
    private int contentWidthPx = 0;
    private int contentHeightPx = 0;
    private String pressedKeyName = null;

    // 保存测量得到的像素宽度，确保 onDraw 与 onMeasure 一致
    private int swStart = 0;
    private int swOct = 0;
    private int swEnd = 0;

    // fallback intrinsic sizes
    private static final int DEFAULT_OCTAVE_W = 375;
    private static final int DEFAULT_OCTAVE_H = 323;

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
    private int pressedKey = 0; // 0=none, 1=white1,2=white2,3=black
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
        keyPathWhite1 = null;
        keyPathWhite2 = null;
        keyPathBlackParts.clear();
        keyPathBlack = null;
        keyPathBlackPart2 = null;
        keyPathWhite1Transformed = null;
        keyPathWhite2Transformed = null;
        keyPathBlackTransformed = null;
        keyPathBlackPart2Transformed = null;

        // 解析 vector xml 中指定名字的 pathData（你在 piano_start.xml 已添加 android:name）
        try {
            XmlResourceParser parser = getResources().getXml(R.drawable.piano_start);
            int eventType = parser.getEventType();
            while (eventType != XmlPullParser.END_DOCUMENT) {
                if (eventType == XmlPullParser.START_TAG && "path".equals(parser.getName())) {
                    String name = parser.getAttributeValue(ANDROID_NS, "name"); // android:name
                    String pathData = parser.getAttributeValue(ANDROID_NS, "pathData"); // android:pathData
                    if (pathData != null && name != null) {
                        if ("key1_white".equals(name)) {
                            keyPathWhite1 = PathParser.createPathFromPathData(pathData);
                        } else if ("key3_white".equals(name)) {
                            keyPathWhite2 = PathParser.createPathFromPathData(pathData);
                        } else if ("key2_black".equals(name)) {
                            Path p = PathParser.createPathFromPathData(pathData);
                            keyPathBlackParts.add(p);
                        } else if ("key2_black_part2".equals(name)) {
                            // 测试用单独路径
                            keyPathBlackPart2 = PathParser.createPathFromPathData(pathData);
                        }
                    }
                }
                eventType = parser.next();
            }
            parser.close();
        } catch (Exception e) {
            Log.e(TAG, "parse piano_start paths failed", e);
        }

        // 初始化 transformed paths / regions（即使某个 key 是 null 也创建 Region，避免后续空指针）
        keyPathWhite1Transformed = (keyPathWhite1 != null) ? new Path() : null;
        keyPathWhite2Transformed = (keyPathWhite2 != null) ? new Path() : null;
        keyPathBlackTransformed = (!keyPathBlackParts.isEmpty()) ? new Path() : null;
        keyPathBlackPart2Transformed = (keyPathBlackPart2 != null) ? new Path() : null;
        keyRegionWhite1 = new Region();
        keyRegionWhite2 = new Region();
        keyRegionBlack = new Region();
        keyRegionBlackPart2 = new Region();
    }


    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int availableH = MeasureSpec.getSize(heightMeasureSpec);
        contentHeightPx = availableH;

        int wStart = (dStart != null) ? safeIntrinsicWidth(dStart) : (DEFAULT_OCTAVE_W / 6);
        int hStart = (dStart != null) ? safeIntrinsicHeight(dStart) : DEFAULT_OCTAVE_H;
        int wOct = (dOctave != null) ? safeIntrinsicWidth(dOctave) : DEFAULT_OCTAVE_W;
        int hOct = (dOctave != null) ? safeIntrinsicHeight(dOctave) : DEFAULT_OCTAVE_H;
        int wEnd = (dEnd != null) ? safeIntrinsicWidth(dEnd) : (DEFAULT_OCTAVE_W / 6);
        int hEnd = (dEnd != null) ? safeIntrinsicHeight(dEnd) : DEFAULT_OCTAVE_H;

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

    private int safeIntrinsicWidth(Drawable d) {
        try {
            int w = d.getIntrinsicWidth();
            return (w > 0) ? w : DEFAULT_OCTAVE_W;
        } catch (Exception e) {
            Log.w(TAG, "safeIntrinsicWidth failed", e);
            return DEFAULT_OCTAVE_W;
        }
    }

    private int safeIntrinsicHeight(Drawable d) {
        try {
            int h = d.getIntrinsicHeight();
            return (h > 0) ? h : DEFAULT_OCTAVE_H;
        } catch (Exception e) {
            Log.w(TAG, "safeIntrinsicHeight failed", e);
            return DEFAULT_OCTAVE_H;
        }
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

        // 绘制按下高亮：白键先绘制抠掉黑键重叠部分；黑键在上方直接绘制
        if (pressedKey != 0) {
            try {
                if (pressedKey == 1 && keyPathWhite1Transformed != null) {
                    // 如果有黑键区域，从白键 path 中抠掉
                    Path drawPath = new Path(keyPathWhite1Transformed);
                    if (keyPathBlackTransformed != null) {
                        drawPath.op(keyPathBlackTransformed, Path.Op.DIFFERENCE);
                    }
                    canvas.drawPath(drawPath, keyHighlightWhitePaint);
                } else if (pressedKey == 2 && keyPathWhite2Transformed != null) {
                    Path drawPath = new Path(keyPathWhite2Transformed);
                    if (keyPathBlackTransformed != null) {
                        drawPath.op(keyPathBlackTransformed, Path.Op.DIFFERENCE);
                    }
                    canvas.drawPath(drawPath, keyHighlightWhitePaint);
                } else if (pressedKey == 3 && keyPathBlackTransformed != null) {
                    // 黑键按下高亮在最上面绘制
                    canvas.drawPath(keyPathBlackTransformed, keyHighlightBlackPaint);
                }
            } catch (Exception e) {
                Log.w(TAG, "highlight draw failed", e);
            }
        }

    }

    // java
    private void updateKeyTransforms(int offsetXForStartDrawable, int destWidth, int destHeight) {
        // destWidth = swStart, destHeight = contentHeightPx (drawable bounds for piano_start)
        if (keyPathWhite1 == null && keyPathWhite2 == null && keyPathBlackParts.isEmpty() && keyPathBlackPart2 == null) return;

        float sx = (float) destWidth / PIANO_START_VIEWPORT_W;
        float sy = (float) destHeight / PIANO_START_VIEWPORT_H;
        Matrix m = new Matrix();
        m.setScale(sx, sy);
        m.postTranslate(offsetXForStartDrawable, 0); // 假设 start drawable 放置在 x = offsetXForStartDrawable

        // transform white1
        if (keyPathWhite1 != null && keyPathWhite1Transformed != null) {
            keyPathWhite1Transformed.reset();
            keyPathWhite1Transformed.addPath(keyPathWhite1, m);
            android.graphics.RectF rf = new android.graphics.RectF();
            keyPathWhite1Transformed.computeBounds(rf, true);
            int left = (int) Math.floor(rf.left) - REGION_PAD_PX;
            int top = (int) Math.floor(rf.top) - REGION_PAD_PX;
            int right = (int) Math.ceil(rf.right) + REGION_PAD_PX;
            int bottom = (int) Math.ceil(rf.bottom) + REGION_PAD_PX;
            keyRegionWhite1.setPath(keyPathWhite1Transformed, new Region(left, top, right, bottom));
        }

        // transform white2
        if (keyPathWhite2 != null && keyPathWhite2Transformed != null) {
            keyPathWhite2Transformed.reset();
            keyPathWhite2Transformed.addPath(keyPathWhite2, m);
            android.graphics.RectF rf = new android.graphics.RectF();
            keyPathWhite2Transformed.computeBounds(rf, true);
            int left = (int) Math.floor(rf.left) - REGION_PAD_PX;
            int top = (int) Math.floor(rf.top) - REGION_PAD_PX;
            int right = (int) Math.ceil(rf.right) + REGION_PAD_PX;
            int bottom = (int) Math.ceil(rf.bottom) + REGION_PAD_PX;
            keyRegionWhite2.setPath(keyPathWhite2Transformed, new Region(left, top, right, bottom));
        }

        // transform black parts -> build combined transformed path + combined Region (use full-clip then validate/fallback)
        if (!keyPathBlackParts.isEmpty() && keyPathBlackTransformed != null) {
            keyPathBlackTransformed.reset();
            keyRegionBlack.setEmpty();

            Region fullClip = new Region(0, 0, Math.max(1, contentWidthPx), Math.max(1, contentHeightPx));

            for (Path part : keyPathBlackParts) {
                Path tmp = new Path();
                tmp.addPath(part, m);
                // add tmp to combined path for drawing
                keyPathBlackTransformed.addPath(tmp);

                Region partRegion = new Region();
                boolean ok = false;

                // 1) 尝试用 fullClip 直接构造
                try {
                    partRegion.setPath(tmp, fullClip);
                    if (!partRegion.isEmpty()) {
                        ok = true;
                    }
                } catch (Exception e) {
                    Log.w(TAG, "setPath with fullClip threw, will fallback to bounds", e);
                    ok = false;
                }

                // 2) 若用 fullClip 得到的 region 为空（或失败），回退到 bounds-based region（带 padding）
                if (!ok) {
                    android.graphics.RectF rf = new android.graphics.RectF();
                    tmp.computeBounds(rf, true);
                    int left = Math.max(0, (int) Math.floor(rf.left) - REGION_PAD_PX);
                    int top = Math.max(0, (int) Math.floor(rf.top) - REGION_PAD_PX);
                    int right = Math.min(Math.max(1, contentWidthPx), (int) Math.ceil(rf.right) + REGION_PAD_PX);
                    int bottom = Math.min(Math.max(1, contentHeightPx), (int) Math.ceil(rf.bottom) + REGION_PAD_PX);
                    Region boundsClip = new Region(left, top, right, bottom);
                    try {
                        partRegion.setPath(tmp, boundsClip);
                        // 如果仍为空，尝试扩大 bounds 再试一次
                        if (partRegion.isEmpty()) {
                            int pad = Math.max(1, REGION_PAD_PX * 4);
                            boundsClip.set(Math.max(0, left - pad), Math.max(0, top - pad),
                                    Math.min(contentWidthPx, right + pad), Math.min(contentHeightPx, bottom + pad));
                            partRegion.setPath(tmp, boundsClip);
                        }
                    } catch (Exception e2) {
                        Log.w(TAG, "fallback setPath failed for black part, using bounds as region", e2);
                        // 最后保底：把部分 bounds 直接作为 Region（避免遗漏）
                        partRegion.set(boundsClip);
                    }
                }

                // union into black region
                keyRegionBlack.op(partRegion, Region.Op.UNION);
            }
        }

        // transform test black path（key2_black_test）
        if (keyPathBlackPart2 != null && keyPathBlackPart2Transformed != null) {
            keyPathBlackPart2Transformed.reset();
            keyPathBlackPart2Transformed.addPath(keyPathBlackPart2, m);

            Region fullClip = new Region(0, 0, Math.max(1, contentWidthPx), Math.max(1, contentHeightPx));
            try {
                keyRegionBlackPart2.setPath(keyPathBlackPart2Transformed, fullClip);
                // 若为空则执行同样的回退逻辑
                if (keyRegionBlackPart2.isEmpty()) {
                    android.graphics.RectF rf = new android.graphics.RectF();
                    keyPathBlackPart2Transformed.computeBounds(rf, true);
                    int left = Math.max(0, (int) Math.floor(rf.left) - REGION_PAD_PX);
                    int top = Math.max(0, (int) Math.floor(rf.top) - REGION_PAD_PX);
                    int right = Math.min(Math.max(1, contentWidthPx), (int) Math.ceil(rf.right) + REGION_PAD_PX);
                    int bottom = Math.min(Math.max(1, contentHeightPx), (int) Math.ceil(rf.bottom) + REGION_PAD_PX);
                    Region fallback = new Region(left, top, right, bottom);
                    try {
                        keyRegionBlackPart2.setPath(keyPathBlackPart2Transformed, fallback);
                        if (keyRegionBlackPart2.isEmpty()) {
                            int pad = Math.max(1, REGION_PAD_PX * 4);
                            fallback.set(Math.max(0, left - pad), Math.max(0, top - pad),
                                    Math.min(contentWidthPx, right + pad), Math.min(contentHeightPx, bottom + pad));
                            keyRegionBlackPart2.setPath(keyPathBlackPart2Transformed, fallback);
                        }
                    } catch (Exception e2) {
                        Log.w(TAG, "fallback setPath failed for key2_black_test, using bounds as region", e2);
                        keyRegionBlackPart2.set(fallback);
                    }
                }
            } catch (Exception e) {
                Log.w(TAG, "setPath with fullClip failed for key2_black_test, fallback to bound-based region", e);
                android.graphics.RectF rf = new android.graphics.RectF();
                keyPathBlackPart2Transformed.computeBounds(rf, true);
                int left = Math.max(0, (int) Math.floor(rf.left) - REGION_PAD_PX);
                int top = Math.max(0, (int) Math.floor(rf.top) - REGION_PAD_PX);
                int right = Math.min(Math.max(1, contentWidthPx), (int) Math.ceil(rf.right) + REGION_PAD_PX);
                int bottom = Math.min(Math.max(1, contentHeightPx), (int) Math.ceil(rf.bottom) + REGION_PAD_PX);
                Region fallback = new Region(left, top, right, bottom);
                try {
                    keyRegionBlackPart2.setPath(keyPathBlackPart2Transformed, fallback);
                } catch (Exception e2) {
                    Log.w(TAG, "final fallback failed for key2_black_test, using bounds region", e2);
                    keyRegionBlackPart2.set(fallback);
                }
            }
        }
    }

    @Override
    public boolean onTouchEvent(android.view.MotionEvent ev) {
        int x = Math.round(ev.getX());
        int y = Math.round(ev.getY());
        switch (ev.getActionMasked()) {
            case android.view.MotionEvent.ACTION_DOWN:
            case android.view.MotionEvent.ACTION_POINTER_DOWN:
                // 黑键优先
                if (keyRegionBlack != null && keyRegionBlack.contains(x, y)) {
                    pressedKey = 3;
                    pressedKeyName = "key2_black";
                    Log.i(TAG, "key pressed: " + pressedKeyName);
                    invalidate();
                    // 回调播放黑键声音（自行实现）
                    return true;
                } else if (keyRegionWhite1 != null && keyRegionWhite1.contains(x, y)) {
                    pressedKey = 1;
                    pressedKeyName = "key1_white";
                    Log.i(TAG, "key pressed: " + pressedKeyName);
                    invalidate();
                    return true;
                } else if (keyRegionWhite2 != null && keyRegionWhite2.contains(x, y)) {
                    pressedKey = 2;
                    pressedKeyName = "key3_white";
                    Log.i(TAG, "key pressed: " + pressedKeyName);
                    invalidate();
                    return true;
                }
                break;
            case android.view.MotionEvent.ACTION_UP:
            case android.view.MotionEvent.ACTION_CANCEL:
            case android.view.MotionEvent.ACTION_POINTER_UP:
                // 松手取消
                if (pressedKey != 0) {
                    // 可以在这里回调停止声音
                    pressedKey = 0;
                    pressedKeyName = null;
                    invalidate();
                }
                break;
            case android.view.MotionEvent.ACTION_MOVE:
                // 可选：实现拖动切换按键（此处保持简单）
                break;
        }
        return super.onTouchEvent(ev);
    }


    public int getContentWidth() {
        return contentWidthPx;
    }
}
