package com.example.pianolab.feature.home.viewmodel;

import android.content.Context;
import android.content.Intent;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.pianolab.feature.home.model.FeatureItem;
import java.util.ArrayList;
import java.util.List;


public class HomeViewModel extends ViewModel {
    // 用LiveData存储功能列表（View层可观察数据变化）
    private final MutableLiveData<List<FeatureItem>> featureItems = new MutableLiveData<>();

    public HomeViewModel() {
        // 初始化功能列表（后期可从本地配置动态加载）
        initFeatureItems();
    }

    // 初始化功能项
    private void initFeatureItems() {
        List<FeatureItem> items = new ArrayList<>();

        //TODO 完成对应模块后须添加相应代码
//        // 添加节拍器（已实现）
//        items.add(new FeatureItem(
//                "节拍器",
//                com.example.pianolab.R.drawable.ic_metronome, // 图标资源
//                BeatActivity.class
//        ));
//        // 添加频率分析（后期实现）
//        items.add(new FeatureItem(
//                "频率分析",
//                com.example.pianolab.R.drawable.ic_frequency,
//                FrequencyActivity.class
//        ));
//        // 添加和弦训练（后期实现）
//        items.add(new FeatureItem(
//                "和弦训练",
//                com.example.pianolab.R.drawable.ic_chord,
//                ChordActivity.class
//        ));
        featureItems.setValue(items);
    }


    public LiveData<List<FeatureItem>> getFeatureItems() {
        return featureItems;
    }

    // 点击后跳转到目标Activity
    public void onFeatureItemClick(Context context, FeatureItem item) {
        if (context == null || item == null) return;
        Intent intent = new Intent(context, item.getTargetCls());
        context.startActivity(intent);
    }
}
