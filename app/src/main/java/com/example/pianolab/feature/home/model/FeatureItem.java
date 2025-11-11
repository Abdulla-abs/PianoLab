package com.example.pianolab.feature.home.model;

public class FeatureItem {
    private String name;       // 功能名称（如“节拍器”）
    private int iconResId;     // 功能图标资源ID
    private Class<?> targetCls; // 跳转的目标Activity

    public FeatureItem(int iconResId, String name, Class<?> targetCls) {
        this.iconResId = iconResId;
        this.name = name;
        this.targetCls = targetCls;
    }

    public int getIconResId() {
        return iconResId;
    }

    public String getName() {
        return name;
    }

    public Class<?> getTargetCls() {
        return targetCls;
    }
}
