// java
package com.example.pianolab.feature.virtual_piano.view;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.HorizontalScrollView;
import android.widget.ImageButton;
import android.widget.SeekBar;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.example.pianolab.R;
import com.example.pianolab.feature.virtual_piano.viewmodel.VirtualPianoViewModel;
import com.google.android.material.switchmaterial.SwitchMaterial;

/**
 * 横屏 Activity：显示可滑动的 88 键钢琴（用滑块同步滚动）
 */
public class VirtualPianoActivity extends AppCompatActivity {
    private static final String TAG = "VirtualPianoActivity";
    private SeekBar seekBar;
    private HorizontalScrollView hsPiano;
    private PianoView pianoView;
    private VirtualPianoViewModel viewModel;
    private ImageButton buttonBack;
    private SwitchMaterial switchShowNote;
    private SwitchMaterial switchSustain;
    private static final String DEFAULT_CENTER_KEY = "key45_white";

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
        buttonBack = findViewById(R.id.button_back);
        buttonBack.setOnClickListener(v -> finish());
        viewModel = new ViewModelProvider(this).get(VirtualPianoViewModel.class);

        switchShowNote = findViewById(R.id.switch_show_note);
        switchShowNote.setOnCheckedChangeListener((buttonView, isChecked) -> {
            Boolean current = viewModel.getShowNoteNames().getValue();
            if (current != null && current == isChecked) return;
            viewModel.setShowNoteNames(isChecked);
        });

        switchSustain = findViewById(R.id.switch_sustain);
        switchSustain.setOnCheckedChangeListener((buttonView, isChecked) -> {
            Boolean current = viewModel.getSustainEnabled().getValue();
            if (current != null && current == isChecked) return;
            viewModel.setSustainEnabled(isChecked);
        });

        viewModel.getShowNoteNames().observe(this, show -> {
           boolean showNames = show == null || show;
            if (switchShowNote.isChecked() != showNames) {
                switchShowNote.setChecked(showNames);
            }
            pianoView.setShowPitchNames(showNames);
        });

        viewModel.getSustainEnabled().observe(this, enabled -> {
            boolean sustain = enabled != null && enabled;
            if (switchSustain.isChecked() != sustain) {
                switchSustain.setChecked(sustain);
            }
            // 具体延音逻辑将来添加
        });

        hsPiano.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                try {
                    hsPiano.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                    int contentW = pianoView.getContentWidth();
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

                    int centerX = pianoView.getKeyCenterX(DEFAULT_CENTER_KEY);
                    int initialScroll = Math.max(0, centerX - visibleW / 2);
                    initialScroll = Math.min(initialScroll, maxScroll);
                    hsPiano.scrollTo(initialScroll, 0);
                    seekBar.setProgress(initialScroll);
                } catch (Exception e) {
                    Log.e(TAG, "onGlobalLayout error", e);
                }

                pianoView.post(() -> {
                    try {
                        pianoView.debugValidateInitKeyHitTest();
                    } catch (Exception e) {
                        Log.e(TAG, "debugValidateInitKeyHitTest error", e);
                    }
                });
            }
        });
    }
}
