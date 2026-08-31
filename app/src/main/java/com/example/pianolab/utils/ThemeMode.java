package com.example.pianolab.utils;

import androidx.appcompat.app.AppCompatDelegate;

/** 应用外观模式：浅色、深色、跟随系统。 */
public enum ThemeMode {
  LIGHT(AppCompatDelegate.MODE_NIGHT_NO),
  DARK(AppCompatDelegate.MODE_NIGHT_YES),
  SYSTEM(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);

  private final int nightMode;

  ThemeMode(int nightMode) {
    this.nightMode = nightMode;
  }

  int getNightMode() {
    return nightMode;
  }

  public static ThemeMode fromName(String name) {
    if (name == null) {
      return SYSTEM;
    }
    try {
      return ThemeMode.valueOf(name);
    } catch (IllegalArgumentException ignored) {
      return SYSTEM;
    }
  }
}
