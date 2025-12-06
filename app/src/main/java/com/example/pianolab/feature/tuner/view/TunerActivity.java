package com.example.pianolab.feature.tuner.view;

import android.os.Bundle;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.databinding.DataBindingUtil;
import androidx.lifecycle.ViewModelProvider;

import com.example.pianolab.R;
import com.example.pianolab.databinding.ActivityTunerBinding;
import com.example.pianolab.feature.tuner.viewmodel.TunerViewModel;

public class TunerActivity extends AppCompatActivity {
    private ActivityTunerBinding binding;
    private TunerViewModel viewModel;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_tuner);
        binding.setLifecycleOwner(this);

        viewModel = new ViewModelProvider(this).get(TunerViewModel.class);
        binding.setViewModel(viewModel);

        initToolbar();
        initSettings();
        initButtons();
        observeState();
    }

    private void initToolbar() {
        binding.buttonBack.setOnClickListener(v -> finish());
    }

    private void initSettings() {
        binding.switchAutoDetect.setOnCheckedChangeListener((compoundButton, isChecked) -> viewModel.onAutoDetectChanged(isChecked));
        binding.switchNoiseFilter.setOnCheckedChangeListener((compoundButton, isChecked) -> viewModel.onNoiseFilterChanged(isChecked));
        binding.switchRefStandard.setOnCheckedChangeListener((compoundButton, isChecked) -> viewModel.onReferenceStandardChanged(isChecked));
    }

    private void initButtons() {
        binding.buttonPlayReference.setOnClickListener(v -> {
            viewModel.toggleReferenceTone();
            binding.buttonPlayReference.setText(viewModel.isReferencePlaying()
                    ? R.string.tuner_stop_reference
                    : R.string.tuner_play_reference);
        });
        binding.toggleListeningButton.setOnClickListener(v -> viewModel.toggleListening());
    }

    private void observeState() {
        viewModel.getTunerState().observe(this, state -> {
            binding.switchAutoDetect.setChecked(state.isAutoDetectEnabled());
            binding.switchNoiseFilter.setChecked(state.isNoiseFilterEnabled());
            binding.switchRefStandard.setChecked(Math.abs(state.getReferenceFrequency() - 442f) < 0.5f);
            binding.toggleListeningButton.setText(state.isListening() ? R.string.tuner_stop_listening : R.string.tuner_start_listening);
            binding.textCurrentNote.setText(state.getDisplayNote());
            binding.textRefFreq.setText(getString(R.string.tuner_frequency, state.getMeasuredFrequency()));
            binding.textCurrFreq.setText(getString(R.string.tuner_frequency, state.getDisplayFrequency()));
            binding.textDeviation.setText(getString(R.string.tuner_deviation, state.getDeviationCents()));
            binding.waveformPlaceholder.setWaveform(state.getWaveformSamples());
            updateDeviationIndicator(state.getDeviationCents());
        });
    }

    private void updateDeviationIndicator(float cents) {
        View ruler = binding.deviationRuler;
        View indicator = binding.deviationIndicator;
        ruler.post(() -> {
            float width = ruler.getWidth();
            float half = width / 2f;
            float normalized = Math.max(-50f, Math.min(50f, cents));
            float offset = normalized / 50f * half;
            indicator.setTranslationX(offset);
        });
    }
}
