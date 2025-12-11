package com.example.pianolab.utils;

import android.content.Context;

import java.util.ArrayList;
import java.util.List;

public class TunerHelper {

    public static float calculate_ref_freq(float measured, float standardFreq) {
        double ratio = measured / standardFreq;
        int steps = (int) Math.round(12d * (Math.log(ratio) / Math.log(2d)));
        return (float) (standardFreq * Math.pow(2d, steps / 12d));
    }
    public static float calculate_deviation_cent(float frequency, float referenceFrequency) {
        if (frequency <= 0 || referenceFrequency <= 0) {
            return 0f;
        }
        double ratio = frequency / referenceFrequency;
        return (float) (1200.0 * Math.log(ratio) / Math.log(2));
    }

    public static float calculate_RMS(float[] samples) {
        float sum = 0f;
        for (float sample : samples) {
            sum += sample * sample;
        }
        return (float) Math.sqrt(sum / samples.length);
    }

    public static float calculate_Median(List<Float> values) {
        if (values.isEmpty()) return 0f;
        List<Float> sorted = new ArrayList<>(values);
        java.util.Collections.sort(sorted);
        int mid = sorted.size() / 2;
        return sorted.size() % 2 == 0
                ? (sorted.get(mid - 1) + sorted.get(mid)) / 2f
                : sorted.get(mid);
    }

    public static int getAudioResourceForDetectedNote(Context context, String detectedNote) {
        if (detectedNote == null || detectedNote.isEmpty() || "--".equals(detectedNote)) {
            return 0;
        }

        // 解析音符和八度
        String noteName = detectedNote.replaceAll("\\d", ""); // 提取音名(C, C#, D...)
        String octaveStr = detectedNote.replaceAll("[^\\d]", ""); // 提取八度数字

        if (octaveStr.isEmpty()) {
            return 0;
        }

        int octave;
        try {
            octave = Integer.parseInt(octaveStr);
        } catch (NumberFormatException e) {
            return 0;
        }

        // 音符到半音偏移量的映射
        String[] noteNames = {"C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B"};
        int noteOffset = -1;

        for (int i = 0; i < noteNames.length; i++) {
            if (noteNames[i].equals(noteName)) {
                noteOffset = i;
                break;
            }
        }

        if (noteOffset == -1) {
            return 0;
        }

        // 计算MIDI编号
        // A0 = MIDI 21, C0 = MIDI 12
        // 公式: MIDI = (octave + 1) * 12 + noteOffset
        int midiNumber = (octave + 1) * 12 + noteOffset;

        // 钢琴范围检查: A0(21) ~ C8(108)
        if (midiNumber < 21 || midiNumber > 108) {
            return 0;
        }

        // 使用 VirtualPianoHelper 获取资源
        return VirtualPianoHelper.getAudioResourceForMidi(context, midiNumber);
    }

    public static String PitchNoteMapper(float measuredFreq, float standardFreq) {
        if (measuredFreq <= 0 || standardFreq <= 0) {
            return "--";
        }

        // 相对于当前参考频率的半音数
        double semitonesFromRef = 12.0 * Math.log(measuredFreq / standardFreq) / Math.log(2);
        int semitonesOffset = (int) Math.round(semitonesFromRef);

        // referenceFreq 对应 A4 (MIDI 69)
        int midiNumber = semitonesOffset + 69;

        int octave = (midiNumber / 12) - 1;
        int noteIndex = Math.floorMod(midiNumber, 12);

        String[] noteNames = {
                "C", "C#", "D", "D#", "E", "F",
                "F#", "G", "G#", "A", "A#", "B"
        };

        return noteNames[noteIndex] + octave;
    }

    public static float calculateNoteFrequency(String noteName, float referenceFreq) {
        if (noteName == null || noteName.isEmpty() || "--".equals(noteName)) {
            return referenceFreq;
        }

        // 1. 解析音符名称
        String note = noteName.replaceAll("[#b]", "").replaceAll("\\d", "");
        boolean isSharp = noteName.contains("#");
        boolean isFlat = noteName.contains("b");
        String octaveStr = noteName.replaceAll("[^\\d]", "");

        if (octaveStr.isEmpty()) {
            return referenceFreq;
        }

        int octave;
        try {
            octave = Integer.parseInt(octaveStr);
        } catch (NumberFormatException e) {
            return referenceFreq;
        }

        // 2. 计算该音符相对于 A 的半音偏移量
        int noteOffset = 0;
        switch (note) {
            case "C": noteOffset = -9; break;  // C 比 A 低 9 个半音
            case "D": noteOffset = -7; break;  // D 比 A 低 7 个半音
            case "E": noteOffset = -5; break;
            case "F": noteOffset = -4; break;
            case "G": noteOffset = -2; break;
            case "A": noteOffset = 0; break;   // A 是参考音
            case "B": noteOffset = 2; break;
            default: return referenceFreq;
        }

        if (isSharp) {
            noteOffset += 1;
        } else if (isFlat) {
            noteOffset -= 1;
        }

        // 3. 计算八度差异(相对于 A4)
        int octaveDiff = octave - 4;  // referenceFreq 对应 A4
        int totalSemitones = octaveDiff * 12 + noteOffset;

        // 4. 计算频率: referenceFreq * 2^(semitones/12)
        return (float) (referenceFreq * Math.pow(2.0, totalSemitones / 12.0));
    }
}
