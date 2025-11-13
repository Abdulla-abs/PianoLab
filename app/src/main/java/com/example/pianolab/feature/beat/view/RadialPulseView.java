package com.example.pianolab.feature.beat.view;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.View;

import androidx.annotation.Nullable;

public class RadialPulseView extends View {

    private Paint paintStrong;
    private Paint paintWeak;
    private int numLines = 24;
    private float innerRadius; // px
    private float baseLength; // px
    private float strongExtra; // px
    private float weakExtra; // px
    private float currentScale = 0f; // 0..1
    private boolean lastStrong = false;
    private ValueAnimator animator;

    public RadialPulseView(Context context) {
        this(context, null);
    }

    public RadialPulseView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    private void init(Context context) {
        paintStrong = new Paint(Paint.ANTI_ALIAS_FLAG);
        paintStrong.setColor(0xFFFF4081); // pink accent
        paintStrong.setStrokeWidth(dpToPx(3));
        paintStrong.setStrokeCap(Paint.Cap.ROUND);

        paintWeak = new Paint(Paint.ANTI_ALIAS_FLAG);
        paintWeak.setColor(0xFFAAAAAA); // muted gray
        paintWeak.setStrokeWidth(dpToPx(1.6f));
        paintWeak.setStrokeCap(Paint.Cap.ROUND);

        innerRadius = dpToPx(8);
        baseLength = dpToPx(28);
        strongExtra = dpToPx(48);
        weakExtra = dpToPx(20);

        animator = ValueAnimator.ofFloat(1f, 0f);
        animator.setDuration(220);
        animator.addUpdateListener(animation -> {
            currentScale = (float) animation.getAnimatedValue();
            invalidate();
        });
    }

    private float dpToPx(float dp) {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, getResources().getDisplayMetrics());
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        int w = getWidth();
        int h = getHeight();
        float cx = w / 2f;
        float cy = h / 2f;

        // 每条线的当前额外长度基于 currentScale
        float extra = currentScale * (lastStrong ? strongExtra : weakExtra);
        float length = baseLength + extra;

        for (int i = 0; i < numLines; i++) {
            double angle = 2 * Math.PI * i / numLines - Math.PI / 2; // start from top
            float cos = (float) Math.cos(angle);
            float sin = (float) Math.sin(angle);
            float sx = cx + cos * innerRadius;
            float sy = cy + sin * innerRadius;
            float ex = cx + cos * (innerRadius + length);
            float ey = cy + sin * (innerRadius + length);

            // 线条颜色/宽度根据是否为强拍来决定（已通过 paintStrong/weak 设置）
            Paint p = lastStrong ? paintStrong : paintWeak;
            canvas.drawLine(sx, sy, ex, ey, p);
        }
    }

    /**
     * Trigger a pulse animation. Call on each beat. strong==true for downbeat
     */
    public void pulse(boolean strong) {
        lastStrong = strong;
        if (animator != null && animator.isRunning()) {
            animator.cancel();
        }
        // 重新创建 animator to avoid lingering listeners if needed
        animator = ValueAnimator.ofFloat(1f, 0f);
        animator.setDuration(strong ? 300 : 220);
        animator.addUpdateListener(animation -> {
            currentScale = (float) animation.getAnimatedValue();
            invalidate();
        });
        animator.start();
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if (animator != null) {
            animator.cancel();
            animator = null;
        }
    }
}

