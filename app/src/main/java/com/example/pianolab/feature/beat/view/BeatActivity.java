package com.example.pianolab.feature.beat.view;

import android.os.Bundle;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.ToggleButton;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.example.pianolab.R;
import com.example.pianolab.feature.beat.viewmodel.BeatViewModel;


public class BeatActivity extends AppCompatActivity {

    private BeatViewModel viewModel;

    private TextView tvBpmValue;
    private SeekBar seekbarBpm;
    private TextView tvBeatsValue;
    private Button btnDecreaseBeats;
    private Button btnIncreaseBeats;
    private ToggleButton togglePlay;
    private TextView tvCurrentBeat;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_beat);

        viewModel = new ViewModelProvider(this).get(BeatViewModel.class);

        tvBpmValue = findViewById(R.id.tv_bpm_value);
        seekbarBpm = findViewById(R.id.seekbar_bpm);
        tvBeatsValue = findViewById(R.id.tv_beats_value);
        btnDecreaseBeats = findViewById(R.id.btn_decrease_beats);
        btnIncreaseBeats = findViewById(R.id.btn_increase_beats);
        togglePlay = findViewById(R.id.toggle_play);
        tvCurrentBeat = findViewById(R.id.tv_current_beat);

        // 初始化 SeekBar 范围映射：progress 0..270 -> BPM 30..300
        seekbarBpm.setMax(270);

        // 观察 BPM
        viewModel.getBpm().observe(this, value -> {
            if (value == null) return;
            tvBpmValue.setText(String.valueOf(value));
            int progress = Math.max(0, Math.min(270, value - 30));
            if (seekbarBpm.getProgress() != progress) {
                seekbarBpm.setProgress(progress);
            }
        });

        // 观察当前拍
        viewModel.getCurrentBeatIndex().observe(this, idx -> {
            if (idx == null) return;
            tvCurrentBeat.setText(getString(R.string.label_current_beat, idx + 1));
        });

        // 观察运行状态
        viewModel.getIsRunning().observe(this, running -> {
            if (running == null) return;
            togglePlay.setChecked(running);
        });

        // SeekBar 交互
        seekbarBpm.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (!fromUser) return;
                int newBpm = progress + 30;
                viewModel.setBpm(newBpm);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) { }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) { }
        });

        // 拍数增减
        btnDecreaseBeats.setOnClickListener(v -> {
            try {
                int cur = Integer.parseInt(tvBeatsValue.getText().toString());
                int next = Math.max(1, cur - 1);
                viewModel.setBeatsPerMeasure(next);
                tvBeatsValue.setText(String.valueOf(next));
            } catch (Exception ignored) { }
        });

        btnIncreaseBeats.setOnClickListener(v -> {
            try {
                int cur = Integer.parseInt(tvBeatsValue.getText().toString());
                int next = Math.min(16, cur + 1);
                viewModel.setBeatsPerMeasure(next);
                tvBeatsValue.setText(String.valueOf(next));
            } catch (Exception ignored) { }
        });

        // 开始/停止
        togglePlay.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) viewModel.start(); else viewModel.stop();
        });

        // 将初始 UI 与 ViewModel 对齐
        Integer initBpm = viewModel.getBpm().getValue();
        if (initBpm != null) tvBpmValue.setText(String.valueOf(initBpm));
        // beats 初始值来自布局的 tv_beats_value（默认为 4），同步到 viewModel
        try {
            int beats = Integer.parseInt(tvBeatsValue.getText().toString());
            viewModel.setBeatsPerMeasure(beats);
        } catch (Exception ignored) { }
    }
}
