package com.example.pianolab.feature.home.model;

import androidx.annotation.ColorRes;
import androidx.annotation.DrawableRes;
import androidx.annotation.StringRes;

public class FeatureItem {
  private final int iconResId;
  @StringRes private final int nameResId;
  @StringRes private final int descriptionResId;
  @DrawableRes private final int cardBackgroundResId;
  @DrawableRes private final int iconCircleBackgroundResId;
  @ColorRes private final int iconTintColorResId;
  private final Class<?> targetCls;

  public FeatureItem(
      int iconResId,
      @StringRes int nameResId,
      @StringRes int descriptionResId,
      @DrawableRes int cardBackgroundResId,
      @DrawableRes int iconCircleBackgroundResId,
      @ColorRes int iconTintColorResId,
      Class<?> targetCls) {
    this.iconResId = iconResId;
    this.nameResId = nameResId;
    this.descriptionResId = descriptionResId;
    this.cardBackgroundResId = cardBackgroundResId;
    this.iconCircleBackgroundResId = iconCircleBackgroundResId;
    this.iconTintColorResId = iconTintColorResId;
    this.targetCls = targetCls;
  }

  public int getIconResId() {
    return iconResId;
  }

  @StringRes
  public int getNameResId() {
    return nameResId;
  }

  @StringRes
  public int getDescriptionResId() {
    return descriptionResId;
  }

  @DrawableRes
  public int getCardBackgroundResId() {
    return cardBackgroundResId;
  }

  @DrawableRes
  public int getIconCircleBackgroundResId() {
    return iconCircleBackgroundResId;
  }

  @ColorRes
  public int getIconTintColorResId() {
    return iconTintColorResId;
  }

  public Class<?> getTargetCls() {
    return targetCls;
  }
}
