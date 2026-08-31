package com.example.pianolab.feature.home.view;

import android.os.Bundle;
import androidx.appcompat.app.AlertDialog;
import androidx.databinding.DataBindingUtil;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.GridLayoutManager;
import com.example.pianolab.R;
import com.example.pianolab.databinding.ActivityHomeBinding;
import com.example.pianolab.feature.home.viewmodel.HomeViewModel;
import com.example.pianolab.ui.BaseActivity;
import com.example.pianolab.utils.ImmersiveUiHelper;
import com.example.pianolab.utils.ThemeManager;
import com.example.pianolab.utils.ThemeMode;

/** 主界面：功能选择入口 */
public class HomeActivity extends BaseActivity {
  private ActivityHomeBinding binding;
  private HomeViewModel homeViewModel;
  private FeatureAdapter featureAdapter;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    ImmersiveUiHelper.enableStandardSystemBars(getWindow());
    binding = DataBindingUtil.setContentView(this, R.layout.activity_home);
    ImmersiveUiHelper.applySystemBarInsets(binding.contentRoot);

    homeViewModel = new ViewModelProvider(this).get(HomeViewModel.class);

    initToolbar();
    initVersionBadge();
    initRecyclerView();
  }

  private void initToolbar() {
    binding.btnTheme.setOnClickListener(v -> showThemeDialog());
  }

  private void showThemeDialog() {
    ThemeMode current = ThemeManager.getThemeMode(this);
    ThemeMode[] modes = ThemeMode.values();
    String[] labels = new String[modes.length];
    int checked = 0;
    for (int i = 0; i < modes.length; i++) {
      labels[i] = getThemeModeLabel(modes[i]);
      if (modes[i] == current) {
        checked = i;
      }
    }

    new AlertDialog.Builder(this)
        .setTitle(R.string.theme_dialog_title)
        .setSingleChoiceItems(
            labels,
            checked,
            (dialog, which) -> {
              ThemeMode selected = modes[which];
              if (selected != ThemeManager.getThemeMode(this)) {
                ThemeManager.setThemeMode(this, selected);
                recreate();
              }
              dialog.dismiss();
            })
        .setNegativeButton(android.R.string.cancel, null)
        .show();
  }

  private String getThemeModeLabel(ThemeMode mode) {
    if (mode == ThemeMode.LIGHT) {
      return getString(R.string.theme_mode_light);
    }
    if (mode == ThemeMode.DARK) {
      return getString(R.string.theme_mode_dark);
    }
    return getString(R.string.theme_mode_system);
  }

  private void initVersionBadge() {
    binding.tvVersionBadge.setText(R.string.home_version_badge);
  }

  private void initRecyclerView() {
    int columns = getResources().getInteger(R.integer.feature_grid_columns);
    binding.rvFeatures.setLayoutManager(new GridLayoutManager(this, columns));

    homeViewModel
        .getFeatureItems()
        .observe(
            this,
            featureItems -> {
              if (featureItems != null) {
                featureAdapter =
                    new FeatureAdapter(
                        featureItems,
                        item -> homeViewModel.onFeatureItemClick(HomeActivity.this, item));
                binding.rvFeatures.setAdapter(featureAdapter);
              }
            });
  }
}
