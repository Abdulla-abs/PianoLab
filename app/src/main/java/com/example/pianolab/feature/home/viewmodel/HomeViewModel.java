package com.example.pianolab.feature.home.viewmodel;

import android.content.Context;
import android.content.Intent;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.pianolab.R;
import com.example.pianolab.feature.home.model.FeatureItem;
import com.example.pianolab.feature.beat.view.BeatActivity;
import com.example.pianolab.feature.virtual_piano.view.VirtualPianoActivity;
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

        // 简化的 demo：仅添加节拍器功能（第一个item）
        items.add(new FeatureItem(
                R.mipmap.ic_launcher, // 临时使用 launcher 图标，后续替换为专业图标
                "节拍器",
                BeatActivity.class
        ));

        // 添加虚拟钢琴入口
        items.add(new FeatureItem(
                R.mipmap.ic_launcher,
                "虚拟钢琴",
                VirtualPianoActivity.class
        ));

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
