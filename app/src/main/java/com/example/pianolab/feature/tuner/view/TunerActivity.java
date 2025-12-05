package com.example.pianolab.feature.tuner.view;

import android.os.Bundle;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.databinding.DataBindingUtil;

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

        binding.toolbar.setNavigationOnClickListener(v -> finish());
        viewModel.getTunerState().observe(this, state -> binding.toggleButton.setText(state.isListening() ? R.string.tuner_stop_listening : R.string.tuner_start_listening));
        binding.toggleButton.setOnClickListener(v -> viewModel.toggleListening());
    }
}

