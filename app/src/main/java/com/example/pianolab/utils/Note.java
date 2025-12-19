package com.example.pianolab.utils;

public class Note {
    private final int abs_idx;
    private final String name;
    private final String alias;
    private final String sample_path;

    public int getAbs_idx() {
        return abs_idx;
    }

    public String getAlias() {
        return alias;
    }

    public String getName() {
        return name;
    }

    public String getSample_path() {
        return sample_path;
    }



    public Note(int abs_idx, String alias, String name, String sample_path) {
        this.abs_idx = abs_idx;
        this.alias = alias;
        this.name = name;
        this.sample_path = sample_path;
    }

}
