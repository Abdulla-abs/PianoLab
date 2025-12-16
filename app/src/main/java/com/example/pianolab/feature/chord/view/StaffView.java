package com.example.pianolab.feature.chord.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.View;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import com.example.pianolab.R;
import java.util.ArrayList;
import java.util.List;

public class StaffView extends View {

    public enum ClefType {
        TREBLE, BASS
    }

    private ClefType clefType = ClefType.TREBLE;
    private Drawable clefDrawable;
    private Drawable noteDrawable;
    private final List<String> noteKeys = new ArrayList<>();

    // Viewport heights from XML
    private static final float VIEWPORT_H_TREBLE = 153.33f;
    private static final float VIEWPORT_H_BASS = 117.5f;

    // Line Y positions from XML (Top to Bottom)
    // Treble: Line 5 (Top, F5) to Line 1 (Bottom, E4)
    private static final float[] TREBLE_LINES_Y = {18.1f, 40.06f, 61.83f, 84.05f, 105.83f};
    // Bass: Line 1 (Top, A3) to Line 5 (Bottom, G2)
    private static final float[] BASS_LINES_Y = {18.1f, 40.06f, 61.83f, 84.05f, 105.83f};

    // Reference notes for calculation
    // Treble Line 1 (Bottom) is E4.
    // Bass Line 5 (Bottom) is G2.
    private static final int TREBLE_BOTTOM_LINE_NOTE_INDEX = 44; // E4 (Key 44)
    private static final int BASS_BOTTOM_LINE_NOTE_INDEX = 23;   // G2 (Key 23)

    public StaffView(Context context) {
        super(context);
        init(context);
    }

    public StaffView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public StaffView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context) {
        noteDrawable = ContextCompat.getDrawable(context, R.drawable.whole_note);
    }

    public void setClefType(ClefType type) {
        this.clefType = type;
        if (type == ClefType.TREBLE) {
            clefDrawable = ContextCompat.getDrawable(getContext(), R.drawable.g_clef);
        } else {
            clefDrawable = ContextCompat.getDrawable(getContext(), R.drawable.f_clef);
        }
        invalidate();
    }

    public void setNotes(List<String> keys) {
        noteKeys.clear();
        if (keys != null) {
            noteKeys.addAll(keys);
        }
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        int w = getWidth();
        int h = getHeight();

        if (clefDrawable != null) {
            // Maintain aspect ratio or fit center? XML uses fitCenter.
            // We'll draw it to fill height, centered horizontally if needed, or just fill bounds.
            // Since we need to map Y coordinates precisely, we should probably fill height and adjust width to aspect ratio,
            // or just fill the view if the view is sized correctly.
            // For simplicity, let's assume the view's height is the authority and we scale the drawable to fit.
            // However, the lines are relative to the viewport height.
            clefDrawable.setBounds(0, 0, w, h);
            clefDrawable.draw(canvas);
        }

        if (noteKeys.isEmpty()) {
            // Draw rest? User mentioned whole_rest.xml but didn't strictly require it here if empty.
            // "whole_rest(当用户一个音都没选中时，显示全休止符去" -> Yes, show rest.
            Drawable rest = ContextCompat.getDrawable(getContext(), R.drawable.whole_rest);
            if (rest != null) {
                int rw = h / 4; // Arbitrary size
                int rh = h / 8;
                int rx = (w - rw) / 2;
                int ry = (h - rh) / 2;
                rest.setBounds(rx, ry, rx + rw, ry + rh);
                rest.draw(canvas);
            }
            return;
        }

        float viewportH = (clefType == ClefType.TREBLE) ? VIEWPORT_H_TREBLE : VIEWPORT_H_BASS;
        float scaleY = h / viewportH;

        // Calculate line spacing in pixels
        // In XML, spacing is approx 22 units.
        float lineSpacingUnits = 22f;
        float lineSpacingPx = lineSpacingUnits * scaleY;
        float halfSpacePx = lineSpacingPx / 2f;

        // Reference Y (Bottom Line)
        float refYUnits = (clefType == ClefType.TREBLE) ? TREBLE_LINES_Y[4] : BASS_LINES_Y[4];
        float refYPx = refYUnits * scaleY;

        int refNoteIndex = (clefType == ClefType.TREBLE) ? TREBLE_BOTTOM_LINE_NOTE_INDEX : BASS_BOTTOM_LINE_NOTE_INDEX;

        for (String key : noteKeys) {
            int keyIndex = getKeyIndex(key);
            if (keyIndex == -1) continue;

            // Filter based on clef
            // Treble: >= C4 (40). Bass: < C4 (40).
            if (clefType == ClefType.TREBLE && keyIndex < 40) continue;
            if (clefType == ClefType.BASS && keyIndex >= 40) continue;

            // Calculate Diatonic Step Difference
            int diatonicDiff = getDiatonicDistance(refNoteIndex, keyIndex);

            // Calculate Y
            // Higher pitch -> Lower Y.
            // Each diatonic step is half a space.
            float yPos = refYPx - (diatonicDiff * halfSpacePx);

            // Draw Note
            if (noteDrawable != null) {
                // Note size? Usually height of a space (approx lineSpacingPx).
                int noteH = (int) lineSpacingPx;
                int noteW = (int) (noteH * 1.5f); // Aspect ratio of whole note

                // Center X?
                // If multiple notes, we might need to offset them, but for now just center or stack.
                // User didn't specify complex layout.
                int xPos = (w - noteW) / 2;

                // yPos is the center of the note head?
                // Lines are drawn at y. Note should be centered on the line/space.
                int top = (int) (yPos - noteH / 2f);

                noteDrawable.setBounds(xPos, top, xPos + noteW, top + noteH);
                noteDrawable.draw(canvas);
            }
        }
    }

    private int getKeyIndex(String keyName) {
        try {
            int us = keyName.indexOf('_');
            if (keyName.startsWith("key") && us > 3) {
                return Integer.parseInt(keyName.substring(3, us));
            }
        } catch (Exception ignore) {}
        return -1;
    }

    // Returns number of diatonic steps (C, D, E...) between fromIndex and toIndex.
    // Positive if toIndex is higher pitch.
    private int getDiatonicDistance(int fromIndex, int toIndex) {
        int[] diatonicMap = {0, 0, 1, 1, 2, 3, 3, 4, 4, 5, 5, 6}; // C=0, C#=0, D=1...

        int fromOctave = (fromIndex - 4) / 12; // Key 4 is C1.
        int fromNote = (fromIndex - 4) % 12;
        int fromDiatonic = fromOctave * 7 + diatonicMap[fromNote];

        int toOctave = (toIndex - 4) / 12;
        int toNote = (toIndex - 4) % 12;
        int toDiatonic = toOctave * 7 + diatonicMap[toNote];

        return toDiatonic - fromDiatonic;
    }
}
