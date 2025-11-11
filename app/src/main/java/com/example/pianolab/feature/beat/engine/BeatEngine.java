package com.example.pianolab.feature.beat.engine;

import com.example.pianolab.feature.beat.model.BeatSettings;

/**
 * 节拍器引擎接口：负责节拍调度并通过监听器回调节拍事件。
 */
public interface BeatEngine {
    interface BeatListener {
        /**
         * called on each beat; beatIndex 从 0 开始，代表当前小节内的拍序号
         * timestampNs 为 System.nanoTime() 的时间戳
         */
        void onBeat(int beatIndex, long timestampNs);

        /**
         * called when a measure (小节) starts (beatIndex == 0)
         */
        default void onMeasureStart(long timestampNs) {}
    }

    void start(BeatSettings settings);

    void stop();

    void setBpm(int bpm);

    void setBeatsPerMeasure(int beatsPerMeasure);

    void setListener(BeatListener listener);

    boolean isRunning();

    void release();
}

