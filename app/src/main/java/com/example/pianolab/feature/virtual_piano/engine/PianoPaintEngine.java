package com.example.pianolab.feature.virtual_piano.engine;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Region;
import android.graphics.drawable.Drawable;
import android.util.Log;

import java.util.Map;
import java.util.Set;

public class PianoPaintEngine {
    private static final String TAG = "PianoPaintEngine";
    private final Paint noteTextPaint = buildNotePaint();

    private static Paint buildNotePaint() {
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setColor(0xFF1E88E5);
        paint.setStyle(Paint.Style.FILL);
        paint.setTextAlign(Paint.Align.CENTER);
        return paint;
    }

    public void drawBackground(
            Canvas canvas,
            Drawable dStart,
            Drawable dOctave,
            Drawable dEnd,
            Paint fallbackStart,
            Paint fallbackOctave,
            Paint fallbackEnd,
            int swStart,
            int swOct,
            int swEnd,
            int octaveCount,
            int contentWidthPx,
            int height
    ) {
        int safeStart = swStart > 0 ? swStart : Math.max(1, contentWidthPx / Math.max(1, octaveCount * 7 + 3));
        int safeOct = swOct > 0 ? swOct : Math.max(1, (contentWidthPx - safeStart - swEnd) / Math.max(1, octaveCount));
        int safeEnd = swEnd > 0 ? swEnd : Math.max(1, contentWidthPx / Math.max(1, octaveCount * 7 + 3));

        int x = 0;
        drawSection(canvas, dStart, fallbackStart, x, safeStart, height);
        x += safeStart;

        for (int i = 0; i < octaveCount; i++) {
            drawSection(canvas, dOctave, fallbackOctave, x, safeOct, height);
            x += safeOct;
        }

        drawSection(canvas, dEnd, fallbackEnd, x, safeEnd, height);
    }

    private void drawSection(Canvas canvas, Drawable drawable, Paint fallback, int left, int width, int height) {
        if (drawable != null) {
            drawable.setBounds(left, 0, left + width, height);
            try {
                drawable.draw(canvas);
                return;
            } catch (Exception e) {
                Log.e(TAG, "drawSection drawable failed", e);
            }
        }
        canvas.drawRect(left, 0, left + width, height, fallback);
    }

    public void drawHighlights(
            Canvas canvas,
            Set<String> activeKeys,
            Map<String, Path> keyTransformedMap,
            Path keyPathWhite1Transformed,
            Path keyPathWhite2Transformed,
            Path keyPathBlackTransformed,
            Path keyPathBlackPart2Transformed,
            Paint keyHighlightWhitePaint,
            Paint keyHighlightBlackPaint
    ) {
        if (activeKeys == null || activeKeys.isEmpty()
                || keyTransformedMap == null
                || keyHighlightWhitePaint == null
                || keyHighlightBlackPaint == null) {
            return;
        }

        Path whiteCombined = new Path();
        boolean hasWhite = false;
        Path blackCombinedActive = new Path();
        boolean hasActiveBlack = false;

        for (String name : activeKeys) {
            if (name == null) continue;
            if (name.endsWith("_white")) {
                Path p = keyTransformedMap.get(name);
                if (p != null) {
                    whiteCombined.addPath(p);
                    hasWhite = true;
                } else if ("key1_white".equals(name) && keyPathWhite1Transformed != null) {
                    whiteCombined.addPath(keyPathWhite1Transformed);
                    hasWhite = true;
                } else if ("key3_white".equals(name) && keyPathWhite2Transformed != null) {
                    whiteCombined.addPath(keyPathWhite2Transformed);
                    hasWhite = true;
                }
            } else if (name.endsWith("_black") || name.endsWith("_black_part2")) {
                Path p = keyTransformedMap.get(name);
                if (p != null) {
                    blackCombinedActive.addPath(p);
                    hasActiveBlack = true;
                } else if (!name.endsWith("_black_part2") && keyPathBlackTransformed != null) {
                    blackCombinedActive.addPath(keyPathBlackTransformed);
                    hasActiveBlack = true;
                } else if (name.endsWith("_black_part2") && keyPathBlackPart2Transformed != null) {
                    blackCombinedActive.addPath(keyPathBlackPart2Transformed);
                    hasActiveBlack = true;
                }
            }
        }

        boolean hasAggregateBlack = false;
        if (keyPathBlackTransformed != null) {
            try {
                RectF rf = new RectF();
                keyPathBlackTransformed.computeBounds(rf, true);
                hasAggregateBlack = rf.width() > 0 && rf.height() > 0;
            } catch (Exception ignored) {
                hasAggregateBlack = true;
            }
        }

        if (hasWhite) {
            if (hasAggregateBlack) {
                Path drawPath = new Path(whiteCombined);
                try {
                    drawPath.op(keyPathBlackTransformed, Path.Op.DIFFERENCE);
                    canvas.drawPath(drawPath, keyHighlightWhitePaint);
                } catch (Exception e) {
                    Log.w(TAG, "white highlight difference failed, fallback direct draw", e);
                    canvas.drawPath(whiteCombined, keyHighlightWhitePaint);
                }
            } else {
                canvas.drawPath(whiteCombined, keyHighlightWhitePaint);
            }
        }

        if (hasActiveBlack) {
            try {
                canvas.drawPath(blackCombinedActive, keyHighlightBlackPaint);
            } catch (Exception e) {
                Log.w(TAG, "black highlight draw failed", e);
            }
        }
    }

    public void drawNoteLabels(
            Canvas canvas,
            boolean showPitchNames,
            Map<String, String> keyNoteNameMap,
            Map<String, Region> keyRegionMap,
            int contentHeightPx
    ) {
        if (!showPitchNames || keyNoteNameMap == null || keyNoteNameMap.isEmpty()
                || keyRegionMap == null || keyRegionMap.isEmpty()) {
            return;
        }
        float textSize = Math.max(12f, contentHeightPx * 0.08f);
        noteTextPaint.setTextSize(textSize);
        Paint.FontMetrics fm = noteTextPaint.getFontMetrics();

        for (Map.Entry<String, String> entry : keyNoteNameMap.entrySet()) {
            Region region = keyRegionMap.get(entry.getKey());
            if (region == null || region.isEmpty()) continue;
            Rect bounds = region.getBounds();
            float cx = bounds.exactCenterX();
            float baseline = bounds.bottom - bounds.height() * 0.08f - fm.bottom;
            canvas.drawText(entry.getValue(), cx, baseline, noteTextPaint);
        }
    }
}
