package com.example.pianolab.utils;

import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Color;
import android.view.View;
import android.view.Window;

import androidx.annotation.NonNull;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;

/**
 * 沉浸式全屏：内容铺满屏幕，状态栏默认隐藏；从顶部下滑时以浮层临时显示，不触发布局变化。
 */
public final class ImmersiveUiHelper {

    private ImmersiveUiHelper() {
    }

    public static void enableImmersiveMode(@NonNull Window window) {
        WindowCompat.setDecorFitsSystemWindows(window, false);
        window.setStatusBarColor(Color.TRANSPARENT);
        window.setNavigationBarColor(Color.TRANSPARENT);
        applyImmersiveSystemUi(window);
    }

    /**
     * 标准系统栏：始终显示状态栏/导航栏。targetSdk 35+ 强制 edge-to-edge，
     * 需配合 {@link #applySystemBarInsets(View)} 为内容区预留安全间距。
     */
    public static void enableStandardSystemBars(@NonNull Window window) {
        WindowCompat.setDecorFitsSystemWindows(window, false);
        WindowInsetsControllerCompat controller =
                WindowCompat.getInsetsController(window, window.getDecorView());
        if (controller == null) {
            return;
        }
        boolean lightBars = !isDarkTheme(window.getContext());
        controller.setAppearanceLightStatusBars(lightBars);
        controller.setAppearanceLightNavigationBars(lightBars);
        controller.show(WindowInsetsCompat.Type.statusBars());
        controller.show(WindowInsetsCompat.Type.navigationBars());
    }

    /** 为内容根布局应用系统栏 insets，避免工具栏被状态栏遮挡。 */
    public static void applySystemBarInsets(@NonNull View view) {
        final int initialLeft = view.getPaddingLeft();
        final int initialTop = view.getPaddingTop();
        final int initialRight = view.getPaddingRight();
        final int initialBottom = view.getPaddingBottom();

        ViewCompat.setOnApplyWindowInsetsListener(
                view,
                (v, windowInsets) -> {
                    Insets insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars());
                    v.setPadding(
                            initialLeft + insets.left,
                            initialTop + insets.top,
                            initialRight + insets.right,
                            initialBottom + insets.bottom);
                    return windowInsets;
                });
        ViewCompat.requestApplyInsets(view);
    }

    /**
     * 阻止系统栏 insets 向下传递，避免内容随状态栏显隐产生 padding 或布局抖动。
     * 配合 {@link #enableImmersiveMode(Window)} 与 {@link #applyImmersiveSystemUi(Window)} 使用。
     */
    public static void suppressSystemBarInsets(@NonNull View view) {
        ViewCompat.setOnApplyWindowInsetsListener(
                view, (v, windowInsets) -> WindowInsetsCompat.CONSUMED);
    }

    /**
     * 在 {@link android.app.Activity#onWindowFocusChanged(boolean)} 中调用，确保从系统栏返回后仍保持沉浸。
     */
    public static void applyImmersiveSystemUi(@NonNull Window window) {
        WindowInsetsControllerCompat controller =
                WindowCompat.getInsetsController(window, window.getDecorView());
        if (controller == null) {
            return;
        }
        controller.setAppearanceLightStatusBars(false);
        controller.setSystemBarsBehavior(
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
        controller.hide(WindowInsetsCompat.Type.statusBars());
    }

    public static boolean isDarkTheme(@NonNull Context context) {
        int nightModeFlags =
                context.getResources().getConfiguration().uiMode
                        & Configuration.UI_MODE_NIGHT_MASK;
        return nightModeFlags == Configuration.UI_MODE_NIGHT_YES;
    }
}
