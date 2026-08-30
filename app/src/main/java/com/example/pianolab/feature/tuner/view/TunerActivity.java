package com.example.pianolab.feature.tuner.view;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.databinding.DataBindingUtil;
import androidx.lifecycle.ViewModelProvider;

import com.example.pianolab.R;
import com.example.pianolab.databinding.ActivityTunerBinding;
import com.example.pianolab.feature.tuner.viewmodel.TunerViewModel;
import com.example.pianolab.utils.ImmersiveUiHelper;

public class TunerActivity extends AppCompatActivity {
    private static final String PERMISSION = Manifest.permission.RECORD_AUDIO;

    private ActivityTunerBinding binding;
    private TunerViewModel viewModel;
    private ActivityResultLauncher<String> permissionLauncher;
    private CompoundButton.OnCheckedChangeListener waveModeListener;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_tuner);
        binding.setLifecycleOwner(this);
        ImmersiveUiHelper.enableImmersiveMode(getWindow());

        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        viewModel = new ViewModelProvider(this).get(TunerViewModel.class);
        binding.setViewModel(viewModel);

        initPermissionLauncher();
        initToolbar();
        initSettings();
        initButtons();
        observeState();
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (viewModel.getTunerState().getValue() != null && viewModel.getTunerState().getValue().isListening()) {
            ensurePermissionThenStart();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (viewModel.getTunerState().getValue() != null && viewModel.getTunerState().getValue().isListening()) {
            viewModel.toggleListening();
        }
    }

    private void initPermissionLauncher() {
        permissionLauncher = registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> {
            if (granted) {
                viewModel.toggleListening();
            } else {
                showPermissionRationale();
            }
        });
    }

    private void initToolbar() {
        binding.buttonBack.setOnClickListener(v -> finish());
    }

    private void initSettings() {
        binding.switchAutoDetect.setOnCheckedChangeListener((compoundButton, isChecked) -> viewModel.onAutoDetectChanged(isChecked));
        binding.switchAutoStop.setOnCheckedChangeListener((compoundButton, isChecked) -> viewModel.onAutoStopChanged(isChecked));
        binding.switchRefStandard.setOnCheckedChangeListener((compoundButton, isChecked) -> viewModel.onReferenceStandardChanged(isChecked));
        waveModeListener = (buttonView, isChecked) -> {
            boolean frequencyMode = !isChecked;
            viewModel.setWaveDisplayMode(frequencyMode);
            binding.waveformPlaceholder.setFrequencyMode(frequencyMode);
        };
        binding.switchWaveMode.setOnCheckedChangeListener(waveModeListener);
    }

    private void initButtons() {
        binding.buttonPlayReference.setOnClickListener(v -> viewModel.playNote(true));
        binding.buttonPlayStandard.setOnClickListener(v->viewModel.playNote(false));
        binding.toggleListeningButton.setOnClickListener(v -> ensurePermissionThenStart());
        binding.textCurrentNote.setOnClickListener(v -> {
            if (viewModel.getTunerState().getValue() != null &&
                    !viewModel.getTunerState().getValue().isAutoDetectEnabled()) {
                showNotePickerDialog();
            }
        });
    }

    private void ensurePermissionThenStart() {
        if (ContextCompat.checkSelfPermission(this, PERMISSION) == PackageManager.PERMISSION_GRANTED) {
            viewModel.toggleListening();
        } else {
            permissionLauncher.launch(PERMISSION);
        }
    }

    private void showPermissionRationale() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.tuner_permission_title)
                .setMessage(R.string.tuner_permission_message)
                .setPositiveButton(android.R.string.ok, null)
                .show();
    }

    private void showNotePickerDialog() {
        if (viewModel.getTunerState().getValue() == null) {
            return;
        }

        String currentNote = viewModel.getTunerState().getValue().getManualNote();
        float standardFreq = viewModel.getTunerState().getValue().getstandardFrequency();

        NotePickerDialog dialog = new NotePickerDialog(
                this,
                currentNote,
                (note) -> viewModel.setManualTarget(note,standardFreq)
        );

        dialog.show();
    }

    private void observeState() {
        viewModel.getTunerState().observe(this, state -> {
            binding.switchAutoDetect.setChecked(state.isAutoDetectEnabled());
            binding.switchAutoStop.setChecked(state.isAutoStopEnabled());
            binding.switchRefStandard.setChecked(Math.abs(state.getstandardFrequency() - 442f) < 0.5f);
            binding.switchWaveMode.setChecked(!viewModel.isFrequencyMode());
            binding.switchWaveMode.setOnCheckedChangeListener(null);
            binding.switchWaveMode.setOnCheckedChangeListener(waveModeListener);

            binding.toggleListeningButton.setText(state.isListening() ? R.string.tuner_stop_listening : R.string.tuner_start_listening);
            binding.textCurrentNote.setText(state.getDisplayNote());
            binding.textDetectedNoteFreq.setText(getString(R.string.tuner_detectedNote_frequency, state.getDisplayFrequency()));
            binding.textCurrFreq.setText(getString(R.string.tuner_current_frequency, state.getMeasuredFrequency()));
            binding.textDeviation.setText(getString(R.string.tuner_deviation, state.getDeviationCents()));

            if (binding.previewPiano instanceof TunerPianoView) {
                ((TunerPianoView) binding.previewPiano).setDetectedNote(state.getDisplayNote());
            }

            binding.waveformPlaceholder.setFrequencyMode(viewModel.isFrequencyMode());
            if (viewModel.isFrequencyMode()) {
                binding.waveformPlaceholder.setWaveform(state.getSpectrumMagnitudes());
            } else {
                binding.waveformPlaceholder.setWaveform(state.getWaveformSamples());
            }
            updateDeviationIndicator(state.getDeviationCents());
        });
    }

    private void updateDeviationIndicator(float cents) {
        if (binding.deviationRuler instanceof DeviationRulerView) {
            ((DeviationRulerView) binding.deviationRuler).setDeviation(cents);
        }
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            ImmersiveUiHelper.applyImmersiveSystemUi(getWindow());
        }
    }
}
