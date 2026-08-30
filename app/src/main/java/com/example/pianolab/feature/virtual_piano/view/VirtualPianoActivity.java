package com.example.pianolab.feature.virtual_piano.view;

import android.os.Bundle;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.ImageButton;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.lifecycle.ViewModelProvider;

import com.example.pianolab.R;
import com.example.pianolab.feature.virtual_piano.viewmodel.VirtualPianoViewModel;
import com.example.pianolab.utils.ImmersiveUiHelper;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.android.material.slider.Slider;

public class VirtualPianoActivity extends AppCompatActivity {
    private static final int MIDI_CENTER_DEFAULT = 60;

    private SlicePianoKeyboardView pianoView;
    private VirtualPianoViewModel viewModel;
    private DrawerLayout drawerLayout;
    private MaterialSwitch switchShowNote;
    private MaterialSwitch switchSustain;
    private Slider sliderKeyScale;

    private boolean noteProgrammatic = false;
    private boolean sustainProgrammatic = false;
    private boolean keyScaleProgrammatic = false;

  @Override
  protected void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    ImmersiveUiHelper.enableImmersiveMode(getWindow());
    setContentView(R.layout.activity_virtual_piano);
    ImmersiveUiHelper.suppressSystemBarInsets(findViewById(R.id.drawer_layout));

    viewModel = new ViewModelProvider(this).get(VirtualPianoViewModel.class);

    drawerLayout = findViewById(R.id.drawer_layout);
    pianoView = findViewById(R.id.piano_view);
    switchShowNote = findViewById(R.id.switch_show_note);
    switchSustain = findViewById(R.id.switch_sustain);
    sliderKeyScale = findViewById(R.id.slider_key_scale);

    setupToolbar();
    setupDrawer();
    setupScrollButtons();
    setupNoteSwitch();
    setupSustainSwitch();
    setupKeyScaleSlider();
    setupInitialScroll();
    observeViewModel();

    getOnBackPressedDispatcher()
        .addCallback(
            this,
            new OnBackPressedCallback(true) {
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

    ImageButton btnMenu = findViewById(R.id.btn_toolbar_menu);
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
    drawerLayout.addDrawerListener(
        new DrawerLayout.SimpleDrawerListener() {
          @Override
          public void onDrawerClosed(View drawerView) {
            if (drawerView.getId() == R.id.drawer_settings) {
              drawerView.scrollTo(0, 0);
            }
          }
        });
  }

  private void setupScrollButtons() {
    findViewById(R.id.btn_scroll_left)
        .setOnClickListener(v -> pianoView.animateScrollByOctaves(-1));
    findViewById(R.id.btn_scroll_right)
        .setOnClickListener(v -> pianoView.animateScrollByOctaves(1));
  }

  private void setupNoteSwitch() {
    switchShowNote.setOnCheckedChangeListener(
        (buttonView, isChecked) -> {
          if (noteProgrammatic) {
            return;
          }
          viewModel.setShowNoteNames(isChecked);
        });
  }

  private void setupSustainSwitch() {
    switchSustain.setOnCheckedChangeListener(
        (buttonView, isChecked) -> {
          if (sustainProgrammatic) {
            return;
          }
          viewModel.setSustainEnabled(isChecked);
        });
  }

  private void setupKeyScaleSlider() {
    sliderKeyScale.addOnChangeListener(
        (slider, value, fromUser) -> {
          if (!fromUser || keyScaleProgrammatic) {
            return;
          }
          viewModel.setKeyScale(value);
        });
  }

  private void setupInitialScroll() {
    pianoView
        .getViewTreeObserver()
        .addOnGlobalLayoutListener(
            new ViewTreeObserver.OnGlobalLayoutListener() {
              @Override
              public void onGlobalLayout() {
                pianoView.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                pianoView.scrollToMidiCenter(MIDI_CENTER_DEFAULT);
              }
            });
  }

  private void observeViewModel() {
    viewModel
        .getShowNoteNames()
        .observe(
            this,
            show -> {
              boolean showNames = show == null || show;
              if (switchShowNote.isChecked() != showNames) {
                noteProgrammatic = true;
                switchShowNote.setChecked(showNames);
                noteProgrammatic = false;
              }
              pianoView.setShowPitchNames(showNames);
            });

    viewModel
        .getSustainEnabled()
        .observe(
            this,
            enabled -> {
              boolean sustain = enabled != null && enabled;
              if (switchSustain.isChecked() != sustain) {
                sustainProgrammatic = true;
                switchSustain.setChecked(sustain);
                sustainProgrammatic = false;
              }
              pianoView.setSustainEnabled(sustain);
            });

    viewModel
        .getKeyScale()
        .observe(
            this,
            scale -> {
              if (scale == null) {
                return;
              }
              if (Math.abs(pianoView.getKeyScale() - scale) > 0.001f) {
                pianoView.setKeyScale(scale);
              }
              if (Math.abs(sliderKeyScale.getValue() - scale) > 0.001f) {
                keyScaleProgrammatic = true;
                sliderKeyScale.setValue(scale);
                keyScaleProgrammatic = false;
              }
            });
  }
}
