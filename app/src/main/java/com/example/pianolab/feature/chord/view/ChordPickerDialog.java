package com.example.pianolab.feature.chord.view;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.CheckBox;
import android.widget.NumberPicker;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;

import com.example.pianolab.R;

public class ChordPickerDialog extends AlertDialog {

    private final OnChordSelectedListener listener;
    private final boolean useFlats;
    private CheckBox cbAccidental;
    private NumberPicker npRootNote;
    private NumberPicker npChordType;

    private static final String[] ROOT_NOTES = {"C", "D", "E", "F", "G", "A", "B"};
    private static final String[] CHORD_TYPES = {"maj", "min", "dim", "aug", "maj7", "min7", "7", "dim7", "m7b5"};

    public interface OnChordSelectedListener {
        void onChordSelected(String chordName);
    }

    public ChordPickerDialog(@NonNull Context context, boolean useFlats, OnChordSelectedListener listener) {
        super(context);
        this.useFlats = useFlats;
        this.listener = listener;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Inflate the custom layout
        View view = LayoutInflater.from(getContext()).inflate(R.layout.dialog_chord_picker, null);
        setView(view);

        // Initialize views
        cbAccidental = view.findViewById(R.id.cb_accidental);
        npRootNote = view.findViewById(R.id.np_root_note);
        npChordType = view.findViewById(R.id.np_chord_type);

        // Setup NumberPickers
        npRootNote.setMinValue(0);
        npRootNote.setMaxValue(ROOT_NOTES.length - 1);
        npRootNote.setDisplayedValues(ROOT_NOTES);
        npRootNote.setWrapSelectorWheel(true);

        npChordType.setMinValue(0);
        npChordType.setMaxValue(CHORD_TYPES.length - 1);
        npChordType.setDisplayedValues(CHORD_TYPES);
        npChordType.setWrapSelectorWheel(true);

        // Setup Accidental CheckBox Text
        if (useFlats) {
            cbAccidental.setText("♭");
        } else {
            cbAccidental.setText("#");
        }

        // Setup Listeners
        npRootNote.setOnValueChangedListener((picker, oldVal, newVal) -> {
            String selectedNote = ROOT_NOTES[newVal];
            updateAccidentalVisibility(selectedNote);
        });

        // Initial update
        updateAccidentalVisibility(ROOT_NOTES[npRootNote.getValue()]);

        // Setup Dialog Buttons
        setButton(BUTTON_POSITIVE, "OK", (dialog, which) -> {
            if (listener != null) {
                listener.onChordSelected(buildChordString());
            }
        });
        setButton(BUTTON_NEGATIVE, "Cancel", (dialog, which) -> dismiss());

        // Play chord sound when OK is clicked
        // Actually, the listener handles generation, which triggers playback in ViewModel/Activity.
        // But we also want to play sound when user changes selection in the dialog?
        // The requirement says: "In 'construct chord' mode, when user constructs a chord... play chord sound".
        // This usually means after they confirm the selection.
        // So the current implementation where listener.onChordSelected is called on OK is correct.
        // The listener (Activity) calls ViewModel.generateChord, which updates keys and triggers playback.

        super.onCreate(savedInstanceState);
    }

    private void updateAccidentalVisibility(String note) {
        boolean show = false;
        if (useFlats) {
            // Show for D, E, G, A, B
            if (note.equals("D") || note.equals("E") || note.equals("G") || note.equals("A") || note.equals("B")) {
                show = true;
            }
        } else {
            // Show for C, D, F, G, A
            if (note.equals("C") || note.equals("D") || note.equals("F") || note.equals("G") || note.equals("A")) {
                show = true;
            }
        }

        if (show) {
            cbAccidental.setVisibility(View.VISIBLE);
        } else {
            cbAccidental.setVisibility(View.INVISIBLE);
            cbAccidental.setChecked(false);
        }
    }

    private String buildChordString() {
        String root = ROOT_NOTES[npRootNote.getValue()];
        String type = CHORD_TYPES[npChordType.getValue()];
        boolean accidentalChecked = cbAccidental.getVisibility() == View.VISIBLE && cbAccidental.isChecked();

        String accidentalSymbol = "";
        if (accidentalChecked) {
            accidentalSymbol = useFlats ? "b" : "#";
        }

        return root + accidentalSymbol + type;
    }
}
