package com.example.pianolab.utils;

import com.example.pianolab.R;
import com.example.pianolab.feature.beat.model.BeatSettings;

import java.util.HashMap;
import java.util.Map;

public class BeatHelper {

    //分别对应countdown,weak,strong 的参数
    public static float[][] VOLUME_PARA = {
            {0.1f, 1.0f, 3.5f},   // 电子合成音
            {0.6f, 2.2f, 4.3f},   // 木制节拍器
            {0.3f, 0.3f, 3.5f},   // 鼓组
            {1.0f, 0.9f, 3.1f}    // 马林巴琴
    };

    public static final Map<Integer, double[]> RHYTHM_DATA = new HashMap<>();

    static {
        // 前附点: 0.75 + 0.25
        RHYTHM_DATA.put(R.drawable.foredot, new double[]{
                2.0,                        // Count
                0.0, 0.75, 0.0, 0.0, 0.0,   // Offsets
                0.0, 0.0, 0.0, 0.0, 0.0     // Accents
        });

        // 后附点: 0.25 + 0.75
        RHYTHM_DATA.put(R.drawable.backdot, new double[]{
                2.0,
                0.0, 0.25, 0.0, 0.0, 0.0,
                0.0, 0.0, 0.0, 0.0, 0.0
        });

        // 前八后十六: 0.5 + 0.25 + 0.25
        RHYTHM_DATA.put(R.drawable.fore8back16, new double[]{
                3.0,
                0.0, 0.5, 0.75, 0.0, 0.0,
                0.0, 0.0, 0.0, 0.0, 0.0
        });

        // 前十六后八: 0.25 + 0.25 + 0.5
        RHYTHM_DATA.put(R.drawable.fore16back8, new double[]{
                3.0,
                0.0, 0.25, 0.5, 0.0, 0.0,
                0.0, 0.0, 0.0, 0.0, 0.0
        });

        // 三连音: 1/3, 1/3, 1/3
        RHYTHM_DATA.put(R.drawable.tercet, new double[]{
                3.0,
                0.0, 1.0/3.0, 2.0/3.0, 0.0, 0.0,
                0.0, 0.0, 0.0, 0.0, 0.0
        });

        // 小切分: 0.25 + 0.5 + 0.25 (中间重音)
        RHYTHM_DATA.put(R.drawable.syncopation, new double[]{
                3.0,
                0.0, 0.25, 0.75, 0.0, 0.0,
                0.0, 1.0, 0.0, 0.0, 0.0     // Index 7 (accents[1]) is 1.0
        });
        RHYTHM_DATA.put(R.drawable.quintuplet, new double[]{
                5.0,                            // Count
                0.0, 0.2, 0.4, 0.6, 0.8,        // Offsets
                0.0, 0.0, 0.0, 0.0, 0.0        // Index 7 (accents[1]) is 1.0
        });
    }


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
    
    public static String getBpmDescription(int bpm){
        if (bpm <= 40) {
            return "Grave";
        } else if (bpm <= 45) {
            return "Lento";
        } else if (bpm <= 50) {
            return "Largo";
        } else if (bpm <= 60) {
            return "Adagio";
        } else if (bpm <= 70) {
            return "Adagietto";
        } else if (bpm <= 85) {
            return "Andante";
        } else if (bpm <= 97) {
            return "Moderato";
        } else if (bpm <= 109) {
            return "Allegretto";
        } else if (bpm <= 132) {
            return "Allegro";
        } else if (bpm <= 140) {
            return "Vivace";
        } else if (bpm <= 177) {
            return "Presto";
        } else {
            return "Prestissimo";
        }
    }
    public static String getBpmDescriptionExtra(int bpm){
        if (bpm <= 40) {
            return "庄板";
        } else if (bpm <= 45) {
            return "慢板";
        } else if (bpm <= 50) {
            return "广板";
        } else if (bpm <= 60) {
            return "柔板";
        } else if (bpm <= 70) {
            return "小柔板";
        } else if (bpm <= 85) {
            return "行板";
        } else if (bpm <= 97) {
            return "中板";
        } else if (bpm <= 109) {
            return "小快板";
        } else if (bpm <= 132) {
            return "快板";
        } else if (bpm <= 140) {
            return "活泼的快板";
        } else if (bpm <= 177) {
            return "急板";
        } else {
            return "最急板";
        }
    }


}
