package com.example.pianolab.utils;

import android.content.Context;
import android.util.TypedValue;
import androidx.annotation.AttrRes;
import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;

/** 从当前主题解析颜色属性，避免在代码中硬编码浅色/深色 token。 */
public final class ThemeColors {

  private ThemeColors() {}

  @ColorInt
  public static int get(@NonNull Context context, @AttrRes int attr) {
    TypedValue typedValue = new TypedValue();
    context.getTheme().resolveAttribute(attr, typedValue, true);
    return typedValue.data;
  }
}
