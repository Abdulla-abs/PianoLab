package com.example.pianolab.feature.virtual_piano.view;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.util.SparseArray;
import android.util.SparseIntArray;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.DecelerateInterpolator;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.example.pianolab.feature.virtual_piano.engine.PianoSoundEngine;
import com.example.pianolab.feature.virtual_piano.model.PianoKeyboardKey;
import com.example.pianolab.feature.virtual_piano.model.PianoKeyboardLayout;
import com.example.pianolab.feature.virtual_piano.model.PianoPitch;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * 88-key piano keyboard rendered from per-key PNG slices. Supports adjustable key scale,
 * animated horizontal scrolling, and multi-touch playback via {@link PianoSoundEngine}.
 */
public class SlicePianoKeyboardView extends View {
    private static final int MIDI_CENTER_DEFAULT = 60; // Middle C
    private static final long SCROLL_ANIM_MS = 280L;

    private final PianoKeyboardLayout layout = new PianoKeyboardLayout();
    private final PianoSoundEngine soundEngine;
    private final SparseIntArray pointerToMidi = new SparseIntArray();
    private final SparseArray<Drawable> drawableCache = new SparseArray<>();
    private final Set<Integer> activeMidiNotes = new HashSet<>();
    private final Map<Integer, Integer> midiRefCount = new HashMap<>();

    private float keyScale = 1f;
    private float scrollX;
    private float maxScrollX;
    private boolean showPitchNames = true;
    private boolean sustainEnabled = false;
    private boolean layoutReady;
    private ValueAnimator scrollAnimator;

    public SlicePianoKeyboardView(Context context) {
        this(context, null);
    }

    public SlicePianoKeyboardView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public SlicePianoKeyboardView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        soundEngine = new PianoSoundEngine(context);
        setClickable(true);
        setFocusable(true);
    }

    public void setKeyScale(float scale) {
        float clamped = Math.max(0.45f, Math.min(1.6f, scale));
        if (Math.abs(clamped - keyScale) < 0.001f) {
            return;
        }
        float anchorRatio = maxScrollX > 0f ? scrollX / maxScrollX : 0.5f;
        keyScale = clamped;
        rebuildLayout();
        scrollX = anchorRatio * maxScrollX;
        clampScroll();
        invalidate();
    }

    public float getKeyScale() {
        return keyScale;
    }

    public void setShowPitchNames(boolean show) {
        showPitchNames = show;
        invalidate();
    }

    public void setSustainEnabled(boolean enabled) {
        sustainEnabled = enabled;
        soundEngine.setSustainEnabled(enabled);
    }

    public int getContentWidth() {
        return Math.round(layout.getContentWidth());
    }

    public int getKeyCenterX(int midi) {
        PianoKeyboardKey key = layout.findByMidi(midi);
        if (key == null) {
            return 0;
        }
        return Math.round(key.centerX());
    }

    public void scrollToMidiCenter(int midi) {
        PianoKeyboardKey key = layout.findByMidi(midi);
        if (key == null || getWidth() <= 0) {
            return;
        }
        float target = key.centerX() - getWidth() * 0.5f;
        setScrollXImmediate(target);
    }

    public void animateScrollBy(float delta) {
        animateScrollTo(scrollX + delta);
    }

    public void animateScrollByOctaves(int octaveDelta) {
        animateScrollBy(layout.getOctaveWidth() * octaveDelta);
    }

    public void animateScrollByViewportFraction(float fraction) {
        int width = getWidth();
        if (width <= 0) {
            return;
        }
        animateScrollBy(width * fraction);
    }

    public void animateScrollTo(float target) {
        if (!layoutReady) {
            setScrollXImmediate(target);
            return;
        }
        float clampedTarget = clamp(target, 0f, maxScrollX);
        if (scrollAnimator != null) {
            scrollAnimator.cancel();
        }
        if (Math.abs(clampedTarget - scrollX) < 1f) {
            scrollX = clampedTarget;
            invalidate();
            return;
        }
        scrollAnimator = ValueAnimator.ofFloat(scrollX, clampedTarget);
        scrollAnimator.setDuration(SCROLL_ANIM_MS);
        scrollAnimator.setInterpolator(new DecelerateInterpolator());
        scrollAnimator.addUpdateListener(
                animation -> {
                    scrollX = (float) animation.getAnimatedValue();
                    invalidate();
                });
        scrollAnimator.start();
    }

    private void setScrollXImmediate(float target) {
        if (scrollAnimator != null) {
            scrollAnimator.cancel();
        }
        scrollX = clamp(target, 0f, maxScrollX);
        invalidate();
    }

    private void rebuildLayout() {
        float gapPx = dp(2f);
        layout.layout(getHeight(), keyScale, gapPx);
        maxScrollX = Math.max(0f, layout.getContentWidth() - getWidth());
        clampScroll();
        layoutReady = getWidth() > 0 && getHeight() > 0;
    }

    private void clampScroll() {
        scrollX = clamp(scrollX, 0f, maxScrollX);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        rebuildLayout();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (!layoutReady) {
            return;
        }

        canvas.save();
        canvas.translate(-scrollX, 0f);

        for (PianoKeyboardKey key : layout.getWhiteKeys()) {
            drawKey(canvas, key);
        }
        for (PianoKeyboardKey key : layout.getBlackKeys()) {
            drawKey(canvas, key);
        }

        canvas.restore();
    }

    private void drawKey(Canvas canvas, PianoKeyboardKey key) {
        boolean pressed = activeMidiNotes.contains(key.midi);
        int resId = pressed ? key.pitch.pressedRes : key.pitch.normalRes;
        Drawable drawable = getCachedDrawable(resId);
        if (drawable == null) {
            return;
        }
        drawable.setBounds(
                Math.round(key.bounds.left),
                Math.round(key.bounds.top),
                Math.round(key.bounds.right),
                Math.round(key.bounds.bottom));
        drawable.setAlpha(showPitchNames ? 255 : 235);
        drawable.draw(canvas);
    }

    private Drawable getCachedDrawable(int resId) {
        Drawable cached = drawableCache.get(resId);
        if (cached != null) {
            return cached;
        }
        Drawable drawable = ContextCompat.getDrawable(getContext(), resId);
        if (drawable != null) {
            drawable = drawable.mutate();
            drawableCache.put(resId, drawable);
        }
        return drawable;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (!layoutReady) {
            return super.onTouchEvent(event);
        }

        final int action = event.getActionMasked();
        final int actionIndex = event.getActionIndex();

        if (action == MotionEvent.ACTION_DOWN || action == MotionEvent.ACTION_POINTER_DOWN) {
            int pointerId = event.getPointerId(actionIndex);
            handlePointerDown(pointerId, event.getX(actionIndex), event.getY(actionIndex));
            return true;
        }

        if (action == MotionEvent.ACTION_MOVE) {
            for (int i = 0; i < event.getPointerCount(); i++) {
                int pointerId = event.getPointerId(i);
                int midi = pointerToMidi.get(pointerId, -1);
                PianoKeyboardKey hit = hitTest(event.getX(i), event.getY(i));
                int hitMidi = hit != null ? hit.midi : -1;
                if (midi == hitMidi) {
                    continue;
                }
                if (midi != -1) {
                    releasePointer(pointerId, midi);
                }
                if (hitMidi != -1) {
                    pressPointer(pointerId, hit);
                }
            }
            return true;
        }

        if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_POINTER_UP) {
            int pointerId = event.getPointerId(actionIndex);
            int midi = pointerToMidi.get(pointerId, -1);
            if (midi != -1) {
                releasePointer(pointerId, midi);
            }
            return true;
        }

        if (action == MotionEvent.ACTION_CANCEL) {
            releaseAllPointers();
            return true;
        }

        return super.onTouchEvent(event);
    }

    private void handlePointerDown(int pointerId, float x, float y) {
        PianoKeyboardKey hit = hitTest(x, y);
        if (hit != null) {
            pressPointer(pointerId, hit);
        }
    }

    private void pressPointer(int pointerId, PianoKeyboardKey key) {
        pointerToMidi.put(pointerId, key.midi);
        int count = midiRefCount.getOrDefault(key.midi, 0);
        if (count == 0) {
            activeMidiNotes.add(key.midi);
            soundEngine.onKeyDown(key.soundKeyName, pointerId);
            invalidate();
        }
        midiRefCount.put(key.midi, count + 1);
    }

    private void releasePointer(int pointerId, int midi) {
        pointerToMidi.delete(pointerId);
        Integer count = midiRefCount.get(midi);
        if (count == null) {
            return;
        }
        int next = count - 1;
        if (next <= 0) {
            midiRefCount.remove(midi);
            activeMidiNotes.remove(midi);
            PianoKeyboardKey key = layout.findByMidi(midi);
            if (key != null) {
                soundEngine.onKeyUp(key.soundKeyName, pointerId);
            }
            invalidate();
        } else {
            midiRefCount.put(midi, next);
        }
    }

    private void releaseAllPointers() {
        for (int i = 0; i < pointerToMidi.size(); i++) {
            int pointerId = pointerToMidi.keyAt(i);
            int midi = pointerToMidi.valueAt(i);
            PianoKeyboardKey key = layout.findByMidi(midi);
            if (key != null) {
                soundEngine.onKeyUp(key.soundKeyName, pointerId);
            }
        }
        pointerToMidi.clear();
        activeMidiNotes.clear();
        midiRefCount.clear();
        soundEngine.releaseAll();
        invalidate();
    }

    private PianoKeyboardKey hitTest(float viewX, float viewY) {
        float contentX = viewX + scrollX;
        for (PianoKeyboardKey key : layout.getBlackKeys()) {
            if (key.contains(contentX, viewY)) {
                return key;
            }
        }
        for (PianoKeyboardKey key : layout.getWhiteKeys()) {
            if (key.contains(contentX, viewY)) {
                return key;
            }
        }
        return null;
    }

    private float dp(float value) {
        return value * getResources().getDisplayMetrics().density;
    }

    private static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if (scrollAnimator != null) {
            scrollAnimator.cancel();
        }
        releaseAllPointers();
    }
}
