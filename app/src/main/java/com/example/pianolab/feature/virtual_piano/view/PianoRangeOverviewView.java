package com.example.pianolab.feature.virtual_piano.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.example.pianolab.R;
import com.example.pianolab.feature.virtual_piano.model.PianoKeyboardKey;
import com.example.pianolab.feature.virtual_piano.model.PianoKeyboardLayout;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Mini keyboard showing the full 88-key range, a draggable viewport indicator, and highlights for
 * keys currently pressed on the main piano view.
 */
public class PianoRangeOverviewView extends View {
    private static final int COLOR_WHITE_KEY = 0xFFE1E3E4;
    private static final int COLOR_BLACK_KEY = 0xFF2E3132;
    private static final int HIGHLIGHT_ALPHA = 168;
    private static final float KEY_CORNER_RADIUS_DP = 1.5f;
    private static final float VIEWPORT_CORNER_RADIUS_DP = 4f;

    public interface OnViewportScrollListener {
        void onViewportScroll(float scrollX);
    }

    private final PianoKeyboardLayout layout = new PianoKeyboardLayout();
    private final Paint whiteKeyPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint blackKeyPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint whiteHighlightPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint blackHighlightPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint viewportPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF viewportRect = new RectF();
    private final RectF keyRect = new RectF();
    private final Set<Integer> activeMidis = new HashSet<>();

    private float scrollX;
    private float contentWidth;
    private float viewportWidth;
    private float keyboardOffsetX;
    private float contentScale;
    private boolean layoutReady;
    private boolean draggingViewport;
    private float dragStartTouchX;
    private float dragStartScrollX;
    private OnViewportScrollListener viewportScrollListener;

    public PianoRangeOverviewView(Context context) {
        this(context, null);
    }

    public PianoRangeOverviewView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public PianoRangeOverviewView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        whiteKeyPaint.setStyle(Paint.Style.FILL);
        whiteKeyPaint.setColor(COLOR_WHITE_KEY);
        blackKeyPaint.setStyle(Paint.Style.FILL);
        blackKeyPaint.setColor(COLOR_BLACK_KEY);
        whiteHighlightPaint.setStyle(Paint.Style.FILL);
        whiteHighlightPaint.setColor(
                ContextCompat.getColor(context, R.color.virtual_piano_overview_white_key_highlight));
        whiteHighlightPaint.setAlpha(HIGHLIGHT_ALPHA);
        blackHighlightPaint.setStyle(Paint.Style.FILL);
        blackHighlightPaint.setColor(
                ContextCompat.getColor(context, R.color.virtual_piano_overview_black_key_highlight));
        blackHighlightPaint.setAlpha(HIGHLIGHT_ALPHA);
        viewportPaint.setStyle(Paint.Style.STROKE);
        viewportPaint.setStrokeWidth(dp(2f));
        viewportPaint.setColor(ContextCompat.getColor(context, R.color.md_theme_light_primary));
        setClickable(true);
    }

    public void setOnViewportScrollListener(@Nullable OnViewportScrollListener listener) {
        viewportScrollListener = listener;
    }

    public void setActiveMidis(Set<Integer> midis) {
        activeMidis.clear();
        if (midis != null) {
            activeMidis.addAll(midis);
        }
        invalidate();
    }

    public void updateViewport(float scrollX, float contentWidth, float viewportWidth) {
        this.scrollX = Math.max(0f, scrollX);
        this.contentWidth = Math.max(1f, contentWidth);
        this.viewportWidth = Math.max(0f, viewportWidth);
        if (layoutReady) {
            contentScale = layout.getContentWidth() / this.contentWidth;
        }
        invalidate();
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        rebuildLayout();
    }

    private void rebuildLayout() {
        if (getWidth() <= 0 || getHeight() <= 0) {
            layoutReady = false;
            return;
        }

        float availableWidth = Math.max(1f, getWidth());
        layout.layout(getHeight(), 1f, dp(1f));
        float fitScale = availableWidth / layout.getContentWidth();
        layout.layout(getHeight(), fitScale, dp(1f));
        keyboardOffsetX = (getWidth() - layout.getContentWidth()) * 0.5f;
        if (contentWidth > 0f) {
            contentScale = layout.getContentWidth() / contentWidth;
        }
        layoutReady = true;
        invalidate();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (!layoutReady) {
            return super.onTouchEvent(event);
        }

        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                if (isTouchInViewport(event.getX(), event.getY())) {
                    draggingViewport = true;
                    dragStartTouchX = event.getX();
                    dragStartScrollX = scrollX;
                    getParent().requestDisallowInterceptTouchEvent(true);
                    return true;
                }
                return false;

            case MotionEvent.ACTION_MOVE:
                if (draggingViewport) {
                    float deltaViewX = event.getX() - dragStartTouchX;
                    float deltaScrollX = contentScale > 0f ? deltaViewX / contentScale : 0f;
                    applyScroll(dragStartScrollX + deltaScrollX, true);
                    return true;
                }
                return false;

            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                if (draggingViewport) {
                    draggingViewport = false;
                    getParent().requestDisallowInterceptTouchEvent(false);
                    return true;
                }
                return false;

            default:
                return super.onTouchEvent(event);
        }
    }

    private boolean isTouchInViewport(float x, float y) {
        RectF viewportInView = getViewportRectInViewCoordinates();
        return viewportInView.contains(x, y);
    }

    private RectF getViewportRectInViewCoordinates() {
        float viewportLeft = keyboardOffsetX + scrollX * contentScale;
        float viewportRight = keyboardOffsetX + (scrollX + viewportWidth) * contentScale;
        viewportLeft = clamp(viewportLeft, keyboardOffsetX, keyboardOffsetX + layout.getContentWidth());
        viewportRight =
                clamp(
                        viewportRight,
                        viewportLeft,
                        keyboardOffsetX + layout.getContentWidth());
        viewportRect.set(viewportLeft, 0f, viewportRight, getHeight());
        return viewportRect;
    }

    private void applyScroll(float targetScrollX, boolean notifyListener) {
        float maxScrollX = Math.max(0f, contentWidth - viewportWidth);
        scrollX = clamp(targetScrollX, 0f, maxScrollX);
        invalidate();
        if (notifyListener && viewportScrollListener != null) {
            viewportScrollListener.onViewportScroll(scrollX);
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (!layoutReady) {
            return;
        }

        float keyRadius = dp(KEY_CORNER_RADIUS_DP);
        canvas.save();
        canvas.translate(keyboardOffsetX, 0f);

        for (PianoKeyboardKey key : layout.getWhiteKeys()) {
            keyRect.set(key.bounds);
            canvas.drawRoundRect(keyRect, keyRadius, keyRadius, whiteKeyPaint);
        }

        for (PianoKeyboardKey key : layout.getWhiteKeys()) {
            if (activeMidis.contains(key.midi)) {
                keyRect.set(key.bounds);
                canvas.drawRoundRect(keyRect, keyRadius, keyRadius, whiteHighlightPaint);
            }
        }

        for (PianoKeyboardKey key : layout.getBlackKeys()) {
            keyRect.set(key.bounds);
            canvas.drawRoundRect(keyRect, keyRadius, keyRadius, blackKeyPaint);
        }

        for (PianoKeyboardKey key : layout.getBlackKeys()) {
            if (activeMidis.contains(key.midi)) {
                keyRect.set(key.bounds);
                canvas.drawRoundRect(keyRect, keyRadius, keyRadius, blackHighlightPaint);
            }
        }

        float viewportLeft = scrollX * contentScale;
        float viewportRight = (scrollX + viewportWidth) * contentScale;
        viewportLeft = clamp(viewportLeft, 0f, layout.getContentWidth());
        viewportRight = clamp(viewportRight, viewportLeft, layout.getContentWidth());
        if (viewportRight - viewportLeft >= dp(2f)) {
            viewportRect.set(viewportLeft, 0f, viewportRight, getHeight());
            canvas.drawRoundRect(
                    viewportRect,
                    dp(VIEWPORT_CORNER_RADIUS_DP),
                    dp(VIEWPORT_CORNER_RADIUS_DP),
                    viewportPaint);
        }

        canvas.restore();
    }

    private float dp(float value) {
        return value * getResources().getDisplayMetrics().density;
    }

    private static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }
}
