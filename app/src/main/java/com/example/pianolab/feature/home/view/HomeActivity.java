package com.example.pianolab.feature.home.view;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.databinding.DataBindingUtil;
import com.example.pianolab.R;
import com.example.pianolab.databinding.ActivityHomeBinding;
import com.example.pianolab.feature.home.viewmodel.HomeViewModel;


/**
 * 主界面：功能选择入口
 */
public class HomeActivity extends AppCompatActivity {
    private ActivityHomeBinding binding; // DataBinding对象
    private HomeViewModel homeViewModel;
    private FeatureAdapter featureAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // 1. 初始化DataBinding（绑定布局）
        binding = DataBindingUtil.setContentView(this, R.layout.activity_home);

        // 2. 初始化ViewModel
        homeViewModel = new ViewModelProvider(this).get(HomeViewModel.class);

        // 3. 绑定ViewModel到布局，并设置生命周期所有者（让LiveData感知Activity生命周期）
        binding.setViewModel(homeViewModel);
        binding.setLifecycleOwner(this);

        // 4. 初始化RecyclerView适配器
        initRecyclerView();
    }

    // 初始化RecyclerView：观察ViewModel的数据变化，更新列表
    private void initRecyclerView() {
        // 设置布局管理器（网格布局，2列）
        binding.rvFeatures.setLayoutManager(new androidx.recyclerview.widget.GridLayoutManager(this, 2));

        // 观察功能列表数据
        homeViewModel.getFeatureItems().observe(this, featureItems -> {
            if (featureItems != null) {
                // 创建适配器
                featureAdapter = new FeatureAdapter(featureItems, item -> {
                    homeViewModel.onFeatureItemClick(HomeActivity.this, item);
                });
                // 设置适配器到RecyclerView
                binding.rvFeatures.setAdapter(featureAdapter);
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (homeViewModel != null) {
            homeViewModel.refreshTrivia();
        }
    }
}