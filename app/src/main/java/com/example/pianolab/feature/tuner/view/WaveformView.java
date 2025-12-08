package com.example.pianolab.feature.tuner.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

import java.util.ArrayList;
import java.util.List;

public class WaveformView extends View {
    private final Paint linePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint barPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final List<Float> samples = new ArrayList<>();
    private boolean frequencyMode = true;

    public WaveformView(Context context) {
        super(context);
        init();
    }

    public WaveformView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public WaveformView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        linePaint.setStyle(Paint.Style.STROKE);
        linePaint.setStrokeWidth(2f);
        linePaint.setColor(0xFFFFFFFF);

        barPaint.setStyle(Paint.Style.FILL);
        barPaint.setColor(0x88FFFFFF);
    }

    public void setWaveform(List<Float> waveform) {
        samples.clear();
        if (waveform != null) {
            samples.addAll(waveform);
        }
        invalidate();
    }

    public void setFrequencyMode(boolean frequencyMode) {
        this.frequencyMode = frequencyMode;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (samples.isEmpty()) {
            return;
        }
        if (frequencyMode) {
            drawSpectrum(canvas);
        } else {
            drawWaveform(canvas);
        }
    }

    private void drawWaveform(Canvas canvas) {
        float width = getWidth();
        float height = getHeight();
        float centerY = height / 2f;
        float step = width / (float) (samples.size() - 1);
        float prevX = 0f;
        float prevY = centerY;
        for (int i = 0; i < samples.size(); i++) {
            float value = Math.max(-1f, Math.min(1f, samples.get(i)));
            float x = i * step;
            float y = centerY - value * (height / 2.2f);
            if (i > 0) {
                canvas.drawLine(prevX, prevY, x, y, linePaint);
            }
            prevX = x;
            prevY = y;
        }
    }

    private void drawSpectrum(Canvas canvas) {
        float width = getWidth();
        float height = getHeight();
        float max = 1f;
        for (float value : samples) {
            if (value > max) {
                max = value;
            }
        }
        if (max <= 0f) {
            return;
        }
        float barWidth = Math.max(width / samples.size(), 2f);
        for (int i = 0; i < samples.size(); i++) {
            float norm = samples.get(i) / max;
            float barHeight = norm * height;
            float left = i * barWidth;
            float right = left + barWidth * 0.8f;
            float top = height - barHeight;
            canvas.drawRect(left, top, right, height, barPaint);
        }
    }
}
