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
import com.example.pianolab.feature.tuner.view.TunerActivity;
import com.example.pianolab.feature.chord.view.ChordActivity;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;


public class HomeViewModel extends ViewModel {
    // 用LiveData存储功能列表（View层可观察数据变化）
    private final MutableLiveData<List<FeatureItem>> featureItems = new MutableLiveData<>();
    private final MutableLiveData<String> _dailyTrivia = new MutableLiveData<>();
    public LiveData<String> getDailyTrivia() {
        return _dailyTrivia;
    }

    public HomeViewModel() {
        // 初始化功能列表（后期可从本地配置动态加载）
        initFeatureItems();
    }

    // 初始化功能项
    private void initFeatureItems() {
        List<FeatureItem> items = new ArrayList<>();


        items.add(new FeatureItem(
                R.drawable.ic_feature_beat, // 节拍器图标
                "节拍器",
                BeatActivity.class
        ));


        items.add(new FeatureItem(
                R.drawable.ic_feature_piano, // 虚拟钢琴图标
                "虚拟钢琴",
                VirtualPianoActivity.class
        ));

        items.add(new FeatureItem(
                R.drawable.ic_feature_tuner, // 调音器图标
                "调音器",
                TunerActivity.class
        ));

        items.add(new FeatureItem(
                R.drawable.ic_feature_chord, // 和弦工具图标
                "和弦工具",
                ChordActivity.class
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

    private final List<String> triviaDatabase = Arrays.asList(
            "你知道吗？钢琴由巴尔托洛梅奥・克里斯托福里（Bartolomeo Cristofori）于 1700 年左右在意大利发明。",
            "你知道吗？钢琴的 “Piano” 一词源于意大利语 “Pianoforte”，意为 “能强能弱”，因为它是历史上第一种能灵活控制音量大小的键盘乐器。",
            "你知道吗？肖邦专门设计了 “肖邦手位” 练习法，把 2、3、4 指放在三个一组的黑键上，能快速提升手指独立性和掌关节爆发力，是初学者练基本功的高效方法。",
            "你知道吗？贝多芬的《月光奏鸣曲》原名是《升 C 小调钢琴奏鸣曲》。“月光” 源于德国诗人路德维希·雷尔施塔布对其第一乐章的描述，形容其如同瑞士琉森湖上月光闪耀的景象。",
            "你知道吗？立式钢琴的琴弦是垂直排列的，而三角钢琴的琴弦是水平排列的，这也是三角钢琴音色更开阔、共鸣更好的重要原因之一。",
            "你知道吗？车尔尼是贝多芬的得意门生，而李斯特又师从车尔尼，他写的 599、849、299 等练习曲，是钢琴学习者从入门到进阶的 “必经之路”。",
            "你知道吗？德彪西是法国印象派音乐大师，其《大海》《月光》等作品，擅长模仿自然之声，意境独特。",
            "你知道吗？“钢琴之王” 李斯特演奏时力道惊人！曾有一次演出中，他激情澎湃的弹奏竟让钢琴琴弦当场崩断，成为音乐史上的趣味佳话。"
    );

    // 3. 随机抽取一条的方法
    public void refreshTrivia() {
        if (!triviaDatabase.isEmpty()) {
            int index = new Random().nextInt(triviaDatabase.size());
            _dailyTrivia.setValue(triviaDatabase.get(index));
        }
    }
}
