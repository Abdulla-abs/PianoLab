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
    
    public static String getBpmDescription(int bpm){
        if (bpm <= 40) {
            return "Grave(庄板)";
        } else if (bpm <= 45) {
            return "Lento(慢板)";
        } else if (bpm <= 50) {
            return "Largo(广板)";
        } else if (bpm <= 60) {
            return "Adagio(柔板)";
        } else if (bpm <= 70) {
            return "Adagietto(小柔板)";
        } else if (bpm <= 85) {
            return "Andante(行板)";
        } else if (bpm <= 97) {
            return "Moderato(中板)";
        } else if (bpm <= 109) {
            return "Allegretto(小快板)";
        } else if (bpm <= 132) {
            return "Allegro(快板)";
        } else if (bpm <= 140) {
            return "Vivace(活泼的快板)";
        } else if (bpm <= 177) {
            return "Presto(急板)";
        } else {
            return "Prestissimo(最急板)";
        }
    }


}
