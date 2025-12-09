package com.example.pianolab.feature.tuner.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.example.pianolab.R;

public class DeviationRulerView extends View {
    private static final float MIN_CENTS = -50f;
    private static final float MAX_CENTS = 50f;
    private static final float GREEN_ZONE = 5f;
    private static final float YELLOW_ZONE = 20f;

    private final Paint zonePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint linePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint cursorPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path cursorPath = new Path();

    private float currentCents = 0f;
    private float viewWidth = 0f;
    private float viewHeight = 0f;

    public DeviationRulerView(Context context) {
        this(context, null);
    }

    public DeviationRulerView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public DeviationRulerView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initPaints();
    }

    private void initPaints() {
        linePaint.setStyle(Paint.Style.STROKE);
        linePaint.setStrokeWidth(2f);
        linePaint.setColor(Color.WHITE);

        textPaint.setTextSize(24f);
        textPaint.setColor(Color.WHITE);
        textPaint.setTextAlign(Paint.Align.CENTER);

        cursorPaint.setColor(ContextCompat.getColor(getContext(), R.color.tuner_cursor_green));
        cursorPaint.setStyle(Paint.Style.FILL);
    }

    public void setDeviation(float cents) {
        this.currentCents = Math.max(MIN_CENTS, Math.min(MAX_CENTS, cents));
        invalidate();
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        viewWidth = w;
        viewHeight = h;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (viewWidth == 0 || viewHeight == 0) return;

        drawColorZones(canvas);
        drawRulerLines(canvas);
        drawCursor(canvas);
    }

    private void drawColorZones(Canvas canvas) {
        float centerX = viewWidth / 2f;
        float rulerTop = viewHeight * 0.1f;
        float rulerBottom = viewHeight * 0.9f;

        // 绿色区 (±5音分)
        float greenLeft = centerX + centsToPixel(-GREEN_ZONE);
        float greenRight = centerX + centsToPixel(GREEN_ZONE);
        zonePaint.setColor(Color.argb(40, 76, 175, 80)); // #4CAF50 半透明
        canvas.drawRect(greenLeft, rulerTop, greenRight, rulerBottom, zonePaint);

        // 黄色区 (±5~20音分)
        zonePaint.setColor(Color.argb(40, 255, 193, 7)); // #FFC107 半透明
        // 左侧黄色区
        float yellowLeftStart = centerX + centsToPixel(-YELLOW_ZONE);
        float yellowLeftEnd = centerX + centsToPixel(-GREEN_ZONE);
        canvas.drawRect(yellowLeftStart, rulerTop, yellowLeftEnd, rulerBottom, zonePaint);
        // 右侧黄色区
        float yellowRightStart = centerX + centsToPixel(GREEN_ZONE);
        float yellowRightEnd = centerX + centsToPixel(YELLOW_ZONE);
        canvas.drawRect(yellowRightStart, rulerTop, yellowRightEnd, rulerBottom, zonePaint);

        // 红色区 (±20以上)
        zonePaint.setColor(Color.argb(40, 244, 67, 54)); // #F44336 半透明
        // 左侧红色区
        canvas.drawRect(0, rulerTop, yellowLeftStart, rulerBottom, zonePaint);
        // 右侧红色区
        canvas.drawRect(yellowRightEnd, rulerTop, viewWidth, rulerBottom, zonePaint);
    }

    private void drawRulerLines(Canvas canvas) {
        float centerX = viewWidth / 2f;
        float rulerTop = viewHeight * 0.1f;
        float rulerBottom = viewHeight * 0.9f;
        float labelOffset = viewWidth * 0.02f;

        // 主刻度 (每10音分)
        linePaint.setStrokeWidth(3f);
        for (int cents = -50; cents <= 50; cents += 10) {
            float x = centerX + centsToPixel(cents);
            canvas.drawLine(x, rulerTop, x, rulerBottom, linePaint);

            // 刻度标签
            String label = String.valueOf(cents);
            float textX = x;
            if (cents == -50) {
                textX += labelOffset; // -50文字向右偏移（靠中心）
            } else if (cents == 50) {
                textX -= labelOffset; // 50文字向左偏移（靠中心）
            }
            canvas.drawText(label, textX, rulerTop - 10f, textPaint);
        }

        // 次要刻度 (每5音分)
        linePaint.setStrokeWidth(2f);
        for (int cents = -45; cents <= 45; cents += 10) {
            float x = centerX + centsToPixel(cents + 5);
            canvas.drawLine(x, rulerTop + 20f, x, rulerBottom - 20f, linePaint);
        }

        // 绘制-5音分刻度（长度与其他次要刻度一致）+ 数字标签
        float minus5X = centerX + centsToPixel(-5);
        canvas.drawLine(minus5X, rulerTop, minus5X, rulerBottom, linePaint);
        canvas.drawText("-5", minus5X, rulerTop - 10f, textPaint);

        // 绘制+5音分刻度（长度与其他次要刻度一致）+ 数字标签
        float plus5X = centerX + centsToPixel(5);
        canvas.drawLine(plus5X, rulerTop, plus5X, rulerBottom, linePaint);
        canvas.drawText("5", plus5X, rulerTop - 10f, textPaint);

        // 中心线 (0音分)
        linePaint.setStrokeWidth(4f);
        linePaint.setColor(Color.argb(180, 255, 255, 255));
        canvas.drawLine(centerX, rulerTop, centerX, rulerBottom, linePaint);
        linePaint.setColor(Color.WHITE);
    }

    private void drawCursor(Canvas canvas) {
        float centerX = viewWidth / 2f;
        float cursorX = centerX + centsToPixel(currentCents);
        float cursorY = viewHeight * 0.5f;

        // 根据偏差设置指针颜色
        if (Math.abs(currentCents) <= GREEN_ZONE) {
            cursorPaint.setColor(ContextCompat.getColor(getContext(), R.color.tuner_cursor_green));
        } else if (Math.abs(currentCents) <= YELLOW_ZONE) {
            cursorPaint.setColor(ContextCompat.getColor(getContext(), R.color.tuner_cursor_yellow));
        } else {
            cursorPaint.setColor(ContextCompat.getColor(getContext(), R.color.tuner_cursor_red));
        }

        // 绘制三角形指针
        cursorPath.reset();
        float size = 30f;
        cursorPath.moveTo(cursorX, cursorY - size);
        cursorPath.lineTo(cursorX - size * 0.6f, cursorY + size * 0.5f);
        cursorPath.lineTo(cursorX + size * 0.6f, cursorY + size * 0.5f);
        cursorPath.close();
        canvas.drawPath(cursorPath, cursorPaint);

        // 绘制指针下方的竖线
        Paint linePaint = new Paint(cursorPaint);
        linePaint.setStyle(Paint.Style.STROKE);
        linePaint.setStrokeWidth(4f);
        canvas.drawLine(cursorX, cursorY + size * 0.5f, cursorX, viewHeight * 0.9f, linePaint);
    }

    private float centsToPixel(float cents) {
        float range = MAX_CENTS - MIN_CENTS;
        return (cents / range) * viewWidth;
    }
}
