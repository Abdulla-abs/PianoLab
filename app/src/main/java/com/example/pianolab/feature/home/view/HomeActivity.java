package com.example.pianolab.feature.home.view;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.databinding.DataBindingUtil;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.GridLayoutManager;
import com.example.pianolab.R;
import com.example.pianolab.databinding.ActivityHomeBinding;
import com.example.pianolab.feature.home.viewmodel.HomeViewModel;
import com.example.pianolab.utils.ImmersiveUiHelper;

/** 主界面：功能选择入口 */
public class HomeActivity extends AppCompatActivity {
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

    initVersionBadge();
    initRecyclerView();
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
