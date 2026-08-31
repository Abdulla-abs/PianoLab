package com.example.pianolab.feature.tuner.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.example.pianolab.R;
import com.example.pianolab.utils.ThemeColors;

import java.util.ArrayList;
import java.util.List;

public class WaveformView extends View {
    private final Paint linePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint barPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint gridPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final List<Float> samples = new ArrayList<>();
    private boolean frequencyMode = true;

    public WaveformView(Context context) {
        this(context, null);
    }

    public WaveformView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public WaveformView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context) {
        int waveColor = resolveThemeColor(context, androidx.appcompat.R.attr.colorPrimary);
        int gridColor = ThemeColors.get(context, com.google.android.material.R.attr.colorOutlineVariant);

        linePaint.setStyle(Paint.Style.STROKE);
        linePaint.setStrokeWidth(dp(2f));
        linePaint.setColor(waveColor);

        barPaint.setStyle(Paint.Style.FILL);
        barPaint.setColor(waveColor);

        gridPaint.setStyle(Paint.Style.STROKE);
        gridPaint.setStrokeWidth(dp(1f));
        gridPaint.setColor(gridColor);
        gridPaint.setAlpha(128);

        setContentDescription(context.getString(R.string.tuner_desc_waveform));
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
        drawGrid(canvas);
        if (samples.isEmpty()) {
            return;
        }
        if (frequencyMode) {
            drawSpectrum(canvas);
        } else {
            drawWaveform(canvas);
        }
    }

    private void drawGrid(Canvas canvas) {
        float width = getWidth();
        float height = getHeight();
        if (width <= 0f || height <= 0f) {
            return;
        }

        float centerY = height / 2f;
        canvas.drawLine(0f, centerY, width, centerY, gridPaint);

        int columns = 5;
        for (int i = 1; i < columns; i++) {
            float x = (width / columns) * i;
            canvas.drawLine(x, 0f, x, height, gridPaint);
        }
    }

    private void drawWaveform(Canvas canvas) {
        if (samples.size() < 2) {
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

        float barWidth = Math.max(width / samples.size(), dp(2f));
        for (int i = 0; i < samples.size(); i++) {
            float norm = samples.get(i) / max;
            float barHeight = norm * height;
            float left = i * barWidth;
            float right = left + barWidth * 0.8f;
            float top = height - barHeight;
            canvas.drawRect(left, top, right, height, barPaint);
        }
    }

    private int resolveThemeColor(Context context, int attr) {
        TypedValue typedValue = new TypedValue();
        if (context.getTheme().resolveAttribute(attr, typedValue, true)) {
            return typedValue.data;
        }
        return ThemeColors.get(context, com.google.android.material.R.attr.colorPrimary);
    }

    private float dp(float value) {
        return TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                value,
                getResources().getDisplayMetrics());
    }
}
