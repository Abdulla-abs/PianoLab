package com.example.pianolab.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import androidx.appcompat.app.AppCompatDelegate;

/** 统一管理应用外观模式，持久化用户选择并应用到全局夜间模式。 */
public final class ThemeManager {

  private static final String PREFS_NAME = "theme_prefs";
  private static final String KEY_THEME_MODE = "theme_mode";

  private ThemeManager() {}

  public static void init(Context context) {
    AppCompatDelegate.setDefaultNightMode(getThemeMode(context).getNightMode());
  }

  public static ThemeMode getThemeMode(Context context) {
    SharedPreferences prefs = prefs(context);
    return ThemeMode.fromName(prefs.getString(KEY_THEME_MODE, ThemeMode.SYSTEM.name()));
  }

  public static void setThemeMode(Context context, ThemeMode mode) {
    if (mode == null) {
      mode = ThemeMode.SYSTEM;
    }
    prefs(context).edit().putString(KEY_THEME_MODE, mode.name()).apply();
    AppCompatDelegate.setDefaultNightMode(mode.getNightMode());
  }

  public static boolean isDarkTheme(Context context) {
    ThemeMode mode = getThemeMode(context);
    if (mode == ThemeMode.DARK) {
      return true;
    }
    if (mode == ThemeMode.LIGHT) {
      return false;
    }
    int nightModeFlags =
        context.getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
    return nightModeFlags == Configuration.UI_MODE_NIGHT_YES;
  }

  private static SharedPreferences prefs(Context context) {
    return context.getApplicationContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
  }
}
