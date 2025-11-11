package com.example.pianolab.feature.beat.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.pianolab.feature.beat.engine.BeatEngine;
import com.example.pianolab.feature.beat.engine.BeatEngineImpl;
import com.example.pianolab.feature.beat.model.BeatSettings;

/**
 * 简单的 ViewModel：无 UI 要求时用于控制节拍器引擎并暴露状态
 */
public class BeatViewModel extends ViewModel {
    private final MutableLiveData<Boolean> isRunning = new MutableLiveData<>(false);
    private final MutableLiveData<Integer> bpm = new MutableLiveData<>(120);
    private final MutableLiveData<Integer> currentBeatIndex = new MutableLiveData<>(0);

    private final BeatEngine engine;
    private final BeatSettings settings = new BeatSettings();

    public BeatViewModel() {
        engine = new BeatEngineImpl();
        engine.setListener((beatIndex, ts) -> {
            // 从后台线程回调，使用 postValue
            currentBeatIndex.postValue(beatIndex);
            // 每次回调不更新 bpm/live running 状态，这由控制函数负责
        });
        bpm.setValue(settings.getBpm());
    }

    //相比于MutableLivedata，Livedata是只读的，仅供view模块查询所用
    public LiveData<Boolean> getIsRunning() {
        return isRunning;
    }

    public LiveData<Integer> getBpm() {
        return bpm;
    }

    public LiveData<Integer> getCurrentBeatIndex() {
        return currentBeatIndex;
    }

    public void start() {
        engine.start(settings);
        isRunning.postValue(true);
    }

    public void stop() {
        engine.stop();
        isRunning.postValue(false);
    }

    public void toggle() {
        Boolean r = isRunning.getValue();
        if (r != null && r) stop(); else start();
    }

    public void setBpm(int newBpm) {
        settings.setBpm(newBpm);
        bpm.postValue(settings.getBpm());
        engine.setBpm(settings.getBpm());
    }

    public void setBeatsPerMeasure(int beats) {
        settings.setBeatsPerMeasure(beats);
        engine.setBeatsPerMeasure(settings.getBeatsPerMeasure());
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        engine.release();
    }
}

