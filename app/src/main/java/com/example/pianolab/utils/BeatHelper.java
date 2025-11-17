package com.example.pianolab.utils;

import com.example.pianolab.feature.beat.model.BeatSettings;

public class BeatHelper {

    public static int BPM_TO_CPM(int bpm,int base_beat){
        return (int) Math.round(bpm * (4.0/base_beat));
    }

    public static int CPM_TO_BPM(int cpm,int base_beat){

        return (int) Math.round(cpm / (4.0/base_beat));
    }

    public static int clampBPM(int bpm){
        if (bpm< BeatSettings.MIN_BPM) return BeatSettings.MIN_BPM;
        if (bpm > BeatSettings.MAX_BPM) return BeatSettings.MAX_BPM;
        return bpm;
    }

    public static int clampCPM(int cpm,int basebeat){
        int MINCPM = BPM_TO_CPM(BeatSettings.MIN_BPM,basebeat);
        int MAXCPM = BPM_TO_CPM(BeatSettings.MAX_BPM,basebeat);
        if(cpm < MINCPM) return MINCPM;
        if (cpm > MAXCPM) return MAXCPM;
        return cpm;
    }


}
