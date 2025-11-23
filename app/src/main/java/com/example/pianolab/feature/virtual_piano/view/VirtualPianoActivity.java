package com.example.pianolab.feature.virtual_piano.view;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import com.example.pianolab.R;
import java.io.InputStream;

/**
 * 简化的虚拟钢琴Activity（横屏，仅显示键盘图片并响应触摸高亮），暂不发声
 */
public class VirtualPianoActivity extends AppCompatActivity {
    private ImageView ivKeys;
    private View overlay;
    private Bitmap keysBitmap;
    private Bitmap mutableBitmap;
    private Paint paint;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 仅在此 Activity 去掉顶部 ActionBar（不改全局主题）
        supportRequestWindowFeature(android.view.Window.FEATURE_NO_TITLE);
        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        // 布局在请求无标题之后设置
        setContentView(R.layout.activity_virtual_piano);

        // 强制横屏显示
        setRequestedOrientation(android.content.pm.ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE);

        ivKeys = findViewById(R.id.iv_keys);
        overlay = findViewById(R.id.key_overlay);

        // 从raw中加载keys.png，使用按需缩放以避免解码失败/OOM
        keysBitmap = loadScaledBitmapFromRaw(R.raw.keys);
        if (keysBitmap != null) {
            // 创建可修改副本用于绘制高亮
            mutableBitmap = keysBitmap.copy(Bitmap.Config.ARGB_8888, true);
            ivKeys.setImageBitmap(mutableBitmap);
        }

        paint = new Paint();
        paint.setColor(Color.argb(120, 255, 255, 0)); // 半透明黄色高亮

        // 处理触摸事件：在overlay上捕获触摸并在对应位置绘制高亮
        overlay.setOnTouchListener((v, event) -> {
            switch (event.getActionMasked()) {
                case MotionEvent.ACTION_DOWN:
                case MotionEvent.ACTION_MOVE:
                    if (mutableBitmap != null && ivKeys.getWidth() > 0 && ivKeys.getHeight() > 0) {
                        handleTouch(event.getX(), event.getY());
                    }
                    return true;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    // 恢复原图
                    restoreOriginalImage();
                    // 触发无障碍点击反馈
                    v.performClick();
                    return true;
            }
            return false;
        });
    }

    /**
     * 从 raw 目录按屏幕分辨率缩放加载 Bitmap，避免大图直接加载导致内存问题
     */
    private Bitmap loadScaledBitmapFromRaw(int resId) {
        InputStream is = null;
        try {
            // 先解 bounds
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            is = getResources().openRawResource(resId);
            BitmapFactory.decodeStream(is, null, options);
            try { is.close(); } catch (Exception ignored) {}

            int reqW = getResources().getDisplayMetrics().widthPixels;
            int reqH = getResources().getDisplayMetrics().heightPixels;

            options.inSampleSize = calculateInSampleSize(options, reqW, reqH);
            options.inJustDecodeBounds = false;
            options.inPreferredConfig = Bitmap.Config.ARGB_8888;

            // 再次打开流进行实际解码
            is = getResources().openRawResource(resId);
            return BitmapFactory.decodeStream(is, null, options);
        } catch (Exception e) {
            // 解码失败返回 null（调用者需处理）
            return null;
        } finally {
            try { if (is != null) is.close(); } catch (Exception ignored) {}
        }
    }

    /**
     * 计算合适的 inSampleSize，保证解码后图片至少能够覆盖请求的宽高
     */
    private int calculateInSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight) {
        int height = options.outHeight;
        int width = options.outWidth;
        int inSampleSize = 1;
        if (height > reqHeight || width > reqWidth) {
            final int halfHeight = height / 2;
            final int halfWidth = width / 2;
            // 使用2的幂次方缩放
            while ((halfHeight / inSampleSize) >= reqHeight && (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2;
            }
        }
        return inSampleSize;
    }

    private void handleTouch(float x, float y) {
        // 在可修改bitmap上绘制一个圆形高亮（作为按键反馈）
        // 需要将触摸坐标从View空间映射到bitmap空间

        float scaleX = (float) mutableBitmap.getWidth() / (float) ivKeys.getWidth();
        float scaleY = (float) mutableBitmap.getHeight() / (float) ivKeys.getHeight();

        float bmpX = x * scaleX;
        float bmpY = y * scaleY;

        // 先 restore 原图，然后绘制高亮
        restoreOriginalImage();

        Canvas canvas = new Canvas(mutableBitmap);
        float radius = 40 * ((scaleX + scaleY) / 2f);
        canvas.drawCircle(bmpX, bmpY, radius, paint);

        runOnUiThread(() -> ivKeys.setImageBitmap(mutableBitmap));
    }

    private void restoreOriginalImage() {
        if (keysBitmap != null && mutableBitmap != null) {
            Canvas canvas = new Canvas(mutableBitmap);
            canvas.drawBitmap(keysBitmap, 0, 0, null);
            runOnUiThread(() -> ivKeys.setImageBitmap(mutableBitmap));
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (keysBitmap != null) keysBitmap.recycle();
        if (mutableBitmap != null) mutableBitmap.recycle();
    }
}
