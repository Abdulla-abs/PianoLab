package com.example.pianolab.feature.tuner.view;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.NumberPicker;
import android.widget.RadioGroup;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;

import com.example.pianolab.R;
import com.example.pianolab.utils.MusicTheoryHelper;

public class NotePickerDialog extends AlertDialog {
    private final OnNoteSelectedListener listener;
    private NumberPicker pickerNoteName;
    private final String initialNote;
    private CheckBox checkFlat;
    private CheckBox checkSharp;

    private NumberPicker pickerOctave;
    private static final String TAG = "NotePickerDialog";


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
        checkSharp = view.findViewById(R.id.check_sharp);
        checkFlat = view.findViewById(R.id.check_flat);
        pickerOctave = view.findViewById(R.id.picker_octave);
        setView(view);
        setTitle(R.string.tuner_select_target_note);
        setButton(Dialog.BUTTON_POSITIVE, getContext().getString(android.R.string.ok), (dialog, which) -> {
            if (listener != null) {
                listener.onNoteSelected(buildNoteString());
            }
        });
        setButton(Dialog.BUTTON_NEGATIVE, getContext().getString(android.R.string.cancel), (dialog, which) -> dismiss());

        if (checkFlat!=null){
            checkFlat.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if(isChecked&&checkSharp!=null) checkSharp.setChecked(false);
            });
        }
        if(checkSharp!=null){
            checkSharp.setOnCheckedChangeListener((buttonView,isChecked)->{
                if(isChecked&&checkFlat!=null) checkFlat.setChecked(false);
            });
        }

        super.onCreate(savedInstanceState);

        pickerNoteName.setMinValue(0);
        pickerNoteName.setMaxValue(MusicTheoryHelper.SIMPLE_NOTE_NAME.length - 1);
        pickerNoteName.setDisplayedValues(MusicTheoryHelper.SIMPLE_NOTE_NAME);
        pickerNoteName.setWrapSelectorWheel(true);

        pickerOctave.setMinValue(MIN_OCTAVE);
        pickerOctave.setMaxValue(MAX_OCTAVE);
        pickerOctave.setWrapSelectorWheel(false);

        setCurrentNote(initialNote);
    }

    private String buildNoteString() {
        String noteName = MusicTheoryHelper.SIMPLE_NOTE_NAME[pickerNoteName.getValue()];
        String accidental = "";

        if (checkSharp!=null&&checkSharp.isChecked()) {
            accidental = "#";
        } else if (checkFlat!=null &&checkFlat.isChecked()) {
            accidental = "b";
        }

        int octave = pickerOctave.getValue();
        return noteName + accidental + octave;
    }

    /**
     * 根据当前音符字符串设置默认值
     */
    public void setCurrentNote(String note) {
        if (checkSharp != null) checkSharp.setChecked(false);
        if (checkFlat != null) checkFlat.setChecked(false);
        if (note == null || note.isEmpty() || "--".equals(note)) {
            pickerNoteName.setValue(5); // 默认 A
            pickerOctave.setValue(4); // 默认 A4
            return;
        }

        // 解析音名
        String noteName = note.replaceAll("[#b]", "").replaceAll("\\d", "");
        for (int i = 0; i < MusicTheoryHelper.SIMPLE_NOTE_NAME.length; i++) {
            if (MusicTheoryHelper.SIMPLE_NOTE_NAME[i].equals(noteName)) {
                pickerNoteName.setValue(i);
                break;
            }
        }

        // 解析升降号
        if (note.contains("#") && checkSharp != null) {
            checkSharp.setChecked(true);
        } else if (note.contains("b") && checkFlat != null) {
            checkFlat.setChecked(true);
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
