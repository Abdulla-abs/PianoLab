package com.example.pianolab.feature.chord.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.View;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import com.example.pianolab.R;
import com.example.pianolab.utils.ChordHelper;

import java.util.ArrayList;
import java.util.List;

public class StaffView extends View {

    public enum ClefType {
        TREBLE, BASS
    }

    private ClefType clefType = ClefType.TREBLE;
    private Drawable clefDrawable;
    private Drawable noteDrawable;
    private Drawable c4NoteDrawable;
    private Drawable sharpDrawable;
    private boolean useFlats = false;
    private final List<String> noteKeys = new ArrayList<>();

    // Viewport heights from XML
    private static final float VIEWPORT_H_TREBLE = 153.33f;
    private static final float VIEWPORT_H_BASS = 153.33f;

    // Line Y positions from XML (Top to Bottom)
    private static final float[] TREBLE_LINES_Y = {18.1f, 40.06f, 61.83f, 84.05f, 105.83f};
    private static final float[] BASS_LINES_Y = {18.1f, 40.06f, 61.83f, 84.05f, 105.83f};

    // Reference notes for calculation
    private static final int TREBLE_BOTTOM_LINE_NOTE_INDEX = 44; // E4 (Key 44)
    private static final int BASS_BOTTOM_LINE_NOTE_INDEX = 23;   // G2 (Key 23)

    // Hyperparameters
    private float secondNoteShiftRatio = 0.85f;
    private float globalNoteShiftX = 60f;
    private float bassClefShiftY = 30f;
    private float flatSpacingRatio = 1.2f;
    private float flatNotePadding = 10f;
    private float c4Scale = 1.4f;

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
        c4NoteDrawable = ContextCompat.getDrawable(context, R.drawable.c4_note);
        sharpDrawable = ContextCompat.getDrawable(context, R.drawable.sharp);
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

    public void setUseFlats(boolean useFlats) {
        this.useFlats = useFlats;
        invalidate();
    }

    // Inner class for rendering info
    private class NoteRenderInfo implements Comparable<NoteRenderInfo> {
        int rawIndex;
        int visualIndex;
        boolean isBlack;
        String keyName;

        public NoteRenderInfo(int rawIndex, String keyName) {
            this.rawIndex = rawIndex;
            this.keyName = keyName;

            // Determine if black key based on index (0=C, 1=C#...)
            // C=0, C#=1, D=2, D#=3, E=4, F=5, F#=6, G=7, G#=8, A=9, A#=10, B=11
            int semitone = (rawIndex - 4) % 12;
            if (semitone < 0) semitone += 12;

            // Black keys are at indices 1, 3, 6, 8, 10 relative to C
            this.isBlack = (semitone == 1 || semitone == 3 || semitone == 6 || semitone == 8 || semitone == 10);

            if (this.isBlack) {
                if (useFlats) {
                    // Treat as flat of the next white key (e.g., C# -> Db)
                    // Visual position moves up one semitone to the white key line
                    this.visualIndex = rawIndex + 1;
                } else {
                    // Treat as sharp of the previous white key (e.g., Db -> C#)
                    // Visual position moves down one semitone to the white key line
                    this.visualIndex = rawIndex - 1;
                }
            } else {
                this.visualIndex = rawIndex;
            }
        }

        @Override
        public int compareTo(NoteRenderInfo o) {
            return Integer.compare(this.rawIndex, o.rawIndex);
        }
    }

    // Inner class for flat placement collision detection
    private static class PlacedFlat {
        int diatonicPos;
        int shiftIndex;

        public PlacedFlat(int diatonicPos, int shiftIndex) {
            this.diatonicPos = diatonicPos;
            this.shiftIndex = shiftIndex;
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        int saveCount = canvas.save();
        if (clefType == ClefType.BASS) {
            canvas.translate(0, bassClefShiftY);
        }

        int w = getWidth();
        int h = getHeight();

        if (clefDrawable != null) {
            clefDrawable.setBounds(0, 0, w, h);
            clefDrawable.draw(canvas);
        }

        float viewportH = (clefType == ClefType.TREBLE) ? VIEWPORT_H_TREBLE : VIEWPORT_H_BASS;
        float scaleY = h / viewportH;
        float lineSpacingUnits = 22f;
        float lineSpacingPx = lineSpacingUnits * scaleY;

        // 1. Filter and build NoteRenderInfo list
        List<NoteRenderInfo> notesToRender = new ArrayList<>();
        for (String key : noteKeys) {
            int keyIndex = ChordHelper.getKeyIndex(key);
            if (keyIndex == -1) continue;

            if (clefType == ClefType.TREBLE && keyIndex >= 40) {
                notesToRender.add(new NoteRenderInfo(keyIndex, key));
            } else if (clefType == ClefType.BASS && keyIndex < 40) {
                notesToRender.add(new NoteRenderInfo(keyIndex, key));
            }
        }
        java.util.Collections.sort(notesToRender);

        // Draw Whole Rest if empty
        if (notesToRender.isEmpty()) {
            Drawable rest = ContextCompat.getDrawable(getContext(), R.drawable.whole_rest);
            if (rest != null) {
                float line2YUnits = (clefType == ClefType.TREBLE) ? TREBLE_LINES_Y[1] : BASS_LINES_Y[1];
                float line2YPx = line2YUnits * scaleY;

                int rh = (int) (lineSpacingPx * 0.5f);
                int rw = (int) (lineSpacingPx * 1.2f);

                int rx = (w - rw) / 2;
                int ry = (int) line2YPx;

                rest.setBounds(rx, ry, rx + rw, ry + rh);
                rest.draw(canvas);
            }
            canvas.restoreToCount(saveCount);
            return;
        }

        float halfSpacePx = lineSpacingPx / 2f;
        float refYUnits = (clefType == ClefType.TREBLE) ? TREBLE_LINES_Y[4] : BASS_LINES_Y[4];
        float refYPx = refYUnits * scaleY;
        int refNoteIndex = (clefType == ClefType.TREBLE) ? TREBLE_BOTTOM_LINE_NOTE_INDEX : BASS_BOTTOM_LINE_NOTE_INDEX;

        Drawable flatDrawable = ContextCompat.getDrawable(getContext(), R.drawable.flat);
        List<PlacedFlat> placedAccidentals = new ArrayList<>();

        if (noteDrawable != null) {
            int noteH = (int) lineSpacingPx;
            int noteW = (int) (noteH * 1.5f);

            // Accidental dimensions
            int accW = (int) (noteW * 0.6f);
            int accH = (int) (noteH * 2.5f);

            int xBase = (w - noteW) / 2 + (int) globalNoteShiftX;
            int prevOffset = 0;

            for (int i = 0; i < notesToRender.size(); i++) {
                NoteRenderInfo currentNote = notesToRender.get(i);
                int currentOffset = 0;

                // Calculate Second Interval Shift
                if (i > 0) {
                    NoteRenderInfo prevNote = notesToRender.get(i - 1);
                    int diatonicDiff = ChordHelper.getDiatonicDistance(prevNote.visualIndex, currentNote.visualIndex);

                    if (diatonicDiff == 1) {
                        if (prevOffset == 0) {
                            currentOffset = 1;
                        }
                    }
                }

                // Calculate Y Position
                int distFromRef = ChordHelper.getDiatonicDistance(refNoteIndex, currentNote.visualIndex);
                float yPos = refYPx - (distFromRef * halfSpacePx);
                int top = (int) (yPos - noteH / 2f);

                // Calculate X Position (Note Head)
                int xShift = (currentOffset == 1) ? (int)(noteW * secondNoteShiftRatio) : 0;
                int xPos = xBase + xShift;

                // Draw Note Head
                // Use c4NoteDrawable for C4 (Key 40), otherwise use standard noteDrawable
                Drawable drawableToDraw = (currentNote.visualIndex == 40 && c4NoteDrawable != null) ? c4NoteDrawable : noteDrawable;
                if (drawableToDraw == c4NoteDrawable) {
                    // [修改] 针对 C4 进行暴力缩放
                    int scaledW = (int) (noteW * c4Scale);
                    int scaledH = (int) (noteH * c4Scale);

                    // 计算中心点，确保缩放后依然居中
                    int centerX = xPos + noteW / 2;
                    int centerY = top + noteH / 2;

                    drawableToDraw.setBounds(
                            centerX - scaledW / 2,
                            centerY - scaledH / 2,
                            centerX + scaledW / 2,
                            centerY + scaledH / 2
                    );
                } else {
                    // 普通音符正常绘制
                    drawableToDraw.setBounds(xPos, top, xPos + noteW, top + noteH);
                }
                drawableToDraw.draw(canvas);

                // Draw Accidental (Flat or Sharp) if needed
                if (currentNote.isBlack) {
                    Drawable accidentalDrawable = useFlats ? flatDrawable : sharpDrawable;

                    if (accidentalDrawable != null) {
                        // Find available shift index for accidental
                        int accShiftIndex = 0;
                        while (true) {
                            boolean collision = false;
                            for (PlacedFlat pf : placedAccidentals) {
                                if (pf.shiftIndex == accShiftIndex) {
                                    // Check vertical distance (less than octave = 7 steps)
                                    if (Math.abs(pf.diatonicPos - distFromRef) < 7) {
                                        collision = true;
                                        break;
                                    }
                                }
                            }
                            if (!collision) {
                                break;
                            }
                            accShiftIndex++;
                        }

                        placedAccidentals.add(new PlacedFlat(distFromRef, accShiftIndex));

                        // Calculate Accidental X Position
                        // Base is to the left of the main note column (xBase), ignoring second shift
                        int accRight = xBase - (int)flatNotePadding;
                        int accXOffset = (int) (accShiftIndex * accW * flatSpacingRatio);

                        int accL = accRight - accW - accXOffset;
                        int accR = accL + accW;

                        // Adjust Accidental Y to align center/belly with line
                        int accTop = (int) (yPos - accH * 0.65f);

                        accidentalDrawable.setBounds(accL, accTop, accR, accTop + accH);
                        accidentalDrawable.draw(canvas);
                    }
                }

                prevOffset = currentOffset;
            }
        }

        canvas.restoreToCount(saveCount);
    }


}
