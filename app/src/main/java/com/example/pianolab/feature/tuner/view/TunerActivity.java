package com.example.pianolab.feature.tuner.view;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.lifecycle.ViewModelProvider;

import com.example.pianolab.R;
import com.example.pianolab.feature.tuner.viewmodel.TunerViewModel;
import com.example.pianolab.utils.ImmersiveUiHelper;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.materialswitch.MaterialSwitch;

public class TunerActivity extends AppCompatActivity {
    private static final String PERMISSION = Manifest.permission.RECORD_AUDIO;
    private static final float IN_TUNE_CENTS = 3f;

    private TunerViewModel viewModel;
    private ActivityResultLauncher<String> permissionLauncher;
    private DrawerLayout drawerLayout;

    private MaterialSwitch switchAutoDetect;
    private MaterialSwitch switchAutoStop;
    private MaterialButtonToggleGroup toggleGroupFreq;
    private MaterialCardView cardActionStart;
    private MaterialCardView cardActionPlayRef;
    private MaterialCardView cardActionPlayStandard;
    private View drawerSettingsContent;
    private TextView textActionStart;
    private TextView textNoteName;
    private TextView textRefFreqValue;
    private TextView textMeasuredFreqValue;
    private TextView textDeviationValue;
    private TextView textWaveformIdle;
    private ImageView imgMeterPointer;

    private boolean settingsProgrammatic = false;
    private boolean freqProgrammatic = false;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ImmersiveUiHelper.enableStandardSystemBars(getWindow());
        setContentView(R.layout.activity_tuner);
        ImmersiveUiHelper.suppressSystemBarInsets(findViewById(R.id.drawer_layout));

        drawerLayout = findViewById(R.id.drawer_layout);
        setupToolbar();
        setupDrawer();

        viewModel = new ViewModelProvider(this).get(TunerViewModel.class);

        switchAutoDetect = findViewById(R.id.switch_auto_detect);
        switchAutoStop = findViewById(R.id.switch_auto_stop);
        toggleGroupFreq = findViewById(R.id.toggle_group_freq);
        cardActionStart = findViewById(R.id.card_action_start);
        cardActionPlayRef = findViewById(R.id.card_action_play_ref);
        cardActionPlayStandard = findViewById(R.id.card_action_play_standard);
        drawerSettingsContent = findViewById(R.id.drawer_settings_content);
        textActionStart = findViewById(R.id.text_action_start);
        textNoteName = findViewById(R.id.text_note_name);
        textRefFreqValue = findViewById(R.id.text_ref_freq_value);
        textMeasuredFreqValue = findViewById(R.id.text_measured_freq_value);
        textDeviationValue = findViewById(R.id.text_deviation_value);
        textWaveformIdle = findViewById(R.id.text_waveform_idle);
        imgMeterPointer = findViewById(R.id.img_meter_pointer);

        setupMeterTicks();
        initPermissionLauncher();
        initSettings();
        initButtons();
        observeState();

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (drawerLayout.isDrawerOpen(GravityCompat.END)) {
                    drawerLayout.closeDrawer(GravityCompat.END);
                } else {
                    setEnabled(false);
                    getOnBackPressedDispatcher().onBackPressed();
                }
            }
        });
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

    private void setupToolbar() {
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }

        ImageButton btnMenu = findViewById(R.id.btn_toolbar_menu);
        btnMenu.setOnClickListener(v -> drawerLayout.openDrawer(GravityCompat.END));
    }

    private void setupDrawer() {
        drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED, GravityCompat.END);
        drawerLayout.addDrawerListener(new DrawerLayout.SimpleDrawerListener() {
            @Override
            public void onDrawerClosed(View drawerView) {
                if (drawerView.getId() == R.id.drawer_settings) {
                    drawerView.scrollTo(0, 0);
                }
            }
        });
    }

    private void setupMeterTicks() {
        LinearLayout ticksRow = findViewById(R.id.meter_ticks_row);
        String[] ticks = getResources().getStringArray(R.array.tuner_meter_ticks);
        for (String tick : ticks) {
            TextView tickView = new TextView(this);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
            tickView.setLayoutParams(params);
            tickView.setGravity(Gravity.CENTER);
            tickView.setText(tick);
            tickView.setTextAppearance(R.style.TextAppearance_PianoLab_LabelSmall);
            tickView.setTextColor(ContextCompat.getColor(this, R.color.md_theme_tool_outline));
            ticksRow.addView(tickView);
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        if (drawerLayout != null && drawerLayout.isDrawerOpen(GravityCompat.END)) {
            drawerLayout.closeDrawer(GravityCompat.END);
            return true;
        }
        finish();
        return true;
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

    private void initSettings() {
        switchAutoDetect.setOnCheckedChangeListener((button, isChecked) -> {
            if (!settingsProgrammatic) {
                viewModel.onAutoDetectChanged(isChecked);
            }
        });
        switchAutoStop.setOnCheckedChangeListener((button, isChecked) -> {
            if (!settingsProgrammatic) {
                viewModel.onAutoStopChanged(isChecked);
            }
        });
        toggleGroupFreq.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (!freqProgrammatic && isChecked) {
                viewModel.onReferenceStandardChanged(checkedId == R.id.btn_freq_442);
            }
        });
    }

    private void initButtons() {
        cardActionStart.setOnClickListener(v -> ensurePermissionThenStart());
        cardActionPlayRef.setOnClickListener(v -> viewModel.playNote(true));
        cardActionPlayStandard.setOnClickListener(v -> viewModel.playNote(false));
        textNoteName.setOnClickListener(v -> {
            if (viewModel.getTunerState().getValue() != null
                    && !viewModel.getTunerState().getValue().isAutoDetectEnabled()) {
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
                note -> viewModel.setManualTarget(note, standardFreq)
        );
        dialog.show();
    }

    private void observeState() {
        viewModel.getTunerState().observe(this, state -> {
            boolean listening = state.isListening();

            settingsProgrammatic = true;
            switchAutoDetect.setChecked(state.isAutoDetectEnabled());
            switchAutoStop.setChecked(state.isAutoStopEnabled());
            settingsProgrammatic = false;

            freqProgrammatic = true;
            int freqButtonId = Math.abs(state.getstandardFrequency() - 442f) < 0.5f
                    ? R.id.btn_freq_442
                    : R.id.btn_freq_440;
            if (toggleGroupFreq.getCheckedButtonId() != freqButtonId) {
                toggleGroupFreq.check(freqButtonId);
            }
            freqProgrammatic = false;

            updateListeningUi(listening);
            updateSettingsEnabled(!listening);

            String displayNote = listening ? state.getDisplayNote() : getString(R.string.none_symbol);
            textNoteName.setText(displayNote);
            textNoteName.setClickable(!state.isAutoDetectEnabled());
            textNoteName.setFocusable(!state.isAutoDetectEnabled());

            textRefFreqValue.setText(getString(R.string.tuner_freq_hz_value, state.getDisplayFrequency()));
            textMeasuredFreqValue.setText(getString(
                    R.string.tuner_freq_hz_value,
                    listening ? state.getMeasuredFrequency() : 0f));

            float deviationCents = listening ? state.getDeviationCents() : 0f;
            textDeviationValue.setText(getString(R.string.tuner_cents_value, deviationCents));
            int deviationColor = resolveDeviationColor(deviationCents);
            textDeviationValue.setTextColor(deviationColor);
            imgMeterPointer.setColorFilter(deviationColor);

            textWaveformIdle.setVisibility(listening ? View.GONE : View.VISIBLE);
        });
    }

    private void updateListeningUi(boolean listening) {
        textActionStart.setText(listening
                ? R.string.tuner_stop_listening
                : R.string.tuner_start_listening);
        int startTextColor = ContextCompat.getColor(this, listening
                ? R.color.md_theme_tool_error
                : R.color.md_theme_tool_onSurface);
        textActionStart.setTextColor(startTextColor);
    }

    private void updateSettingsEnabled(boolean enabled) {
        float alpha = enabled ? 1f : 0.5f;
        drawerSettingsContent.setAlpha(alpha);
        switchAutoDetect.setEnabled(enabled);
        switchAutoStop.setEnabled(enabled);
        for (int i = 0; i < toggleGroupFreq.getChildCount(); i++) {
            toggleGroupFreq.getChildAt(i).setEnabled(enabled);
        }

        cardActionPlayRef.setEnabled(enabled);
        cardActionPlayStandard.setEnabled(enabled);
        cardActionPlayRef.setAlpha(enabled ? 1f : 0.5f);
        cardActionPlayStandard.setAlpha(enabled ? 1f : 0.5f);
    }

    private int resolveDeviationColor(float cents) {
        float absCents = Math.abs(cents);
        if (absCents <= IN_TUNE_CENTS) {
            return ContextCompat.getColor(this, R.color.tuner_cursor_green);
        }
        if (cents < -IN_TUNE_CENTS) {
            return ContextCompat.getColor(this, R.color.tuner_cursor_yellow);
        }
        return ContextCompat.getColor(this, R.color.tuner_cursor_red);
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            ImmersiveUiHelper.applyImmersiveSystemUi(getWindow());
        }
    }
}
