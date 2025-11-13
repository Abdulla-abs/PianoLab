package com.example.pianolab.feature.beat.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.pianolab.feature.beat.engine.BeatEngine;
import com.example.pianolab.feature.beat.engine.BeatEngineImpl;
import com.example.pianolab.feature.beat.model.BeatSettings;

/**
 * 简单的 ViewModel：无 UI 要求时用于控制节拍器引擎并暴露状态
 */
public class BeatViewModel extends AndroidViewModel {

    private final BeatEngineImpl engine;
    private final MutableLiveData<Integer> bpm = new MutableLiveData<>();
    private final MutableLiveData<Integer> beatsPerMeasure = new MutableLiveData<>();
    private final MutableLiveData<Boolean> running = new MutableLiveData<>(false);
    private final MutableLiveData<String> status = new MutableLiveData<>("stopped");

    // 新增：当前拍索引（0-based）
    private final MutableLiveData<Integer> currentBeatIndex = new MutableLiveData<>(0);

    public BeatViewModel(@NonNull Application application) {
        super(application);
        engine = new BeatEngineImpl(application);
        bpm.setValue(120);
        beatsPerMeasure.setValue(4);

        engine.setListener(new BeatEngine.BeatListener() {
            @Override
            public void onBeat(int beatIndex, long timestampNs) {
                // 更新当前拍索引并更新状态文本
                currentBeatIndex.postValue(beatIndex);
                status.postValue("Beat: " + beatIndex);
            }

            @Override
            public void onMeasureStart(long timestampNs) {
                status.postValue("Measure start");
            }
        });
    }

    public LiveData<Integer> getBpm() {
        return bpm;
    }

    public LiveData<Integer> getBeatsPerMeasure() {
        return beatsPerMeasure;
    }



    public LiveData<Boolean> getIsRunning() { return running; }

    public LiveData<String> getStatus() { return status; }


    public LiveData<Integer> getCurrentBeatIndex() { return currentBeatIndex; }

    public void setBpm(int value) {
        if (value < 1) value = 1;
        bpm.setValue(value);
        engine.setBpm(value);
    }

    public void setBeatsPerMeasure(int value) {
        if (value <= 0) value = 4;
        beatsPerMeasure.setValue(value);
        engine.setBeatsPerMeasure(value);
    }

    public void start() {
        BeatSettings s = new BeatSettings(bpm.getValue() != null ? bpm.getValue() : 120,
                beatsPerMeasure.getValue() != null ? beatsPerMeasure.getValue() : 4);
        engine.start(s);
        running.setValue(true);
        status.setValue("running");
    }

    public void stop() {
        engine.stop();
        running.setValue(false);
        status.setValue("stopped");
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        engine.release();
    }
}
