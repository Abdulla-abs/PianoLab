package com.example.pianolab.feature.chord.view;

import android.os.Bundle;
import android.view.ViewTreeObserver;
import android.widget.HorizontalScrollView;
import android.widget.ImageButton;
import android.widget.SeekBar;
import androidx.appcompat.app.AppCompatActivity;
import com.example.pianolab.R;

public class ChordActivity extends AppCompatActivity {
    private SeekBar seekBar;
    private HorizontalScrollView hsPiano;
    private ChordPianoView pianoView;
    private ImageButton buttonBack;
    private ImageButton buttonReset;
    private ImageButton buttonBackout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chord);

        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        seekBar = findViewById(R.id.seekBar);
        // The HorizontalScrollView in activity_chord.xml is actually a NonScrollableHorizontalScrollView
        // but we can cast it to HorizontalScrollView or use findViewById with the correct type if we had access to it.
        // Since NonScrollableHorizontalScrollView extends HorizontalScrollView, this is fine.
        hsPiano = findViewById(R.id.hs_piano);
        pianoView = findViewById(R.id.piano_view);
        buttonBack = findViewById(R.id.button_back);
        buttonReset = findViewById(R.id.button_reset);
        buttonBackout = findViewById(R.id.button_backout);

        buttonBack.setOnClickListener(v -> finish());
        buttonReset.setOnClickListener(v -> pianoView.reset());
        buttonBackout.setOnClickListener(v -> pianoView.backout());

        hsPiano.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
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

                // Center on C4 (key 40)
                // C2 is key 16. C4 is key 40.
                // key40_white
                int centerX = pianoView.getKeyCenterX("key40_white");
                int initialScroll = Math.max(0, centerX - visibleW / 2);
                initialScroll = Math.min(initialScroll, maxScroll);
                hsPiano.scrollTo(initialScroll, 0);
                seekBar.setProgress(initialScroll);
            }
        });
    }
}
