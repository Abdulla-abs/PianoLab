package com.example.pianolab.feature.beat.view;

import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.ToggleButton;
import android.widget.EditText;
import android.text.InputFilter;
import android.text.InputType;
import android.text.method.DigitsKeyListener;
import android.widget.ImageView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.example.pianolab.R;
import com.example.pianolab.feature.beat.viewmodel.BeatViewModel;
import com.example.pianolab.feature.beat.model.BeatSettings;
import com.example.pianolab.utils.BeatHelper;


public class BeatActivity extends AppCompatActivity {

    private BeatViewModel viewModel;

    private TextView tvBpmValue;
    private TextView tvCrochetValue; // 新增：四分音符数值显示
    private SeekBar seekbarBpm;
    private TextView tvBeatsValue;
    private ToggleButton togglePlay;

    private Button btnBack; // 返回按钮

    // 新增：可视化控件
    private RadialPulseView radialPulseView;
    

    // 倒计时音效与计时器
    private MediaPlayer countdownPlayer;
    private CountDownTimer countdownTimer;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_beat);

        viewModel = new ViewModelProvider(this).get(BeatViewModel.class);

        // 绑定视图
        radialPulseView = findViewById(R.id.radial_pulse_view);
        // 将中心文本字体设置为更大（超参数，方便调整）
        radialPulseView.setCenterTextSizeFactor(0.7f);
        // 初始化倒计时音效（确保 res/raw/countdown.wav 存在）
        try {
            countdownPlayer = MediaPlayer.create(this, R.raw.countdown);
        } catch (Exception ignored) { countdownPlayer = null; }
        btnBack = findViewById(R.id.btn_back);
        tvBpmValue = findViewById(R.id.tv_bpm_value);
        tvCrochetValue = findViewById(R.id.tv_crochet_value);
        // 新绑定 base-beat 图标

        seekbarBpm = findViewById(R.id.seekbar_bpm);
        tvBeatsValue = findViewById(R.id.tv_beats_value);
        togglePlay = findViewById(R.id.toggle_play);

        // 返回按钮行为：结束当前 Activity，返回上一个（Home）
        btnBack.setOnClickListener(v -> finish());

        // 初始化 SeekBar 范围映射：progress 0..270 -> BPM 30..300
        seekbarBpm.setMax(270);

        // 观察 BPM
        viewModel.getBpm().observe(this, value -> {
            if (value == null) return;

            Integer base = viewModel.getBaseBeat().getValue();
            if (base == null) base = 4;

            tvBpmValue.setText(String.valueOf(value));

            int cpm = BeatHelper.BPM_TO_CPM(value,base);
            tvCrochetValue.setText(String.valueOf(cpm));
            int progress = Math.max(0, Math.min(270, value - 30));
            if (seekbarBpm.getProgress() != progress) {
                seekbarBpm.setProgress(progress);
            }
        });

        // 观察 baseBeat，
        viewModel.getBaseBeat().observe(this, b -> {
            if (b == null) return;
            // 更新显示的 bpm 标记：触发一次对 bpm 的重新计算（如果已有 bpm 会被 observer更新）
            Integer curBpm = viewModel.getBpm().getValue();
            if (curBpm != null) {
                int curCpm = BeatHelper.BPM_TO_CPM(curBpm,b);
                tvBpmValue.setText(String.valueOf(curBpm));
                tvCrochetValue.setText(String.valueOf(curCpm));
            }
        });

        // 点击 bpm 文本可以手动输入 BPM
        tvBpmValue.setClickable(true);
        tvBpmValue.setOnClickListener(v -> {
            EditText et = new EditText(this);
            et.setInputType(InputType.TYPE_CLASS_NUMBER);
            et.setKeyListener(DigitsKeyListener.getInstance("0123456789"));
            et.setFilters(new InputFilter[]{new InputFilter.LengthFilter(4)});

            Integer curQuarterBpm = viewModel.getBpm().getValue();
            Integer base = viewModel.getBaseBeat().getValue();
            if (base == null) base = 4;
            int prefill = curQuarterBpm != null ? curQuarterBpm : 120;
            et.setText(String.valueOf(prefill));

            new androidx.appcompat.app.AlertDialog.Builder(this)
                    .setTitle("设置BPM(每分钟X拍)")
                    .setView(et)
                    .setPositiveButton("确定", (dialog, which) -> {
                        try {
                            String s = et.getText().toString().trim();
                            if (s.isEmpty()) return;
                            int ori_bpm = Integer.parseInt(s);
                            int bpmVal = BeatHelper.clampBPM(ori_bpm);
                            viewModel.setBpm(bpmVal);
                        } catch (Exception ignored) { }
                    })
                    .setNegativeButton("取消", null)
                    .show();
        });

        tvCrochetValue.setClickable(true);
        tvCrochetValue.setOnClickListener(v -> {
            EditText et = new EditText(this);
            et.setInputType(InputType.TYPE_CLASS_NUMBER);
            et.setKeyListener(DigitsKeyListener.getInstance("0123456789"));
            et.setFilters(new InputFilter[]{new InputFilter.LengthFilter(4)});

            Integer curQuarterBpm = viewModel.getBpm().getValue();
            Integer base = viewModel.getBaseBeat().getValue();
            if (base == null) base = 4;
            int pre = curQuarterBpm != null ? BeatHelper.BPM_TO_CPM(curQuarterBpm,base) : BeatHelper.BPM_TO_CPM(120,base);
            et.setText(String.valueOf(pre));

            new androidx.appcompat.app.AlertDialog.Builder(this)
                    .setTitle("设置四分音符每分钟 (♪ = ?)")
                    .setView(et)
                    .setPositiveButton("确定", (dialog, which) -> {
                        try {
                            String s = et.getText().toString().trim();
                            if (s.isEmpty()) return;
                            int q = Integer.parseInt(s);
                            int bpm = BeatHelper.CPM_TO_BPM(q,viewModel.getBaseBeat().getValue());
                            bpm = BeatHelper.clampBPM(bpm);
                            viewModel.setBpm(bpm);
                        } catch (Exception ignored) { }
                    })
                    .setNegativeButton("取消", null)
                    .show();
        });


        // 观察当前拍
        viewModel.getCurrentBeatIndex().observe(this, idx -> {
            if (idx == null) return;

            // 修复：避免在首次注册 observer（Activity 进入时）触发一次不必要的脉冲。
            // 只有在引擎实际运行时才触发可视化脉冲与停止倒计时动作。
            Boolean engineRunningNow = viewModel.getEngineRunning().getValue();
            if (engineRunningNow != null && engineRunningNow) {
                // 触发可视化脉冲与更新中心文本：使用 post 确保在 UI 线程执行
                if (radialPulseView != null) {
                    radialPulseView.post(() -> {
                        radialPulseView.pulse(idx == 0);
                        radialPulseView.setCenterText(String.valueOf(idx + 1));
                        // 停止倒计时（防止 race）
                        if (countdownTimer != null) {
                            countdownTimer.cancel();
                            countdownTimer = null;
                        }
                        radialPulseView.stopCountdown();
                    });
                }
            } else {
                // 引擎未运行时仅更新 UI 文本（例如进入页面或处于倒计时阶段），但不触发脉冲
                if (radialPulseView != null) {
                    radialPulseView.post(() -> radialPulseView.setCenterText(String.valueOf(idx + 1)));
                }
            }
        });

        // 观察运行状态
        viewModel.getIsRunning().observe(this, running -> {
            if (running == null) return;
            togglePlay.setChecked(running);
            // 当用户点击开始（running==true）时，显示 3 秒倒计时；当停止时，取消倒计时并恢复中心显示
            if (running) {
                radialPulseView.startCountdown(3);
                // 同步中心文本为拍号（如 4/4）在倒计时阶段显示倒计时数字，startCountdown 会覆盖文本
                radialPulseView.setCenterText(tvBeatsValue.getText().toString());
                // 启动每秒一次的倒计时音效，3 次（3,2,1）
                if (countdownTimer != null) {
                    countdownTimer.cancel();
                }
                countdownTimer = new CountDownTimer(3000, 1000) {
                    @Override
                    public void onTick(long millisUntilFinished) {
                        int secLeft = (int) Math.ceil(millisUntilFinished / 1000.0);
                        // 播放倒计时音效
                        try {
                            if (countdownPlayer != null) {
                                countdownPlayer.start();
                                // reset for next play
                                countdownPlayer.seekTo(0);
                            }
                        } catch (Exception ignored) {}
                        // 更新中心文本（radialPulseView 也会更新，但我们主动设置以保证同步）
                        if (radialPulseView != null) radialPulseView.setCenterText(String.valueOf(secLeft));
                    }

                    @Override
                    public void onFinish() {
                        // 倒计时结束，等待引擎实际启动（ViewModel 会在 3s 后调用 engine.start）
                    }
                };
                countdownTimer.start();
            } else {
                if (countdownTimer != null) {
                    countdownTimer.cancel();
                    countdownTimer = null;
                }
                radialPulseView.stopCountdown();
                radialPulseView.setCenterText(tvBeatsValue.getText().toString());
                // 停止引擎时确保当前拍显示回默认

            }
        });

        // 观察引擎实际运行状态：当 engine 实际开始运行时，停止倒计时并由 currentBeatIndex 更新中心显示与脉冲
        viewModel.getEngineRunning().observe(this, engineRunning -> {
            if (engineRunning == null) return;
            if (engineRunning) {
                radialPulseView.stopCountdown();
                Integer idx = viewModel.getCurrentBeatIndex().getValue();
                if (idx != null) {
                    String center = String.valueOf(idx + 1);
                    radialPulseView.setCenterText(center);
                    radialPulseView.pulse(idx == 0);

                }
            }
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





        // 新增：点击拍号文本弹出选择拍号列表（支持常用拍号，例如 2/4, 3/4, 4/4, 3/8, 6/8 等）
        tvBeatsValue.setOnClickListener(v -> {
            final String[] items = new String[]{
                    "2/4","3/4","4/4","3/8","6/8","5/4","6/4","7/4","5/8","7/8","8/8","9/8","2/2","3/2"
            };
            // 映射到 beatsPerMeasure 和 baseBeat
            new androidx.appcompat.app.AlertDialog.Builder(this)
                    .setTitle("选择拍号")
                    .setItems(items, (dialog, which) -> {
                        String sel = items[which];
                        // 解析 a/b
                        try {
                            String[] parts = sel.split("/");
                            int a = Integer.parseInt(parts[0]);
                            int b = Integer.parseInt(parts[1]);
                            viewModel.setBeatsPerMeasure(a);
                            // 同步 baseBeat 到 viewModel（若支持）
                            viewModel.setBaseBeat(b);
                            tvBeatsValue.setText(sel);
                        } catch (Exception ignored) { }
                    })
                    .show();
        });

        // 开始/停止
        togglePlay.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) viewModel.start(); else viewModel.stop();
        });

        // 将初始 UI 与 ViewModel 对齐
        Integer initBpm = viewModel.getBpm().getValue();
        if (initBpm != null) {
            Integer base = viewModel.getBaseBeat().getValue() != null ? viewModel.getBaseBeat().getValue() : 4;
            int initCpm = BeatHelper.BPM_TO_CPM(initBpm,base);
            tvBpmValue.setText(String.valueOf(initBpm));
            tvCrochetValue.setText(String.valueOf(initCpm));
        }
        // beats 初始值来自布局的 tv_beats_value（默认为 4），同步到 viewModel
        try {
            int beats = Integer.parseInt(tvBeatsValue.getText().toString());
            // 若文本为 a/b 格式，例如 4/4，解析前部分
            if (tvBeatsValue.getText().toString().contains("/")) {
                String[] parts = tvBeatsValue.getText().toString().split("/");
                beats = Integer.parseInt(parts[0]);
            }
            viewModel.setBeatsPerMeasure(beats);
        } catch (Exception ignored) { }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (countdownTimer != null) {
            countdownTimer.cancel();
            countdownTimer = null;
        }
        try {
            if (countdownPlayer != null) {
                countdownPlayer.release();
                countdownPlayer = null;
            }
        } catch (Exception ignored) {}
    }
}
