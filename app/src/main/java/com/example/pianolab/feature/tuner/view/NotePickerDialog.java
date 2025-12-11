package com.example.pianolab.feature.tuner.view;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.NumberPicker;
import android.widget.RadioGroup;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;

import com.example.pianolab.R;

public class NotePickerDialog extends AlertDialog {
    private final OnNoteSelectedListener listener;
    private NumberPicker pickerNoteName;
    private final String initialNote;
    private RadioGroup radioAccidental;
    private NumberPicker pickerOctave;
    private static final String TAG = "NotePickerDialog";

    private static final String[] NOTE_NAMES = {"C", "D", "E", "F", "G", "A", "B"};
    private static final int MIN_OCTAVE = 0;
    private static final int MAX_OCTAVE = 8;

    public interface OnNoteSelectedListener {
        void onNoteSelected(String note);
    }

    public NotePickerDialog(@NonNull Context context, String currentNote, OnNoteSelectedListener listener) {
        super(context);
        this.listener = listener;
        this.initialNote = currentNote;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        View view = LayoutInflater.from(getContext()).inflate(R.layout.dialog_note_picker, null);
        pickerNoteName = view.findViewById(R.id.picker_note_name);
        radioAccidental = view.findViewById(R.id.radio_accidental);
        pickerOctave = view.findViewById(R.id.picker_octave);
        setView(view);
        setTitle(R.string.tuner_select_target_note);
        setButton(Dialog.BUTTON_POSITIVE, getContext().getString(android.R.string.ok), (dialog, which) -> {
            if (listener != null) {
                listener.onNoteSelected(buildNoteString());
            }
        });
        setButton(Dialog.BUTTON_NEGATIVE, getContext().getString(android.R.string.cancel), (dialog, which) -> dismiss());

        super.onCreate(savedInstanceState);

        pickerNoteName.setMinValue(0);
        pickerNoteName.setMaxValue(NOTE_NAMES.length - 1);
        pickerNoteName.setDisplayedValues(NOTE_NAMES);
        pickerNoteName.setWrapSelectorWheel(true);

        pickerOctave.setMinValue(MIN_OCTAVE);
        pickerOctave.setMaxValue(MAX_OCTAVE);
        pickerOctave.setWrapSelectorWheel(false);

        setCurrentNote(initialNote);
    }

    private String buildNoteString() {
        String noteName = NOTE_NAMES[pickerNoteName.getValue()];
        String accidental = "";

        int checkedId = radioAccidental.getCheckedRadioButtonId();
        if (checkedId == R.id.radio_sharp) {
            accidental = "#";
        } else if (checkedId == R.id.radio_flat) {
            accidental = "b";
        }

        int octave = pickerOctave.getValue();
        return noteName + accidental + octave;
    }

    /**
     * 根据当前音符字符串设置默认值
     */
    public void setCurrentNote(String note) {
        if (note == null || note.isEmpty() || "--".equals(note)) {
            pickerNoteName.setValue(5); // 默认 A
            pickerOctave.setValue(4); // 默认 A4
            return;
        }

        // 解析音名
        String noteName = note.replaceAll("[#b]", "").replaceAll("\\d", "");
        for (int i = 0; i < NOTE_NAMES.length; i++) {
            if (NOTE_NAMES[i].equals(noteName)) {
                pickerNoteName.setValue(i);
                break;
            }
        }

        // 解析升降号
        if (note.contains("#")) {
            radioAccidental.check(R.id.radio_sharp);
        } else if (note.contains("b")) {
            radioAccidental.check(R.id.radio_flat);
        }

        // 解析八度
        String octaveStr = note.replaceAll("[^\\d]", "");
        if (!octaveStr.isEmpty()) {
            try {
                int octave = Integer.parseInt(octaveStr);
                if (octave >= MIN_OCTAVE && octave <= MAX_OCTAVE) {
                    pickerOctave.setValue(octave);
                }
            } catch (NumberFormatException ignored) {
            }
        }
    }
}
