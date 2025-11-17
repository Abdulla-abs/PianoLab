package com.example.pianolab.feature.beat.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.pianolab.feature.beat.engine.BeatEngine;
import com.example.pianolab.feature.beat.engine.BeatEngineImpl;
import com.example.pianolab.feature.beat.model.BeatSettings;
import com.example.pianolab.utils.BeatHelper;

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
    // 引擎实际运行状态（只有在 engine.start() 完成后为 true）
    private final MutableLiveData<Boolean> engineRunning = new MutableLiveData<>(false);

    // 新增：baseBeat（以几分音符为一拍，例如 4 表示四分音符）
    private final MutableLiveData<Integer> baseBeat = new MutableLiveData<>(4);

    // 用于实现启动前的倒计时（3 秒）
    private final android.os.Handler handler = new android.os.Handler(android.os.Looper.getMainLooper());
    private Runnable startRunnable;

    public BeatViewModel(@NonNull Application application) {
        super(application);
        engine = new BeatEngineImpl(application);
        bpm.setValue(120);
        beatsPerMeasure.setValue(4);
        baseBeat.setValue(4);

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

    public LiveData<Boolean> getEngineRunning() { return engineRunning; }

    public LiveData<Integer> getBaseBeat() { return baseBeat; }

    public void setBpm(int value) {

        value = BeatHelper.clampBPM(value);

        bpm.setValue(value);
        engine.setBpm(value);
    }

    //measure 小节，表示每小节几拍
    public void setBeatsPerMeasure(int value) {
        if (value <= 0) value = 4;
        beatsPerMeasure.setValue(value);
        engine.setBeatsPerMeasure(value);
    }

    public void setBaseBeat(int value) {
        if (value != 4 && value != 8 && value != 2) {
            // 仅允许 2/4/8 等常见值，其他默认为4
            value = 4;
        }
        baseBeat.setValue(value);
        // 当前 engine 实现不使用 baseBeat，但保存在 settings 以备将来使用
    }

    public void start() {
        // 如果已经在倒计时或运行中，忽略重复 start
        Boolean alreadyRunning = running.getValue();
        if (alreadyRunning != null && alreadyRunning) return;

        BeatSettings s = new BeatSettings(bpm.getValue() != null ? bpm.getValue() : 120,
                beatsPerMeasure.getValue() != null ? beatsPerMeasure.getValue() : 4);

        // 标记为正在准备启动（UI 可显示为已切换），实际 engine 在倒计时结束后启动
        running.setValue(true);
        status.setValue("starting");

        // 取消前一个可能存在的 runnable
        if (startRunnable != null) handler.removeCallbacks(startRunnable);
        startRunnable = () -> {
            engine.start(s);
            engineRunning.postValue(true);
            status.postValue("running");
        };
        // 倒计时 3 秒
        handler.postDelayed(startRunnable, 3000L);
    }

    public void stop() {
        // 取消未执行的启动任务
        if (startRunnable != null) {
            handler.removeCallbacks(startRunnable);
            startRunnable = null;
        }
        // 停止引擎（如果已在运行）
        if (engine != null && engine.isRunning()) {
            engine.stop();
        }
        engineRunning.setValue(false);
        running.setValue(false);
        status.setValue("stopped");
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        engine.release();
    }
}
