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
    private final List<Float> samples = new ArrayList<>();

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
    }

    public void setWaveform(List<Float> waveform) {
        samples.clear();
        if (waveform != null) {
            samples.addAll(waveform);
        }
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (samples.isEmpty()) {
            return;
        }
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
}

