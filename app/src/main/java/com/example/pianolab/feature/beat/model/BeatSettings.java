package com.example.pianolab.feature.beat.model;


public class BeatSettings {
    private int bpm; // 每分钟节拍数
    private int beatsPerMeasure; // 每小节拍数，例如 4 表示 4/4
    private int baseBeat;  //以几分音符为一拍
    private int specialRhythmId; // 特殊节奏型的资源ID
    private int toneType;

    public static final int MIN_BPM = 20;
    public static final int MAX_BPM = 240;

    public static final int TONE_ELECTRONIC = 0;
    public static final int TONE_MECHANICAL = 1;

    public int getBaseBeat() {
        return baseBeat;
    }

    public void setBaseBeat(int baseBeat) {
        if (baseBeat == 4 || baseBeat == 8) {
            this.baseBeat = baseBeat;
        } else {
            // 非法值时使用默认 4
            this.baseBeat = 4;
        }
    }

    public int getSpecialRhythmId() {
        return specialRhythmId;
    }

    public void setSpecialRhythmId(int specialRhythmId) {
        this.specialRhythmId = specialRhythmId;
    }

    public int getToneType() {
        return toneType;
    }

    public void setToneType(int toneType) {
        this.toneType = toneType;
    }

    public BeatSettings() {
        this.bpm = 80;
        this.beatsPerMeasure = 4;
        this.baseBeat = 4;
        this.specialRhythmId = 0; // 0 or R.drawable.none
        this.toneType = TONE_ELECTRONIC;
    }

    public BeatSettings(int bpm, int beatsPerMeasure) {
        setBpm(bpm);
        setBeatsPerMeasure(beatsPerMeasure);
        this.baseBeat = 4;
        this.specialRhythmId = 0;
        this.toneType = TONE_ELECTRONIC;
    }

    public int getBpm() {
        return bpm;
    }

    public void setBpm(int bpm) {
        if (bpm < MIN_BPM) bpm = MIN_BPM;
        if (bpm > MAX_BPM) bpm = MAX_BPM;
        this.bpm = bpm;

        //TODO 超出范围添加  message box 提示
    }

    public int getBeatsPerMeasure() {
        return beatsPerMeasure;
    }

    public void setBeatsPerMeasure(int beatsPerMeasure) {
        if (beatsPerMeasure <= 0) beatsPerMeasure = 4;
        this.beatsPerMeasure = beatsPerMeasure;
    }



}
