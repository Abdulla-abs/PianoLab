package com.example.pianolab.feature.tuner.view;

import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.View;
import android.view.animation.DecelerateInterpolator;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.example.pianolab.R;

public class DeviationRulerView extends View {
    private static final float MIN_CENTS = -50f;
    private static final float MAX_CENTS = 50f;
    private static final float IN_TUNE_CENTS = 3f;
    private static final float TARGET_ZONE_CENTS = 5f;
    private static final float SCALE_USABLE_RATIO = 0.9f;
    private static final long ANIMATION_DURATION_MS = 120L;

    private final Paint trackPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint tickPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint centerLinePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint zonePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint labelPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint pointerPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint pointerLinePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path pointerPath = new Path();

    private final String[] tickLabels;

    private float displayedCents;
    private boolean active;
    private ValueAnimator centsAnimator;

    private float trackY;
    private float tickTopY;
    private float labelBaselineY;
    private float pointerTipY;
    private float pointerBaseY;
    private float pointerHalfWidth;
    private float centerLineTopY;
    private float zoneTopY;
    private float zoneHeight;

    public DeviationRulerView(Context context) {
        this(context, null);
    }

    public DeviationRulerView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public DeviationRulerView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        tickLabels = getResources().getStringArray(R.array.tuner_meter_ticks);
        initPaints(context);
        setContentDescription(context.getString(R.string.tuner_desc_meter));
    }

    private void initPaints(Context context) {
        int trackColor = ContextCompat.getColor(context, R.color.md_theme_tool_outline);
        int centerColor = ContextCompat.getColor(context, R.color.tuner_cursor_green);

        trackPaint.setStyle(Paint.Style.STROKE);
        trackPaint.setStrokeWidth(dp(2f));
        trackPaint.setColor(trackColor);

        tickPaint.setStyle(Paint.Style.STROKE);
        tickPaint.setStrokeWidth(dp(1f));
        tickPaint.setColor(trackColor);

        centerLinePaint.setStyle(Paint.Style.STROKE);
        centerLinePaint.setStrokeWidth(dp(2f));
        centerLinePaint.setColor(centerColor);

        zonePaint.setStyle(Paint.Style.FILL);
        zonePaint.setColor(ContextCompat.getColor(context, R.color.tuner_in_tune_zone));

        labelPaint.setTextAlign(Paint.Align.CENTER);
        labelPaint.setTextSize(sp(10f));
        labelPaint.setColor(trackColor);

        pointerPaint.setStyle(Paint.Style.FILL);
        pointerLinePaint.setStyle(Paint.Style.STROKE);
        pointerLinePaint.setStrokeWidth(dp(2f));
    }

    public void setActive(boolean active) {
        if (this.active == active) {
            return;
        }
        this.active = active;
        if (!active) {
            cancelAnimation();
            displayedCents = 0f;
        }
        invalidate();
    }

    public void setDeviation(float cents) {
        float clamped = Math.max(MIN_CENTS, Math.min(MAX_CENTS, cents));
        if (!active) {
            displayedCents = 0f;
            invalidate();
            return;
        }
        animateTo(clamped);
    }

    private void animateTo(float targetCents) {
        cancelAnimation();
        if (Math.abs(displayedCents - targetCents) < 0.05f) {
            displayedCents = targetCents;
            invalidate();
            return;
        }
        centsAnimator = ValueAnimator.ofFloat(displayedCents, targetCents);
        centsAnimator.setDuration(ANIMATION_DURATION_MS);
        centsAnimator.setInterpolator(new DecelerateInterpolator());
        centsAnimator.addUpdateListener(animation -> {
            displayedCents = (float) animation.getAnimatedValue();
            invalidate();
        });
        centsAnimator.start();
    }

    private void cancelAnimation() {
        if (centsAnimator != null) {
            centsAnimator.cancel();
            centsAnimator = null;
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        cancelAnimation();
        super.onDetachedFromWindow();
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        float labelSize = labelPaint.getTextSize();
        labelBaselineY = h - dp(2f);
        trackY = labelBaselineY - labelSize - dp(4f);
        tickTopY = trackY - dp(10f);
        centerLineTopY = trackY - dp(20f);
        zoneTopY = centerLineTopY;
        zoneHeight = trackY - zoneTopY;
        pointerTipY = trackY - dp(12f);
        pointerBaseY = trackY - dp(24f);
        pointerHalfWidth = dp(6f);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (getWidth() == 0 || getHeight() == 0) {
            return;
        }

        drawTargetZone(canvas);
        drawTrack(canvas);
        drawCenterLine(canvas);
        drawTicks(canvas);
        drawLabels(canvas);
        if (active) {
            drawPointer(canvas, displayedCents);
        }
    }

    private void drawTargetZone(Canvas canvas) {
        float left = centsToX(-TARGET_ZONE_CENTS);
        float right = centsToX(TARGET_ZONE_CENTS);
        canvas.drawRect(left, zoneTopY, right, zoneTopY + zoneHeight, zonePaint);
    }

    private void drawTrack(Canvas canvas) {
        canvas.drawLine(0f, trackY, getWidth(), trackY, trackPaint);
    }

    private void drawCenterLine(Canvas canvas) {
        float centerX = getWidth() / 2f;
        canvas.drawLine(centerX, centerLineTopY, centerX, trackY, centerLinePaint);
    }

    private void drawTicks(Canvas canvas) {
        for (int cents = -50; cents <= 50; cents += 10) {
            float x = centsToX(cents);
            canvas.drawLine(x, tickTopY, x, trackY, tickPaint);
        }
    }

    private void drawLabels(Canvas canvas) {
        int[] tickValues = {-50, -40, -30, -20, -10, 0, 10, 20, 30, 40, 50};
        int count = Math.min(tickLabels.length, tickValues.length);
        for (int i = 0; i < count; i++) {
            canvas.drawText(tickLabels[i], centsToX(tickValues[i]), labelBaselineY, labelPaint);
        }
    }

    private void drawPointer(Canvas canvas, float cents) {
        float pointerX = centsToX(cents);
        int pointerColor = resolvePointerColor(cents);
        pointerPaint.setColor(pointerColor);
        pointerLinePaint.setColor(pointerColor);

        pointerPath.reset();
        pointerPath.moveTo(pointerX, pointerTipY);
        pointerPath.lineTo(pointerX - pointerHalfWidth, pointerBaseY);
        pointerPath.lineTo(pointerX + pointerHalfWidth, pointerBaseY);
        pointerPath.close();
        canvas.drawPath(pointerPath, pointerPaint);
        canvas.drawLine(pointerX, pointerTipY, pointerX, trackY, pointerLinePaint);
    }

    private int resolvePointerColor(float cents) {
        float absCents = Math.abs(cents);
        if (absCents <= IN_TUNE_CENTS) {
            return ContextCompat.getColor(getContext(), R.color.tuner_cursor_green);
        }
        if (cents < -IN_TUNE_CENTS) {
            return ContextCompat.getColor(getContext(), R.color.tuner_cursor_yellow);
        }
        return ContextCompat.getColor(getContext(), R.color.tuner_cursor_red);
    }

    private float centsToX(float cents) {
        float midX = getWidth() / 2f;
        return midX + (cents / MAX_CENTS) * (getWidth() / 2f) * SCALE_USABLE_RATIO;
    }

    private float dp(float value) {
        return TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                value,
                getResources().getDisplayMetrics());
    }

    private float sp(float value) {
        return TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_SP,
                value,
                getResources().getDisplayMetrics());
    }
}
