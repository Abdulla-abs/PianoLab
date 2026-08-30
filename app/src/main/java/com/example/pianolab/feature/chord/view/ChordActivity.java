package com.example.pianolab.feature.chord.view;

import android.os.Bundle;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.lifecycle.ViewModelProvider;

import com.example.pianolab.R;
import com.example.pianolab.feature.chord.viewmodel.ChordViewModel;
import com.example.pianolab.feature.virtual_piano.view.PianoRangeOverviewView;
import com.example.pianolab.feature.virtual_piano.view.SlicePianoKeyboardView;
import com.example.pianolab.utils.ImmersiveUiHelper;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.android.material.card.MaterialCardView;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ChordActivity extends AppCompatActivity {
    private static final int MIDI_CENTER_DEFAULT = 60;

    private ChordViewModel viewModel;
    private DrawerLayout drawerLayout;
    private SlicePianoKeyboardView pianoView;
    private PianoRangeOverviewView rangeOverviewView;
    private StaffView staffGClef;
    private StaffView staffFClef;
    private TextView tvChordName;
    private TextView textChordModeDesc;
    private TextView textAccidentalDesc;
    private MaterialButtonToggleGroup toggleGroupChordMode;
    private MaterialButtonToggleGroup toggleGroupAccidental;
    private MaterialCardView cardPlayChord;
    private boolean chordModeProgrammatic;
    private boolean accidentalProgrammatic;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ImmersiveUiHelper.enableStandardSystemBars(getWindow());
        setContentView(R.layout.activity_chord);
        ImmersiveUiHelper.suppressSystemBarInsets(findViewById(R.id.drawer_layout));

        viewModel = new ViewModelProvider(this).get(ChordViewModel.class);

        drawerLayout = findViewById(R.id.drawer_layout);
        pianoView = findViewById(R.id.piano_view);
        rangeOverviewView = findViewById(R.id.piano_range_overview);
        staffGClef = findViewById(R.id.staff_g_clef);
        staffFClef = findViewById(R.id.staff_f_clef);
        tvChordName = findViewById(R.id.tv_chord_name);
        textChordModeDesc = findViewById(R.id.text_chord_mode_desc);
        textAccidentalDesc = findViewById(R.id.text_accidental_desc);
        toggleGroupChordMode = findViewById(R.id.toggle_group_chord_mode);
        toggleGroupAccidental = findViewById(R.id.toggle_group_accidental);
        cardPlayChord = findViewById(R.id.card_play_chord);

        setupToolbar();
        setupDrawer();
        setupPiano();
        setupSettings();
        setupActions();
        observeViewModel();
        setupInitialScroll();

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
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            ImmersiveUiHelper.applyImmersiveSystemUi(getWindow());
        }
    }

    private void setupToolbar() {
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }

        ImageButton btnUndo = findViewById(R.id.btn_toolbar_undo);
        ImageButton btnReset = findViewById(R.id.btn_toolbar_reset);
        ImageButton btnMenu = findViewById(R.id.btn_toolbar_menu);

        btnUndo.setOnClickListener(v -> viewModel.backout());
        btnReset.setOnClickListener(v -> viewModel.reset());
        btnMenu.setOnClickListener(v -> drawerLayout.openDrawer(GravityCompat.END));
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

    private void setupPiano() {
        pianoView.setSelectionMode(true);
        pianoView.setOnKeyToggledListener(key -> viewModel.toggleKey(key));

        pianoView.setOnScrollStateChangedListener(
                (scrollX, contentWidth, viewportWidth) ->
                        rangeOverviewView.updateViewport(scrollX, contentWidth, viewportWidth));
        rangeOverviewView.setOnViewportScrollListener(pianoView::setKeyboardScrollX);
    }

    private void setupSettings() {
        staffGClef.setClefType(StaffView.ClefType.TREBLE);
        staffFClef.setClefType(StaffView.ClefType.BASS);

        toggleGroupChordMode.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (chordModeProgrammatic || !isChecked) {
                return;
            }
            viewModel.isChordFuncMode.setValue(checkedId == R.id.btn_mode_construct);
        });

        toggleGroupAccidental.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (accidentalProgrammatic || !isChecked) {
                return;
            }
            applyAccidentalMode(checkedId == R.id.btn_accidental_flat);
        });
    }

    private void setupActions() {
        tvChordName.setOnClickListener(v -> {
            Boolean constructMode = viewModel.isChordFuncMode.getValue();
            if (constructMode != null && constructMode) {
                showChordPickerDialog();
            }
        });

        cardPlayChord.setOnClickListener(v -> playSelectedChord());
    }

    private void setupInitialScroll() {
        pianoView.getViewTreeObserver().addOnGlobalLayoutListener(
                new ViewTreeObserver.OnGlobalLayoutListener() {
                    @Override
                    public void onGlobalLayout() {
                        pianoView.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                        pianoView.scrollToMidiCenter(MIDI_CENTER_DEFAULT);
                    }
                });
    }

    private void observeViewModel() {
        viewModel.selectedKeys.observe(this, keys -> {
            pianoView.setSelectedKeys(keys);
            staffGClef.setNotes(keys);
            staffFClef.setNotes(keys);
            updateRangeOverviewSelection(keys);
        });

        viewModel.chordText.observe(this, text -> tvChordName.setText(text));

        viewModel.playChordNonce.observe(this, nonce -> playSelectedChord());

        viewModel.isChordFuncMode.observe(this, constructMode -> {
            boolean enabled = constructMode != null && constructMode;
            int modeButtonId = enabled ? R.id.btn_mode_construct : R.id.btn_mode_detect;
            if (toggleGroupChordMode.getCheckedButtonId() != modeButtonId) {
                chordModeProgrammatic = true;
                toggleGroupChordMode.check(modeButtonId);
                chordModeProgrammatic = false;
            }
            textChordModeDesc.setText(
                    enabled ? R.string.chord_mode_construct_desc : R.string.chord_mode_detect_desc);
            updateChordNameInteractiveState(enabled);
        });
    }

    private void playSelectedChord() {
        List<String> keys = viewModel.selectedKeys.getValue();
        if (keys != null && !keys.isEmpty()) {
            pianoView.playChord(new ArrayList<>(keys));
        }
    }

    private void updateChordNameInteractiveState(boolean constructMode) {
        if (constructMode) {
            tvChordName.setBackgroundResource(R.drawable.bg_manual_note_blue);
            tvChordName.setClickable(true);
            tvChordName.setFocusable(true);
        } else {
            tvChordName.setBackground(null);
            tvChordName.setClickable(false);
            tvChordName.setFocusable(false);
        }
    }

    private void updateRangeOverviewSelection(List<String> keys) {
        Set<Integer> midis = new HashSet<>(SlicePianoKeyboardView.midisFromKeyNames(keys));
        rangeOverviewView.setActiveMidis(midis);
    }

    private void applyAccidentalMode(boolean useFlats) {
        staffGClef.setUseFlats(useFlats);
        staffFClef.setUseFlats(useFlats);
        textAccidentalDesc.setText(
                useFlats ? R.string.chord_accidental_flat_desc : R.string.chord_accidental_sharp_desc);
        int accidentalButtonId = useFlats ? R.id.btn_accidental_flat : R.id.btn_accidental_sharp;
        if (toggleGroupAccidental.getCheckedButtonId() != accidentalButtonId) {
            accidentalProgrammatic = true;
            toggleGroupAccidental.check(accidentalButtonId);
            accidentalProgrammatic = false;
        }
    }

    private void showChordPickerDialog() {
        boolean useFlats = toggleGroupAccidental.getCheckedButtonId() == R.id.btn_accidental_flat;
        new ChordPickerDialog(this, useFlats, chordName -> viewModel.generateChord(chordName)).show();
    }
}
