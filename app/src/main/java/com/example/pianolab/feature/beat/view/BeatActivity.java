package com.example.pianolab.feature.beat.view;

import android.content.res.ColorStateList;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.text.InputFilter;
import android.text.InputType;
import android.text.method.DigitsKeyListener;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import androidx.annotation.Nullable;
import com.example.pianolab.ui.BaseActivity;
import com.example.pianolab.utils.ThemeColors;
import androidx.core.graphics.Insets;
import androidx.core.widget.NestedScrollView;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.ViewModelProvider;
import com.example.pianolab.R;
import com.example.pianolab.feature.beat.model.BeatSettings;
import com.example.pianolab.feature.beat.viewmodel.BeatViewModel;
import com.example.pianolab.utils.BeatHelper;
import com.example.pianolab.utils.ImmersiveUiHelper;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.android.material.slider.Slider;

public class BeatActivity extends BaseActivity {

  private BeatViewModel viewModel;

  private android.widget.TextView tvBpmValue;
  private android.widget.TextView tvBpmCpmSubtitle;
  private Slider sliderBpm;
  private android.widget.TextView tvTempoMarking;
  private android.widget.TextView tvTempoMarkingExtra;
  private MaterialButton btnPlay;
  private ImageButton btnToolbarPlay;
  private RadialPulseView radialPulseView;
  private MaterialSwitch switchAccent;
  private ChipGroup chipGroupTimeSig;
  private ChipGroup chipGroupTone;
  private ChipGroup chipGroupRhythm;
  private BottomSheetBehavior<View> bottomSheetBehavior;
  private NestedScrollView scrollPanelControls;

  private CountDownTimer countdownTimer;
  private boolean accentProgrammatic = false;
  private boolean toneProgrammatic = false;
  private boolean rhythmProgrammatic = false;
  private boolean playStateProgrammatic = false;
  private String selectedTimeSignature = "4/4";

  @Override
  protected void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    ImmersiveUiHelper.enableStandardSystemBars(getWindow());
    setContentView(R.layout.activity_beat);

    setupToolbar();

    viewModel = new ViewModelProvider(this).get(BeatViewModel.class);

    radialPulseView = findViewById(R.id.radial_pulse_view);
    radialPulseView.setCenterTextSizeFactor(0.45f);

    tvBpmValue = findViewById(R.id.tv_bpm_value);
    tvBpmCpmSubtitle = findViewById(R.id.tv_bpm_cpm_subtitle);
    tvTempoMarking = findViewById(R.id.tv_tempo_marking);
    tvTempoMarkingExtra = findViewById(R.id.tv_tempo_marking_extra);
    sliderBpm = findViewById(R.id.slider_bpm);
    btnPlay = findViewById(R.id.btn_play);
    btnToolbarPlay = findViewById(R.id.btn_toolbar_play);
    switchAccent = findViewById(R.id.switch_accent);
    chipGroupTimeSig = findViewById(R.id.chip_group_time_sig);
    chipGroupTone = findViewById(R.id.chip_group_tone);
    chipGroupRhythm = findViewById(R.id.chip_group_rhythm);
    scrollPanelControls = findViewById(R.id.scroll_panel_controls);

    setupBottomSheet();
    setupTimeSignatureChips();
    setupToneChips();
    setupRhythmChips();
    setupBpmControls();
    setupAccentSwitch();
    setupPlayButton();

    viewModel.getBpm().observe(this, this::onBpmChanged);
    viewModel.getBaseBeat().observe(this, b -> onBpmChanged(viewModel.getBpm().getValue()));
    viewModel.getAccentEnabled().observe(this, this::onAccentChanged);
    viewModel.getCurrentBeatIndex().observe(this, this::onCurrentBeatChanged);
    viewModel.getIsRunning().observe(this, this::onRunningChanged);
    viewModel.getEngineRunning().observe(this, this::onEngineRunningChanged);

    syncInitialState();
  }

  private void setupToolbar() {
    MaterialToolbar toolbar = findViewById(R.id.toolbar);
    setSupportActionBar(toolbar);
    if (getSupportActionBar() != null) {
      getSupportActionBar().setDisplayHomeAsUpEnabled(true);
      getSupportActionBar().setDisplayShowHomeEnabled(true);
    }
  }

  @Override
  public boolean onSupportNavigateUp() {
    finish();
    return true;
  }

  private void setupBottomSheet() {
    View bottomSheet = findViewById(R.id.panel_controls);
    View contentRoot = findViewById(R.id.content_root);
    int peekHeight = getResources().getDimensionPixelSize(R.dimen.beat_bottom_sheet_peek_height);

    bottomSheetBehavior = BottomSheetBehavior.from(bottomSheet);
    bottomSheetBehavior.setHideable(false);
    bottomSheetBehavior.setFitToContents(false);
    bottomSheetBehavior.setPeekHeight(peekHeight);
    bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
    bottomSheetBehavior.addBottomSheetCallback(
        new BottomSheetBehavior.BottomSheetCallback() {
          @Override
          public void onStateChanged(View bottomSheetView, int newState) {
            if (newState == BottomSheetBehavior.STATE_COLLAPSED) {
              resetPanelScroll();
            }
          }

          @Override
          public void onSlide(View bottomSheetView, float slideOffset) {}
        });

    ViewCompat.setOnApplyWindowInsetsListener(
        contentRoot,
        (v, windowInsets) -> {
          Insets insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars());
          v.setPadding(insets.left, insets.top, insets.right, insets.bottom + peekHeight);
          return windowInsets;
        });
    ViewCompat.requestApplyInsets(contentRoot);
  }

  private void setupTimeSignatureChips() {
    String[] items = getResources().getStringArray(R.array.beat_time_signatures);
    for (String item : items) {
      Chip chip = new Chip(this);
      chip.setText(item);
      chip.setCheckable(true);
      chip.setTag(item);
      chip.setChipBackgroundColor(
          ColorStateList.valueOf(ThemeColors.get(this, com.google.android.material.R.attr.colorSurfaceVariant)));
      chip.setTextAppearance(R.style.TextAppearance_PianoLab_LabelLarge);
      chip.setOnClickListener(v -> applyTimeSignature(item));
      chipGroupTimeSig.addView(chip);
      if ("4/4".equals(item)) {
        chip.setChecked(true);
        selectedTimeSignature = item;
        applyChipStyle(chip, true);
      }
    }
    chipGroupTimeSig.setOnCheckedStateChangeListener(
        (group, checkedIds) -> {
          if (checkedIds.isEmpty()) {
            return;
          }
          Chip chip = group.findViewById(checkedIds.get(0));
          if (chip != null && chip.getTag() instanceof String) {
            applyTimeSignature((String) chip.getTag());
          }
        });
  }

  private void applyTimeSignature(String sel) {
    selectedTimeSignature = sel;
    try {
      String[] parts = sel.split("/");
      int beats = Integer.parseInt(parts[0]);
      int base = Integer.parseInt(parts[1]);
      viewModel.setBeatsPerMeasure(beats);
      viewModel.setBaseBeat(base);
      if (radialPulseView != null) {
        radialPulseView.setCenterText(sel);
      }
    } catch (Exception ignored) {
    }
    selectTimeSignatureChip(sel);
  }

  private void applyChipStyle(Chip chip, boolean selected) {
    if (selected) {
      chip.setChipBackgroundColor(
          ColorStateList.valueOf(ThemeColors.get(this, com.google.android.material.R.attr.colorPrimaryContainer)));
      chip.setTextColor(ThemeColors.get(this, com.google.android.material.R.attr.colorOnPrimaryContainer));
    } else {
      chip.setChipBackgroundColor(
          ColorStateList.valueOf(ThemeColors.get(this, com.google.android.material.R.attr.colorSurfaceVariant)));
      chip.setTextColor(ThemeColors.get(this, com.google.android.material.R.attr.colorOnSurfaceVariant));
    }
  }

  private void selectTimeSignatureChip(String timeSignature) {
    for (int i = 0; i < chipGroupTimeSig.getChildCount(); i++) {
      View child = chipGroupTimeSig.getChildAt(i);
      if (child instanceof Chip) {
        Chip chip = (Chip) child;
        boolean selected = timeSignature.equals(chip.getTag());
        chip.setChecked(selected);
        applyChipStyle(chip, selected);
      }
    }
  }

  private Chip createSettingsChip(String label, int index) {
    Chip chip = new Chip(this);
    chip.setText(label);
    chip.setCheckable(true);
    chip.setTag(index);
    chip.setChipBackgroundColor(
        ColorStateList.valueOf(
            ThemeColors.get(this, com.google.android.material.R.attr.colorSurfaceVariant)));
    chip.setTextAppearance(R.style.TextAppearance_PianoLab_LabelLarge);
    return chip;
  }

  private void selectChipInGroup(ChipGroup group, int index) {
    for (int i = 0; i < group.getChildCount(); i++) {
      View child = group.getChildAt(i);
      if (child instanceof Chip) {
        Chip chip = (Chip) child;
        boolean selected =
            chip.getTag() instanceof Integer && ((Integer) chip.getTag()).intValue() == index;
        chip.setChecked(selected);
        applyChipStyle(chip, selected);
      }
    }
  }

  private int rhythmIndexForId(int rhythmId) {
    for (int i = 0; i < BeatHelper.SPECIAL_RHYTHM_IDS.length; i++) {
      if (BeatHelper.SPECIAL_RHYTHM_IDS[i] == rhythmId) {
        return i;
      }
    }
    return 0;
  }

  private void setupToneChips() {
    String[] toneOptions = getResources().getStringArray(R.array.tone_options);
    for (int i = 0; i < toneOptions.length; i++) {
      final int position = i;
      Chip chip = createSettingsChip(toneOptions[i], position);
      chip.setOnClickListener(v -> applyToneSelection(position));
      chipGroupTone.addView(chip);
      if (i == 0) {
        chip.setChecked(true);
        applyChipStyle(chip, true);
      }
    }
    chipGroupTone.setOnCheckedStateChangeListener(
        (group, checkedIds) -> {
          if (checkedIds.isEmpty() || toneProgrammatic) {
            return;
          }
          Chip chip = group.findViewById(checkedIds.get(0));
          if (chip != null && chip.getTag() instanceof Integer) {
            applyToneSelection((Integer) chip.getTag());
          }
        });

    viewModel
        .getToneType()
        .observe(
            this,
            type -> {
              if (type == null || type < 0 || type >= toneOptions.length) {
                return;
              }
              toneProgrammatic = true;
              selectChipInGroup(chipGroupTone, type);
              toneProgrammatic = false;
            });
  }

  private void applyToneSelection(int position) {
    if (toneProgrammatic) {
      return;
    }
    Integer current = viewModel.getToneType().getValue();
    if (current == null || current != position) {
      viewModel.setToneType(position);
    }
    selectChipInGroup(chipGroupTone, position);
  }

  private void setupRhythmChips() {
    String[] rhythmOptions = getResources().getStringArray(R.array.special_rhythm_options);
    for (int i = 0; i < rhythmOptions.length; i++) {
      final int position = i;
      Chip chip = createSettingsChip(rhythmOptions[i], position);
      chip.setOnClickListener(v -> applyRhythmSelection(position));
      chipGroupRhythm.addView(chip);
      if (i == 0) {
        chip.setChecked(true);
        applyChipStyle(chip, true);
      }
    }
    chipGroupRhythm.setOnCheckedStateChangeListener(
        (group, checkedIds) -> {
          if (checkedIds.isEmpty() || rhythmProgrammatic) {
            return;
          }
          Chip chip = group.findViewById(checkedIds.get(0));
          if (chip != null && chip.getTag() instanceof Integer) {
            applyRhythmSelection((Integer) chip.getTag());
          }
        });

    viewModel
        .getSpecialRhythmId()
        .observe(
            this,
            rhythmId -> {
              if (rhythmId == null) {
                return;
              }
              rhythmProgrammatic = true;
              selectChipInGroup(chipGroupRhythm, rhythmIndexForId(rhythmId));
              rhythmProgrammatic = false;
            });
  }

  private void applyRhythmSelection(int position) {
    if (rhythmProgrammatic
        || position < 0
        || position >= BeatHelper.SPECIAL_RHYTHM_IDS.length) {
      return;
    }
    int rhythmId = BeatHelper.SPECIAL_RHYTHM_IDS[position];
    Integer current = viewModel.getSpecialRhythmId().getValue();
    if (current == null || current != rhythmId) {
      viewModel.setSpecialRhythmId(rhythmId);
      restartMetronomeIfRunning();
    }
    selectChipInGroup(chipGroupRhythm, position);
  }

  private void setupBpmControls() {
    findViewById(R.id.btn_bpm_dec)
        .setOnClickListener(v -> adjustBpm(-1));
    findViewById(R.id.btn_bpm_inc)
        .setOnClickListener(v -> adjustBpm(1));

    sliderBpm.addOnChangeListener(
        (slider, value, fromUser) -> {
          if (!fromUser) {
            return;
          }
          viewModel.setBpm(Math.round(value));
        });

    sliderBpm.addOnSliderTouchListener(
        new Slider.OnSliderTouchListener() {
          @Override
          public void onStartTrackingTouch(Slider slider) {}

          @Override
          public void onStopTrackingTouch(Slider slider) {
            Boolean isRunning = viewModel.getIsRunning().getValue();
            if (isRunning != null && isRunning) {
              viewModel.stop();
              viewModel.start();
            }
          }
        });

    tvBpmValue.setOnClickListener(v -> showBpmInputDialog());
  }

  private void adjustBpm(int delta) {
    Integer current = viewModel.getBpm().getValue();
    int bpm = current != null ? current : 120;
    viewModel.setBpm(BeatHelper.clampBPM(bpm + delta));
    restartMetronomeIfRunning();
  }

  private void onBpmChanged(Integer value) {
    if (value == null) {
      return;
    }
    Integer base = viewModel.getBaseBeat().getValue();
    if (base == null) {
      base = 4;
    }

    tvBpmValue.setText(String.valueOf(value));
    tvBpmCpmSubtitle.setText(
        getString(R.string.beat_bpm_cpm_subtitle, value, BeatHelper.BPM_TO_CPM(value, base)));
    tvTempoMarking.setText(BeatHelper.getBpmDescription(value));
    String extra = BeatHelper.getBpmDescriptionExtra(value);
    tvTempoMarkingExtra.setText(extra);
    tvTempoMarkingExtra.setVisibility(
        extra == null || extra.isEmpty() ? View.GONE : View.VISIBLE);

    float sliderValue = value;
    if (Math.abs(sliderBpm.getValue() - sliderValue) > 0.5f) {
      sliderBpm.setValue(sliderValue);
    }
  }

  private void setupAccentSwitch() {
    switchAccent.setOnCheckedChangeListener(
        (buttonView, isChecked) -> {
          if (accentProgrammatic) {
            accentProgrammatic = false;
            return;
          }
          try {
            viewModel.setAccentEnabled(isChecked);
          } catch (Exception ignored) {
          }
          restartMetronomeIfRunning();
        });
  }

  private void onAccentChanged(Boolean enabled) {
    boolean e = enabled == null || enabled;
    if (switchAccent != null && switchAccent.isChecked() != e) {
      accentProgrammatic = true;
      switchAccent.setChecked(e);
    }
  }

  private void restartMetronomeIfRunning() {
    Boolean isRunning = viewModel.getIsRunning().getValue();
    if (isRunning != null && isRunning) {
      viewModel.stop();
      viewModel.start();
    }
  }

  private void setupPlayButton() {
    View.OnClickListener playListener = v -> togglePlayback();
    btnPlay.setOnClickListener(playListener);
    btnToolbarPlay.setOnClickListener(playListener);
  }

  private void togglePlayback() {
    Boolean running = viewModel.getIsRunning().getValue();
    if (running != null && running) {
      viewModel.stop();
    } else {
      collapseBottomSheet();
      viewModel.start();
    }
  }

  private void collapseBottomSheet() {
    resetPanelScroll();
    if (bottomSheetBehavior != null
        && bottomSheetBehavior.getState() != BottomSheetBehavior.STATE_COLLAPSED) {
      bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
    }
  }

  private void resetPanelScroll() {
    if (scrollPanelControls != null) {
      scrollPanelControls.scrollTo(0, 0);
    }
  }

  private void onRunningChanged(Boolean running) {
    if (running == null) {
      return;
    }
    updatePlayButtonUi(running);

    if (running) {
      radialPulseView.startCountdown(3);
      radialPulseView.setCenterText(selectedTimeSignature);
      if (countdownTimer != null) {
        countdownTimer.cancel();
      }
      countdownTimer =
          new CountDownTimer(3000, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
              int secLeft = (int) Math.ceil(millisUntilFinished / 1000.0);
              try {
                viewModel.playCountdown();
              } catch (Exception ignored) {
              }
              if (radialPulseView != null) {
                radialPulseView.setCenterText(String.valueOf(secLeft));
              }
            }

            @Override
            public void onFinish() {}
          };
      countdownTimer.start();
    } else {
      if (countdownTimer != null) {
        countdownTimer.cancel();
        countdownTimer = null;
      }
      radialPulseView.stopCountdown();
      radialPulseView.setCenterText(selectedTimeSignature);
    }
  }

  private void updatePlayButtonUi(boolean running) {
    if (playStateProgrammatic) {
      return;
    }
    playStateProgrammatic = true;
    if (running) {
      btnPlay.setText(R.string.beat_stop);
      btnPlay.setIconResource(R.drawable.ic_pause);
      btnPlay.setBackgroundTintList(
          ColorStateList.valueOf(ThemeColors.get(this, com.google.android.material.R.attr.colorError)));
      btnPlay.setTextColor(ThemeColors.get(this, com.google.android.material.R.attr.colorOnError));
      btnPlay.setIconTint(ColorStateList.valueOf(ThemeColors.get(this, com.google.android.material.R.attr.colorOnError)));
      btnToolbarPlay.setImageResource(R.drawable.ic_pause);
      btnToolbarPlay.setContentDescription(getString(R.string.beat_stop));
      btnToolbarPlay.setImageTintList(
          ColorStateList.valueOf(ThemeColors.get(this, com.google.android.material.R.attr.colorError)));
    } else {
      btnPlay.setText(R.string.beat_play);
      btnPlay.setIconResource(R.drawable.ic_play_arrow);
      btnPlay.setBackgroundTintList(
          ColorStateList.valueOf(ThemeColors.get(this, com.google.android.material.R.attr.colorPrimary)));
      btnPlay.setTextColor(ThemeColors.get(this, com.google.android.material.R.attr.colorOnPrimary));
      btnPlay.setIconTint(ColorStateList.valueOf(ThemeColors.get(this, com.google.android.material.R.attr.colorOnPrimary)));
      btnToolbarPlay.setImageResource(R.drawable.ic_play_arrow);
      btnToolbarPlay.setContentDescription(getString(R.string.beat_play));
      btnToolbarPlay.setImageTintList(
          ColorStateList.valueOf(ThemeColors.get(this, com.google.android.material.R.attr.colorPrimary)));
    }
    playStateProgrammatic = false;
  }

  private void onCurrentBeatChanged(Integer idx) {
    if (idx == null) {
      return;
    }
    Boolean engineRunningNow = viewModel.getEngineRunning().getValue();
    if (engineRunningNow != null && engineRunningNow) {
      if (radialPulseView != null) {
        radialPulseView.post(
            () -> {
              radialPulseView.pulse(idx == 0);
              radialPulseView.setCenterText(String.valueOf(idx + 1));
              if (countdownTimer != null) {
                countdownTimer.cancel();
                countdownTimer = null;
              }
              radialPulseView.stopCountdown();
            });
      }
    }
  }

  private void onEngineRunningChanged(Boolean engineRunning) {
    if (engineRunning == null || !engineRunning) {
      return;
    }
    radialPulseView.stopCountdown();
    Integer idx = viewModel.getCurrentBeatIndex().getValue();
    if (idx != null) {
      radialPulseView.setCenterText(String.valueOf(idx + 1));
      radialPulseView.pulse(idx == 0);
    }
  }

  private void showBpmInputDialog() {
    EditText et = new EditText(this);
    et.setInputType(InputType.TYPE_CLASS_NUMBER);
    et.setKeyListener(DigitsKeyListener.getInstance("0123456789"));
    et.setFilters(new InputFilter[] {new InputFilter.LengthFilter(4)});

    Integer curQuarterBpm = viewModel.getBpm().getValue();
    int prefill = curQuarterBpm != null ? curQuarterBpm : 120;
    et.setText(String.valueOf(prefill));

    new androidx.appcompat.app.AlertDialog.Builder(this)
        .setTitle(R.string.beat_set_bpm_title)
        .setView(et)
        .setPositiveButton(
            R.string.beat_ok,
            (dialog, which) -> {
              try {
                String s = et.getText().toString().trim();
                if (s.isEmpty()) {
                  return;
                }
                int bpmVal = BeatHelper.clampBPM(Integer.parseInt(s));
                viewModel.setBpm(bpmVal);
              } catch (Exception ignored) {
              }
            })
        .setNegativeButton(R.string.beat_cancel, null)
        .show();
  }

  private void syncInitialState() {
    Integer initBpm = viewModel.getBpm().getValue();
    if (initBpm != null) {
      onBpmChanged(initBpm);
    }
    try {
      viewModel.setBeatsPerMeasure(4);
      viewModel.setBaseBeat(4);
    } catch (Exception ignored) {
    }
    selectTimeSignatureChip(selectedTimeSignature);
    if (radialPulseView != null) {
      radialPulseView.setCenterText(selectedTimeSignature);
    }
    updatePlayButtonUi(false);
  }

  @Override
  protected void onStop() {
    super.onStop();
    if (!isChangingConfigurations() && viewModel != null) {
      viewModel.stop();
    }
  }

  @Override
  protected void onDestroy() {
    if (countdownTimer != null) {
      countdownTimer.cancel();
      countdownTimer = null;
    }
    super.onDestroy();
  }
}
