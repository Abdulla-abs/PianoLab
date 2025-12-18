package com.example.pianolab.feature.chord.view;

import android.os.Bundle;
import android.view.ViewTreeObserver;
import android.widget.HorizontalScrollView;
import android.widget.ImageButton;
import android.widget.SeekBar;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import com.example.pianolab.R;
import com.example.pianolab.feature.chord.viewmodel.ChordViewModel;
import com.google.android.material.switchmaterial.SwitchMaterial;

public class ChordActivity extends AppCompatActivity {
    private SeekBar seekBar;
    private HorizontalScrollView hsPiano;
    private ChordPianoView pianoView;
    private ImageButton buttonBack;
    private ImageButton buttonReset;
    private ImageButton buttonBackout;
    private StaffView staffGClef;
    private StaffView staffFClef;
    private TextView tvChordName;
    private ChordViewModel viewModel;
    private SwitchMaterial switchAccidentalMode;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chord);

        viewModel = new ViewModelProvider(this).get(ChordViewModel.class);

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
        staffGClef = findViewById(R.id.staff_g_clef);
        staffFClef = findViewById(R.id.staff_f_clef);
        tvChordName = findViewById(R.id.tv_chord_name);
        switchAccidentalMode = findViewById(R.id.switch_accidental_mode);

        staffGClef.setClefType(StaffView.ClefType.TREBLE);
        staffFClef.setClefType(StaffView.ClefType.BASS);

        switchAccidentalMode.setOnCheckedChangeListener((buttonView, isChecked) -> {
            // isChecked = true -> use flats (降号)
            // isChecked = false -> use sharps (升号)
            staffGClef.setUseFlats(isChecked);
            staffFClef.setUseFlats(isChecked);
        });

        buttonBack.setOnClickListener(v -> finish());
        buttonReset.setOnClickListener(v -> viewModel.reset());
        buttonBackout.setOnClickListener(v -> viewModel.backout());

        pianoView.setOnKeyToggledListener(key -> viewModel.toggleKey(key));

        viewModel.selectedKeys.observe(this, keys -> {
            pianoView.setSelectedKeys(keys);
            staffGClef.setNotes(keys);
            staffFClef.setNotes(keys);
        });

        viewModel.chordText.observe(this, text -> {
            tvChordName.setText(text);
        });

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
