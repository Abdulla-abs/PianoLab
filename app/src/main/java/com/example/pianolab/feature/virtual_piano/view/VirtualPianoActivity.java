// java
package com.example.pianolab.feature.virtual_piano.view;

import android.os.Bundle;
import android.util.Log;
import android.view.ViewTreeObserver;
import android.widget.HorizontalScrollView;
import android.widget.SeekBar;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.pianolab.R;

/**
 * 横屏 Activity：显示可滑动的 88 键钢琴（用滑块同步滚动）
 */
public class VirtualPianoActivity extends AppCompatActivity {
    private static final String TAG = "VirtualPianoActivity";
    private SeekBar seekBar;
    private HorizontalScrollView hsPiano;
    private PianoView pianoView;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        supportRequestWindowFeature(android.view.Window.FEATURE_NO_TITLE);
        if (getSupportActionBar() != null) getSupportActionBar().hide();
        setRequestedOrientation(android.content.pm.ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE);

        setContentView(R.layout.activity_virtual_piano);

        seekBar = findViewById(R.id.seekBar);
        hsPiano = findViewById(R.id.hs_piano);
        pianoView = findViewById(R.id.piano_view);

        hsPiano.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                try {
                    hsPiano.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                    int contentW = pianoView.getContentWidth(); // 修正：使用内容宽度
                    int visibleW = hsPiano.getWidth();
                    int maxScroll = Math.max(0, contentW - visibleW);
                    seekBar.setMax(maxScroll);

                    seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                        @Override public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                            if (fromUser) {
                                hsPiano.scrollTo(progress, 0);
                            }
                        }
                        @Override public void onStartTrackingTouch(SeekBar seekBar) { }
                        @Override public void onStopTrackingTouch(SeekBar seekBar) { }
                    });

                    hsPiano.setOnScrollChangeListener((v, scrollX, scrollY, oldScrollX, oldScrollY) -> {
                        seekBar.setProgress(scrollX);
                    });
                } catch (Exception e) {
                    Log.e(TAG, "onGlobalLayout error", e);
                }
            }
        });
    }
}
