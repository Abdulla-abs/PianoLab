package com.example.pianolab.utils;

import java.sql.Struct;

public class MusicTheoryHelper {

    public static final Note[] ALL_PIANO_NOTES = {
            //占位符
            new Note(0,"none","none","none"),
            // 第1-13键：A0 - A1
            new Note(1, "A0", "none", "k021"),
            new Note(2, "#A0", "bB0", "k022"),
            new Note(3, "B0", "none", "k023"),
            new Note(4, "C1", "none", "k024"),
            new Note(5, "#C1", "bD1", "k025"),
            new Note(6, "D1", "none", "k026"),
            new Note(7, "#D1", "bE1", "k027"),
            new Note(8, "E1", "none", "k028"),
            new Note(9, "F1", "none", "k029"),
            new Note(10, "#F1", "bG1", "k030"),
            new Note(11, "G1", "none", "k031"),
            new Note(12, "#G1", "bA1", "k032"),
            new Note(13, "A1", "none", "k033"),

            // 第14-25键：#A1 - A2
            new Note(14, "#A1", "bB1", "k034"),
            new Note(15, "B1", "none", "k035"),
            new Note(16, "C2", "none", "k036"),
            new Note(17, "#C2", "bD2", "k037"),
            new Note(18, "D2", "none", "k038"),
            new Note(19, "#D2", "bE2", "k039"),
            new Note(20, "E2", "none", "k040"),
            new Note(21, "F2", "none", "k041"),
            new Note(22, "#F2", "bG2", "k042"),
            new Note(23, "G2", "none", "k043"),
            new Note(24, "#G2", "bA2", "k044"),
            new Note(25, "A2", "none", "k045"),

            // 第26-37键：#A2 - A3
            new Note(26, "#A2", "bB2", "k046"),
            new Note(27, "B2", "none", "k047"),
            new Note(28, "C3", "none", "k048"),
            new Note(29, "#C3", "bD3", "k049"),
            new Note(30, "D3", "none", "k050"),
            new Note(31, "#D3", "bE3", "k051"),
            new Note(32, "E3", "none", "k052"),
            new Note(33, "F3", "none", "k053"),
            new Note(34, "#F3", "bG3", "k054"),
            new Note(35, "G3", "none", "k055"),
            new Note(36, "#G3", "bA3", "k056"),
            new Note(37, "A3", "none", "k057"),

            // 第38-49键：#A3 - A4（标准音A4，440Hz）
            new Note(38, "#A3", "bB3", "k058"),
            new Note(39, "B3", "none", "k059"),
            new Note(40, "C4", "none", "k060"), // 中央C
            new Note(41, "#C4", "bD4", "k061"),
            new Note(42, "D4", "none", "k062"),
            new Note(43, "#D4", "bE4", "k063"),
            new Note(44, "E4", "none", "k064"),
            new Note(45, "F4", "none", "k065"),
            new Note(46, "#F4", "bG4", "k066"),
            new Note(47, "G4", "none", "k067"),
            new Note(48, "#G4", "bA4", "k068"),
            new Note(49, "A4", "none", "k069"), // 标准音

            // 第50-61键：#A4 - A5
            new Note(50, "#A4", "bB4", "k070"),
            new Note(51, "B4", "none", "k071"),
            new Note(52, "C5", "none", "k072"),
            new Note(53, "#C5", "bD5", "k073"),
            new Note(54, "D5", "none", "k074"),
            new Note(55, "#D5", "bE5", "k075"),
            new Note(56, "E5", "none", "k076"),
            new Note(57, "F5", "none", "k077"),
            new Note(58, "#F5", "bG5", "k078"),
            new Note(59, "G5", "none", "k079"),
            new Note(60, "#G5", "bA5", "k080"),
            new Note(61, "A5", "none", "k081"),

            // 第62-73键：#A5 - A6
            new Note(62, "#A5", "bB5", "k082"),
            new Note(63, "B5", "none", "k083"),
            new Note(64, "C6", "none", "k084"),
            new Note(65, "#C6", "bD6", "k085"),
            new Note(66, "D6", "none", "k086"),
            new Note(67, "#D6", "bE6", "k087"),
            new Note(68, "E6", "none", "k088"),
            new Note(69, "F6", "none", "k089"),
            new Note(70, "#F6", "bG6", "k090"),
            new Note(71, "G6", "none", "k091"),
            new Note(72, "#G6", "bA6", "k092"),
            new Note(73, "A6", "none", "k093"),

            // 第74-85键：#A6 - A7
            new Note(74, "#A6", "bB6", "k094"),
            new Note(75, "B6", "none", "k095"),
            new Note(76, "C7", "none", "k096"),
            new Note(77, "#C7", "bD7", "k097"),
            new Note(78, "D7", "none", "k098"),
            new Note(79, "#D7", "bE7", "k099"),
            new Note(80, "E7", "none", "k100"),
            new Note(81, "F7", "none", "k101"),
            new Note(82, "#F7", "bG7", "k102"),
            new Note(83, "G7", "none", "k103"),
            new Note(84, "#G7", "bA7", "k104"),
            new Note(85, "A7", "none", "k105"),

            // 第86-88键：#A7 - C8（最高音）
            new Note(86, "#A7", "bB7", "k106"),
            new Note(87, "B7", "none", "k107"),
            new Note(88, "C8", "none", "k108")
    };




    public static String[] SIMPLE_NOTE_NAME = {"C", "D", "E", "F", "G", "A", "B"};
    public static String[] CHORD_NAME = {
            "Maj",
            "min",
            "dim",
            "aug",
            "sus2",
            "sus4",
            "Maj7",
            "7",
            "m7",
            "mM7",
            "m7b5",
            "dim7",
            "aug7",
            "augM7",
            "6",
            "m6"
    };

    public static String[] SIMPLE_NOTE_NAME_WITH_SHARP = {
             "C","#C", "D","#D","E","F",
            "#F", "G","#G","A", "#A","B"

    };

    public static String[] SIMPLE_NOTE_NAME_WITH_FLAT = {
            "C",
            "bD","D","bE","E","F","bG","G",
            "bA","A","bB","B"
    };



}

