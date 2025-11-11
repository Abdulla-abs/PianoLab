package com.example.pianolab.feature.home.view;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.databinding.DataBindingUtil;
import com.example.pianolab.R;
import com.example.pianolab.databinding.ActivityHomeBinding;
import com.example.pianolab.feature.home.model.FeatureItem;
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

        // 4. 初始化GridView适配器
        initGridView();
    }

    // 初始化GridView：观察ViewModel的数据变化，更新列表
    private void initGridView() {
        // 观察功能列表数据
        homeViewModel.getFeatureItems().observe(this, featureItems -> {
            if (featureItems != null) {
                // 创建适配器（参数：上下文、item布局、数据列表）
                featureAdapter = new FeatureAdapter(
                        HomeActivity.this,
                        R.layout.item_feature, // 单个功能项的布局
                        featureItems
                );
                // 设置适配器到GridView
                binding.gvFeatures.setAdapter(featureAdapter);
            }
        });

        // 绑定GridView的点击事件（点击item时调用ViewModel的方法）
        binding.gvFeatures.setOnItemClickListener((parent, view, position, id) -> {
            if (featureAdapter != null) {
                FeatureItem item = featureAdapter.getItem(position);
                homeViewModel.onFeatureItemClick(HomeActivity.this, item);
            }
        });
    }
}