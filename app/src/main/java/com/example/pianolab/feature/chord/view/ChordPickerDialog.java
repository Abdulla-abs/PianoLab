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
    private static final String[] CHORD_TYPES = {"maj", "min", "dim", "aug", "maj7", "min7", "7", "dim7", "m7b5","sus2","sus4"};

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


        super.onCreate(savedInstanceState);
    }

    private void updateAccidentalVisibility(String note) {
        boolean show = false;
        if (useFlats) {
            if (note.equals("D") || note.equals("E") || note.equals("G") || note.equals("A") || note.equals("B")) {
                show = true;
            }
        } else {
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
