package com.example.pianolab.feature.virtual_piano.view;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 88-key piano keyboard rendered from per-key PNG slices. Supports adjustable key scale,
 * animated horizontal scrolling, and multi-touch playback via {@link PianoSoundEngine}.
 */
public class SlicePianoKeyboardView extends View {
    private static final long SCROLL_ANIM_MS = 280L;
    private static final int COLOR_LABEL_WHITE = 0xFF727785;
    private static final int COLOR_SELECTION_WHITE = 0x99FF9800;
    private static final int COLOR_SELECTION_BLACK = 0x995400FF;

    public interface OnKeyToggledListener {
        void onKeyToggled(String keyName);
    }

    public interface OnScrollStateChangedListener {
        void onScrollStateChanged(float scrollX, float contentWidth, float viewportWidth);
    }

    public interface OnActiveKeysChangedListener {
        void onActiveKeysChanged(Set<Integer> activeMidiNotes);
    }

    private final PianoKeyboardLayout layout = new PianoKeyboardLayout();
    private final PianoSoundEngine soundEngine;
    private final Paint whiteLabelPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint selectionWhitePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint selectionBlackPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final SparseIntArray pointerToMidi = new SparseIntArray();
    private final SparseArray<Drawable> drawableCache = new SparseArray<>();
    private final Set<Integer> activeMidiNotes = new HashSet<>();
    private final Set<Integer> selectedMidiNotes = new HashSet<>();
    private final Map<Integer, Integer> midiRefCount = new HashMap<>();

    private boolean selectionMode;
    private String lastToggledKeyName;
    private OnKeyToggledListener keyToggledListener;
    private float keyScale = 1f;
    private float scrollX;
    private float maxScrollX;
    private boolean showPitchNames = true;
    private boolean sustainEnabled = false;
    private boolean layoutReady;
    private ValueAnimator scrollAnimator;
    private OnScrollStateChangedListener scrollStateListener;
    private OnActiveKeysChangedListener activeKeysListener;

    public SlicePianoKeyboardView(Context context) {
        this(context, null);
    }

    public SlicePianoKeyboardView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public SlicePianoKeyboardView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        soundEngine = new PianoSoundEngine(context);
        whiteLabelPaint.setColor(COLOR_LABEL_WHITE);
        whiteLabelPaint.setTextAlign(Paint.Align.CENTER);
        selectionWhitePaint.setStyle(Paint.Style.FILL);
        selectionWhitePaint.setColor(COLOR_SELECTION_WHITE);
        selectionBlackPaint.setStyle(Paint.Style.FILL);
        selectionBlackPaint.setColor(COLOR_SELECTION_BLACK);
        setClickable(true);
        setFocusable(true);
    }

    public void setOnScrollStateChangedListener(@Nullable OnScrollStateChangedListener listener) {
        scrollStateListener = listener;
        notifyScrollStateChanged();
    }

    public void setOnActiveKeysChangedListener(@Nullable OnActiveKeysChangedListener listener) {
        activeKeysListener = listener;
        notifyActiveKeysChanged();
    }

    public void setOnKeyToggledListener(@Nullable OnKeyToggledListener listener) {
        keyToggledListener = listener;
    }

    public void setSelectionMode(boolean enabled) {
        selectionMode = enabled;
        if (enabled) {
            clearActivePointers();
        }
        invalidate();
    }

    public void setSelectedKeys(@Nullable List<String> keyNames) {
        stopChordPlayback();
        if (keyNames == null || keyNames.isEmpty()) {
            if (lastToggledKeyName != null) {
                soundEngine.onKeyUp(lastToggledKeyName, 0);
                lastToggledKeyName = null;
            }
        }
        selectedMidiNotes.clear();
        if (keyNames != null) {
            for (String keyName : keyNames) {
                int midi = midiFromKeyName(keyName);
                if (midi != -1) {
                    selectedMidiNotes.add(midi);
                }
            }
        }
        invalidate();
    }

    public void playChord(@Nullable List<String> keyNames) {
        if (keyNames == null || keyNames.isEmpty()) {
            return;
        }
        soundEngine.playChord(keyNames);
    }

    public void stopChordPlayback() {
        soundEngine.stopChordPlayback();
    }

    public void setKeyboardScrollX(float target) {
        setScrollXImmediate(target);
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

    public float getKeyboardScrollX() {
        return scrollX;
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
            setScrollXImmediate(clampedTarget);
            return;
        }
        scrollAnimator = ValueAnimator.ofFloat(scrollX, clampedTarget);
        scrollAnimator.setDuration(SCROLL_ANIM_MS);
        scrollAnimator.setInterpolator(new DecelerateInterpolator());
        scrollAnimator.addUpdateListener(
                animation -> setScrollXImmediate((float) animation.getAnimatedValue()));
        scrollAnimator.start();
    }

    private void setScrollXImmediate(float target) {
        scrollX = clamp(target, 0f, maxScrollX);
        invalidate();
        notifyScrollStateChanged();
    }

    private void rebuildLayout() {
        if (getWidth() <= 0 || getHeight() <= 0) {
            layoutReady = false;
            return;
        }
        layout.layout(getHeight(), keyScale, dp(2f));
        maxScrollX = Math.max(0f, layout.getContentWidth() - getWidth());
        clampScroll();
        layoutReady = true;
        notifyScrollStateChanged();
    }

    private void notifyScrollStateChanged() {
        if (scrollStateListener == null || !layoutReady) {
            return;
        }
        scrollStateListener.onScrollStateChanged(scrollX, layout.getContentWidth(), getWidth());
    }

    private void notifyActiveKeysChanged() {
        if (activeKeysListener == null) {
            return;
        }
        activeKeysListener.onActiveKeysChanged(Collections.unmodifiableSet(new HashSet<>(activeMidiNotes)));
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

        if (selectionMode) {
            drawWhiteSelectionHighlights(canvas);
        }

        for (PianoKeyboardKey key : layout.getBlackKeys()) {
            drawKey(canvas, key);
        }

        if (selectionMode) {
            drawBlackSelectionHighlights(canvas);
        }

        if (showPitchNames) {
            drawNoteLabels(canvas);
        }

        canvas.restore();
    }

    private void drawNoteLabels(Canvas canvas) {
        float textSize = Math.max(sp(12f), layout.getWhiteKeyHeight() * 0.08f);
        whiteLabelPaint.setTextSize(textSize);
        Paint.FontMetrics whiteFm = whiteLabelPaint.getFontMetrics();

        for (PianoKeyboardKey key : layout.getWhiteKeys()) {
            drawKeyLabel(canvas, key, whiteLabelPaint, whiteFm, 0.12f);
        }
    }

    private void drawKeyLabel(
            Canvas canvas,
            PianoKeyboardKey key,
            Paint paint,
            Paint.FontMetrics fontMetrics,
            float bottomPaddingFraction) {
        float cx = key.bounds.centerX();
        float baseline =
                key.bounds.bottom - key.bounds.height() * bottomPaddingFraction - fontMetrics.bottom;
        canvas.drawText(key.label, cx, baseline, paint);
    }

    private void drawWhiteSelectionHighlights(Canvas canvas) {
        for (PianoKeyboardKey key : layout.getWhiteKeys()) {
            if (selectedMidiNotes.contains(key.midi)) {
                canvas.drawRect(key.bounds, selectionWhitePaint);
            }
        }
    }

    private void drawBlackSelectionHighlights(Canvas canvas) {
        for (PianoKeyboardKey key : layout.getBlackKeys()) {
            if (selectedMidiNotes.contains(key.midi)) {
                canvas.drawRect(key.bounds, selectionBlackPaint);
            }
        }
    }

    private void drawKey(Canvas canvas, PianoKeyboardKey key) {
        boolean pressed = !selectionMode && activeMidiNotes.contains(key.midi);
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
        drawable.setAlpha(255);
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

        if (selectionMode) {
            return handleSelectionTouch(event);
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
            clearActivePointers();
            return true;
        }

        return super.onTouchEvent(event);
    }

    private boolean handleSelectionTouch(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            performClick();
            PianoKeyboardKey hit = hitTest(event.getX(), event.getY());
            if (hit != null) {
                toggleSelectedKey(hit);
            }
            return true;
        }
        return true;
    }

    private void toggleSelectedKey(PianoKeyboardKey key) {
        stopChordPlayback();
        if (lastToggledKeyName != null) {
            soundEngine.onKeyUp(lastToggledKeyName, 0);
            lastToggledKeyName = null;
        }

        if (selectedMidiNotes.contains(key.midi)) {
            selectedMidiNotes.remove(key.midi);
        } else {
            selectedMidiNotes.add(key.midi);
            soundEngine.onKeyDown(key.soundKeyName, 0);
            lastToggledKeyName = key.soundKeyName;
        }

        if (keyToggledListener != null) {
            keyToggledListener.onKeyToggled(key.soundKeyName);
        }
        invalidate();
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
            notifyActiveKeysChanged();
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
            notifyActiveKeysChanged();
        } else {
            midiRefCount.put(midi, next);
        }
    }

    private void clearActivePointers() {
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
        invalidate();
        notifyActiveKeysChanged();
    }

    private void releaseAllPointers() {
        clearActivePointers();
        soundEngine.releaseAll();
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

    private float sp(float value) {
        return value * getResources().getDisplayMetrics().scaledDensity;
    }

    private static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }

    private static int midiFromKeyName(String keyName) {
        if (keyName == null || !keyName.startsWith("key")) {
            return -1;
        }
        int underscore = keyName.indexOf('_');
        if (underscore <= 3) {
            return -1;
        }
        try {
            return Integer.parseInt(keyName.substring(3, underscore)) + 20;
        } catch (NumberFormatException ignored) {
            return -1;
        }
    }

    public static List<Integer> midisFromKeyNames(@Nullable List<String> keyNames) {
        List<Integer> midis = new ArrayList<>();
        if (keyNames == null) {
            return midis;
        }
        for (String keyName : keyNames) {
            int midi = midiFromKeyName(keyName);
            if (midi != -1) {
                midis.add(midi);
            }
        }
        return midis;
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
