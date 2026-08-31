package com.example.pianolab.feature.chord.view;

import android.app.Dialog;
import android.content.Context;
import android.view.ContextThemeWrapper;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.view.ViewTreeObserver;
import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.core.widget.NestedScrollView;

import com.example.pianolab.R;

public class ChordPickerDialog {

    private static final String[] ROOT_NOTES = {"C", "D", "E", "F", "G", "A", "B"};
    private static final String[] CHORD_TYPES = {
            "maj", "min", "dim", "aug", "7", "maj7", "min7", "dim7", "m7b5", "sus2", "sus4"
    };

    private static final int ACCIDENTAL_NONE = 0;
    private static final int ACCIDENTAL_SHARP = 1;
    private static final int ACCIDENTAL_FLAT = 2;

    private final Context context;
    private final OnChordSelectedListener listener;

    private TextView btnAccidentalNone;
    private TextView btnAccidentalSharp;
    private TextView btnAccidentalFlat;
    private WheelPickerView pickerRootNote;
    private WheelPickerView pickerChordType;
    private TextView tvSelectedChordValue;

    private int selectedAccidental = ACCIDENTAL_NONE;

    @ColorInt
    private int accidentalSelectedTextColor;
    @ColorInt
    private int accidentalUnselectedTextColor;

    public interface OnChordSelectedListener {
        void onChordSelected(String chordName);
    }

    public ChordPickerDialog(@NonNull Context context, @SuppressWarnings("unused") boolean useFlats,
                             OnChordSelectedListener listener) {
        this.context = context;
        this.listener = listener;
    }

    public void show() {
        Context dialogContext = new ContextThemeWrapper(context, R.style.ThemeOverlay_PianoLab_ChordPickerDialog);
        View view = LayoutInflater.from(dialogContext).inflate(R.layout.dialog_chord_picker, null);
        bindViews(view);
        setupColors();
        setupPickers();
        setupAccidentalButtons();
        updateAccidentalAvailability();
        updateAccidentalUi();
        updateSelectedChordPreview();
        setupScrollIndicator(view);

        Dialog dialog = new Dialog(dialogContext);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(view);
        dialog.setCancelable(true);

        Window window = dialog.getWindow();
        if (window != null) {
            window.setBackgroundDrawable(new ColorDrawable(android.graphics.Color.TRANSPARENT));
            DisplayMetrics metrics = context.getResources().getDisplayMetrics();
            int maxWidth = (int) TypedValue.applyDimension(
                    TypedValue.COMPLEX_UNIT_DIP, 400f, metrics);
            int horizontalMargin = (int) TypedValue.applyDimension(
                    TypedValue.COMPLEX_UNIT_DIP, 24f, metrics);
            int dialogWidth = Math.min(metrics.widthPixels - horizontalMargin * 2, maxWidth);
            float maxHeightFraction = context.getResources().getFraction(
                    R.fraction.chord_picker_max_height_fraction, 1, 1);
            int dialogHeight = (int) (metrics.heightPixels * maxHeightFraction);
            window.setLayout(dialogWidth, dialogHeight);
        }

        view.findViewById(R.id.btn_cancel).setOnClickListener(v -> dialog.dismiss());
        view.findViewById(R.id.btn_ok).setOnClickListener(v -> {
            if (listener != null) {
                listener.onChordSelected(buildChordString());
            }
            dialog.dismiss();
        });

        dialog.show();
    }

    private void bindViews(View view) {
        btnAccidentalNone = view.findViewById(R.id.btn_accidental_none);
        btnAccidentalSharp = view.findViewById(R.id.btn_accidental_sharp);
        btnAccidentalFlat = view.findViewById(R.id.btn_accidental_flat);
        pickerRootNote = view.findViewById(R.id.picker_root_note);
        pickerChordType = view.findViewById(R.id.picker_chord_type);
        tvSelectedChordValue = view.findViewById(R.id.tv_selected_chord_value);
    }

    private void setupColors() {
        accidentalSelectedTextColor = ContextCompat.getColor(context, R.color.chord_picker_title);
        accidentalUnselectedTextColor = ContextCompat.getColor(context, R.color.chord_picker_label);
    }

    private void setupPickers() {
        pickerRootNote.setDisplayedValues(ROOT_NOTES);
        pickerChordType.setDisplayedValues(CHORD_TYPES);

        pickerRootNote.setOnValueChangedListener((picker, oldVal, newVal) -> {
            updateAccidentalAvailability();
            updateSelectedChordPreview();
        });
        pickerChordType.setOnValueChangedListener((picker, oldVal, newVal) -> updateSelectedChordPreview());
    }

    private void setupAccidentalButtons() {
        View.OnClickListener clickListener = v -> {
            if (!v.isEnabled()) {
                return;
            }
            if (v == btnAccidentalNone) {
                selectedAccidental = ACCIDENTAL_NONE;
            } else if (v == btnAccidentalSharp) {
                selectedAccidental = ACCIDENTAL_SHARP;
            } else if (v == btnAccidentalFlat) {
                selectedAccidental = ACCIDENTAL_FLAT;
            }
            updateAccidentalUi();
            updateSelectedChordPreview();
        };

        btnAccidentalNone.setOnClickListener(clickListener);
        btnAccidentalSharp.setOnClickListener(clickListener);
        btnAccidentalFlat.setOnClickListener(clickListener);
    }

    private void updateAccidentalUi() {
        resetAccidentalButton(btnAccidentalNone);
        resetAccidentalButton(btnAccidentalSharp);
        resetAccidentalButton(btnAccidentalFlat);

        TextView selectedButton;
        switch (selectedAccidental) {
            case ACCIDENTAL_SHARP:
                selectedButton = btnAccidentalSharp;
                break;
            case ACCIDENTAL_FLAT:
                selectedButton = btnAccidentalFlat;
                break;
            default:
                selectedButton = btnAccidentalNone;
                break;
        }

        selectedButton.setBackgroundResource(R.drawable.bg_accidental_selected);
        selectedButton.setTextColor(accidentalSelectedTextColor);
        selectedButton.setTypeface(Typeface.DEFAULT_BOLD);
    }

    private void resetAccidentalButton(TextView button) {
        button.setBackground(null);
        button.setTextColor(accidentalUnselectedTextColor);
        button.setTypeface(Typeface.DEFAULT);
    }

    private void updateAccidentalAvailability() {
        String note = ROOT_NOTES[pickerRootNote.getValue()];
        boolean sharpEnabled = canUseSharp(note);
        boolean flatEnabled = canUseFlat(note);

        setAccidentalButtonEnabled(btnAccidentalSharp, sharpEnabled);
        setAccidentalButtonEnabled(btnAccidentalFlat, flatEnabled);

        if ((selectedAccidental == ACCIDENTAL_SHARP && !sharpEnabled)
                || (selectedAccidental == ACCIDENTAL_FLAT && !flatEnabled)) {
            selectedAccidental = ACCIDENTAL_NONE;
            updateAccidentalUi();
        }
    }

    private void setAccidentalButtonEnabled(TextView button, boolean enabled) {
        button.setEnabled(enabled);
        button.setClickable(enabled);
        button.setAlpha(enabled ? 1f : 0.38f);
    }

    private boolean canUseSharp(String note) {
        return !note.equals("E") && !note.equals("B");
    }

    private boolean canUseFlat(String note) {
        return !note.equals("C") && !note.equals("F");
    }

    private String buildRootWithAccidental() {
        String root = ROOT_NOTES[pickerRootNote.getValue()];
        switch (selectedAccidental) {
            case ACCIDENTAL_SHARP:
                return root + "#";
            case ACCIDENTAL_FLAT:
                return root + "b";
            default:
                return root;
        }
    }

    private String buildChordString() {
        return buildRootWithAccidental() + CHORD_TYPES[pickerChordType.getValue()];
    }

    private void updateSelectedChordPreview() {
        String root = buildRootWithAccidental();
        String type = CHORD_TYPES[pickerChordType.getValue()];
        tvSelectedChordValue.setText(context.getString(R.string.chord_picker_preview_format, root, type));
    }

    private void setupScrollIndicator(View root) {
        NestedScrollView scrollView = root.findViewById(R.id.scroll_content);
        View track = root.findViewById(R.id.scroll_indicator_track);
        View thumb = root.findViewById(R.id.scroll_indicator_thumb);
        Runnable updateIndicator = () -> updateScrollIndicator(scrollView, track, thumb);

        scrollView.setOnScrollChangeListener(
                (NestedScrollView.OnScrollChangeListener) (v, scrollX, scrollY, oldScrollX, oldScrollY) ->
                        updateIndicator.run());
        scrollView.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                scrollView.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                updateIndicator.run();
            }
        });
    }

    private void updateScrollIndicator(NestedScrollView scrollView, View track, View thumb) {
        View child = scrollView.getChildAt(0);
        if (child == null) {
            return;
        }

        int contentHeight = child.getHeight();
        int viewportHeight = scrollView.getHeight();
        if (contentHeight <= viewportHeight) {
            track.setVisibility(View.GONE);
            thumb.setVisibility(View.GONE);
            return;
        }

        track.setVisibility(View.VISIBLE);
        thumb.setVisibility(View.VISIBLE);

        int trackHeight = track.getHeight();
        float viewportRatio = (float) viewportHeight / contentHeight;
        int minThumbHeight = context.getResources().getDimensionPixelSize(
                R.dimen.chord_picker_scroll_indicator_min_thumb);
        int thumbHeight = Math.max((int) (trackHeight * viewportRatio), minThumbHeight);

        int maxScroll = contentHeight - viewportHeight;
        float scrollFraction = maxScroll > 0 ? (float) scrollView.getScrollY() / maxScroll : 0f;
        int thumbTop = (int) ((trackHeight - thumbHeight) * scrollFraction);

        FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) thumb.getLayoutParams();
        params.height = thumbHeight;
        params.topMargin = thumbTop;
        thumb.setLayoutParams(params);
    }
}
