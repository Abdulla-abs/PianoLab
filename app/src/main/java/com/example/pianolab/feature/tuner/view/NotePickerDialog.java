package com.example.pianolab.feature.tuner.view;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.CheckBox;
import android.widget.NumberPicker;

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

        // 互斥逻辑
        if (checkFlat != null) {
            checkFlat.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (isChecked && checkSharp != null) checkSharp.setChecked(false);
            });
        }
        if (checkSharp != null) {
            checkSharp.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (isChecked && checkFlat != null) checkFlat.setChecked(false);
            });
        }

        super.onCreate(savedInstanceState);

        // 初始化 Picker
        pickerNoteName.setMinValue(0);
        pickerNoteName.setMaxValue(MusicTheoryHelper.SIMPLE_NOTE_NAME.length - 1);
        pickerNoteName.setDisplayedValues(MusicTheoryHelper.SIMPLE_NOTE_NAME);
        // setWrapSelectorWheel 会在 updateValidOptions 中动态设置

        pickerOctave.setMinValue(MIN_OCTAVE);
        pickerOctave.setMaxValue(MAX_OCTAVE);
        pickerOctave.setWrapSelectorWheel(false);

        // 添加监听器，当值改变时触发校验逻辑
        pickerOctave.setOnValueChangedListener((picker, oldVal, newVal) -> updateValidOptions());
        pickerNoteName.setOnValueChangedListener((picker, oldVal, newVal) -> updateValidOptions());

        // 设置初始值
        setCurrentNote(initialNote);

        // 立即执行一次校验，确保初始状态符合逻辑
        updateValidOptions();
    }

    /**
     * 核心逻辑：根据当前选择的八度和音名，限制可选范围和升降号
     */
    private void updateValidOptions() {
        int octave = pickerOctave.getValue();
        int currentNoteIndex = pickerNoteName.getValue(); // 0=C, 1=D, 2=E, 3=F, 4=G, 5=A, 6=B

        // --- 1. 根据八度限制音名选择范围 ---
        int minNoteIndex = 0;
        int maxNoteIndex = 6;
        boolean wrapNotes = true;

        if (octave == 0) {
            // 八度 0：钢琴最低音是 A0，所以只能选 A(5) 和 B(6)
            minNoteIndex = 5;
            maxNoteIndex = 6;
            wrapNotes = false;
        } else if (octave == 8) {
            // 八度 8：钢琴最高音是 C8，所以只能选 C(0)
            minNoteIndex = 0;
            maxNoteIndex = 0;
            wrapNotes = false;
        }

        // 如果范围发生变化，或者当前值不在新范围内，需要更新 Picker
        if (pickerNoteName.getMinValue() != minNoteIndex || pickerNoteName.getMaxValue() != maxNoteIndex) {
            // 暂时关闭循环滚动以防崩溃
            pickerNoteName.setWrapSelectorWheel(false);

            // 修正当前值：如果当前值超出了新范围，强制归位
            if (currentNoteIndex < minNoteIndex) {
                currentNoteIndex = minNoteIndex;
                pickerNoteName.setValue(currentNoteIndex);
            } else if (currentNoteIndex > maxNoteIndex) {
                currentNoteIndex = maxNoteIndex;
                pickerNoteName.setValue(currentNoteIndex);
            }

            pickerNoteName.setMinValue(minNoteIndex);
            pickerNoteName.setMaxValue(maxNoteIndex);
            pickerNoteName.setWrapSelectorWheel(wrapNotes);
        }

        // --- 2. 根据音名和八度限制升降号 (Checkbox) ---
        boolean enableSharp = true;
        boolean enableFlat = true;

        // 规则：八度 8 (C8) 是最高音，没有升降号
        if (octave == 8) {
            enableSharp = false;
            enableFlat = false;
        }

        // 规则：八度 0 的 A (A0) 是最低音，没有降号 (Ab0 不存在)
        if (octave == 0 && currentNoteIndex == 5) {
            enableFlat = false;
        }

        // 规则：不存在的半音 (Enharmonic simplifications)
        // 索引映射: 0:C, 1:D, 2:E, 3:F, 4:G, 5:A, 6:B
        if (currentNoteIndex == 0) enableFlat = false;  // Cb -> B (禁用降号)
        if (currentNoteIndex == 2) enableSharp = false; // E# -> F (禁用升号)
        if (currentNoteIndex == 3) enableFlat = false;  // Fb -> E (禁用降号)
        if (currentNoteIndex == 6) enableSharp = false; // B# -> C (禁用升号)

        updateCheckbox(checkSharp, enableSharp);
        updateCheckbox(checkFlat, enableFlat);
    }

    private void updateCheckbox(CheckBox checkBox, boolean enable) {
        if (checkBox == null) return;
        if (enable) {
            checkBox.setVisibility(View.VISIBLE);
            checkBox.setEnabled(true);
        } else {
            // 隐藏时必须取消选中，否则逻辑会出错
            checkBox.setChecked(false);
            checkBox.setVisibility(View.INVISIBLE); // 使用 INVISIBLE 保持布局占位，避免跳动
            checkBox.setEnabled(false);
        }
    }

    private String buildNoteString() {
        String noteName = MusicTheoryHelper.SIMPLE_NOTE_NAME[pickerNoteName.getValue()];
        String accidental = "";

        if (checkSharp != null && checkSharp.isChecked()) {
            accidental = "#";
        } else if (checkFlat != null && checkFlat.isChecked()) {
            accidental = "b";
        }

        int octave = pickerOctave.getValue();
        return noteName + accidental + octave;
    }

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
