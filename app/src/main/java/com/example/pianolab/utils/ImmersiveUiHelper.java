package com.example.pianolab.utils;

import android.graphics.Color;
import android.view.Window;

import androidx.annotation.NonNull;
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
}
