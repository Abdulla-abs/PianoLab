package com.example.pianolab.feature.tuner.viewmodel;

import android.app.Application;
import android.content.res.AssetFileDescriptor;
import android.media.MediaPlayer;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.pianolab.feature.tuner.engine.TarsosAudioEngine;
import com.example.pianolab.feature.tuner.model.TunerState;
import com.example.pianolab.utils.TunerHelper;

import java.util.ArrayList;
import java.util.List;

public class TunerViewModel extends AndroidViewModel implements TarsosAudioEngine.Listener {
    private static final String TAG = "TunerViewModel";
    private static final float DEFAULT_STANDARD = 440f;
    private static final float ALT_STANDARD = 442f;
    private static final float PROBABILITY_THRESHOLD = 0.85f;
    private static final float SAMPLE_RATE = 44100f;
    private static final int BUFFER_SIZE = 1024*4;
    private static final int STABLE_FRAME_COUNT = 5;
    private static final int DELAY_FRAME_COUNT = 3;
    private static final float STABLE_DEVIATION_CENTS = 10f;

    private static final float SILENCE_RMS_RATIO = 0.2f;
    private static final float MIN_SILENCE_RMS = 0.008f;
    private static final int SILENCE_STOP_FRAME_COUNT = 15;

    private int silenceFrameCount = 0;
    private boolean pitchDetectedInSession = false;
    private float sessionPeakRms = 0f;

    private final List<Float> stableFrequencies = new ArrayList<>();
    private int delayCounter = 0;

    private final MutableLiveData<TunerState> tunerState = new MutableLiveData<>(TunerState.idle());
    private final TarsosAudioEngine audioEngine;
    private boolean referencePlaying = false;
    private String manualTargetNote = "A4";
    private float manualTargetFrequency = DEFAULT_STANDARD;
    private boolean listening;
    private boolean frequencyMode = true;

    private MediaPlayer mediaPlayer;
    private final Handler mediaHandler = new Handler(Looper.getMainLooper());
    private Runnable stopPlaybackTask;
    private static final int PLAYBACK_DURATION_MS = 3000;


    public TunerViewModel(@NonNull Application application) {
        super(application);
        audioEngine = new TarsosAudioEngine(SAMPLE_RATE, BUFFER_SIZE, this);
    }

    public LiveData<TunerState> getTunerState() {
        return tunerState;
    }

    public void setWaveDisplayMode(boolean frequencyMode) {
        this.frequencyMode = frequencyMode;
        tunerState.setValue(safeState());
    }

    public boolean isFrequencyMode() {
        return frequencyMode;
    }

    public void onAutoDetectChanged(boolean enabled) {
        TunerState current = safeState();
        tunerState.setValue(current.withAutoDetect(enabled));
        if (!enabled) {
            tunerState.setValue(tunerState.getValue().withManualTarget(manualTargetNote, manualTargetFrequency));
        }
    }

    public void onAutoStopChanged(boolean enabled) {
        tunerState.setValue(safeState().withAutoStop(enabled));
        if (enabled && safeState().isListening()) {
            resetAutoStopState();
        }
    }

    public void onReferenceStandardChanged(boolean use442) {
        float freq = use442 ? 442f : 440f;
        tunerState.setValue(safeState().withstandardFrequency(freq));
        if (!safeState().isAutoDetectEnabled()) {
            setManualTarget(manualTargetNote, freq);
        }
    }

    public void setManualTarget(String note, float standardFreq) {
        this.manualTargetNote = note;
        this.manualTargetFrequency = TunerHelper.calculateNoteFrequency(note, standardFreq);
        tunerState.setValue(safeState().withManualTarget(note, manualTargetFrequency));
    }

    public void toggleListening() {
        stopPlayback();
        TunerState current = safeState();
        boolean newListening = !current.isListening();
        Log.d(TAG, "toggleListening -> " + newListening);
        tunerState.setValue(current.withListening(newListening));
        listening = newListening;
        if (newListening) {
            resetAutoStopState();
            audioEngine.start();
        } else {
            audioEngine.stop();
        }
    }

    public void playNote(boolean mode) {
        stopPlayback();

        String note;
        if (mode) {
            TunerState current = safeState();
            if(current.isAutoDetectEnabled()){
                note = safeState().getDisplayNote();
            }
            else {
                note = safeState().getManualNote();
            }

//            Log.d(TAG, "curr_note"+note);
        } else {
            note = "A4";
        }

        if (note == null || "--".equals(note)) {
            return;
        }

        int resId = TunerHelper.getAudioResourceForDetectedNote(getApplication(), note);
        if (resId == 0) {
            Log.w(TAG, "playNote: no resource for note=" + note);
            return;
        }

        try {
            mediaPlayer = new MediaPlayer();

            // 设置音频资源
            AssetFileDescriptor afd = getApplication().getResources().openRawResourceFd(resId);
            if (afd == null) {
                Log.w(TAG, "playNote: cannot open resource " + resId);
                return;
            }

            mediaPlayer.setDataSource(afd.getFileDescriptor(), afd.getStartOffset(), afd.getLength());
            afd.close();

            // 准备播放
            mediaPlayer.prepare();
            mediaPlayer.start();
            referencePlaying = true;

            Log.d(TAG, "playNote: started playing note=" + note + " resId=" + resId);

            // 3秒后自动停止
            stopPlaybackTask = () -> {
                stopPlayback();
                referencePlaying = false;
            };
            mediaHandler.postDelayed(stopPlaybackTask, PLAYBACK_DURATION_MS);

            // 监听播放完成(如果音频本身小于3秒)
            mediaPlayer.setOnCompletionListener(mp -> {
                Log.d(TAG, "playNote: playback completed");
                stopPlayback();
                referencePlaying = false;
            });

            // 监听错误
            mediaPlayer.setOnErrorListener((mp, what, extra) -> {
                Log.e(TAG, "playNote: MediaPlayer error what=" + what + " extra=" + extra);
                stopPlayback();
                referencePlaying = false;
                return true;
            });

        } catch (Exception e) {
            Log.e(TAG, "playNote: failed to play", e);
            stopPlayback();
            referencePlaying = false;
        }
    }

    private void stopPlayback() {
        // 取消定时停止任务
        if (stopPlaybackTask != null) {
            mediaHandler.removeCallbacks(stopPlaybackTask);
            stopPlaybackTask = null;
        }

        // 释放 MediaPlayer
        if (mediaPlayer != null) {
            try {
                if (mediaPlayer.isPlaying()) {
                    mediaPlayer.stop();
                }
                mediaPlayer.reset();
                mediaPlayer.release();
            } catch (Exception e) {
                Log.w(TAG, "stopPlayback: error releasing MediaPlayer", e);
            } finally {
                mediaPlayer = null;
            }
        }

        referencePlaying = false;
    }

    public boolean isReferencePlaying() {
        return referencePlaying;
    }

    @Override
    public void onCleared() {
        super.onCleared();
        stopPlayback();
        mediaHandler.removeCallbacksAndMessages(null);
        audioEngine.release();
    }

    @Override
    public void onPitch(@NonNull com.example.pianolab.feature.tuner.engine.PitchDetectionResult result) {
        if (!safeState().isListening() || !result.isReliable(PROBABILITY_THRESHOLD)) {
            return;
        }

        float measured = result.getFrequency();
        if (measured <= 0f) {
            return;
        }

        TunerState current = safeState();
        float targetFreq;

        if (current.isAutoDetectEnabled()) {
            float detectedNoteFreq = TunerHelper.calculate_ref_freq(measured, current.getstandardFrequency());
            String note = TunerHelper.PitchNoteMapper(measured,current.getstandardFrequency());
            tunerState.setValue(current
                    .withAutoDetection(note, detectedNoteFreq)
                    .withMeasurement(measured)
                    .withDeviation(TunerHelper.calculate_deviation_cent(measured, detectedNoteFreq)));
        } else {
            targetFreq = current.getManualFrequency();
            tunerState.setValue(current
                    .withMeasurement(measured)
                    .withDeviation(TunerHelper.calculate_deviation_cent(measured, targetFreq)));
        }

        if (current.isAutoStopEnabled()) {
            pitchDetectedInSession = true;
        }
    }

    @Override
    public void onError(@NonNull Throwable throwable) {
        Log.e(TAG, "Audio engine error", throwable);
        TunerState current = safeState();
        listening = false;
        tunerState.postValue(current.withListening(false));
    }

    @Override
    public void onSpectrum(@NonNull float[] magnitudes) {
        if (!safeState().isListening()) {
            return;
        }
        List<Float> data = new ArrayList<>(magnitudes.length);
        for (float value : magnitudes) {
            data.add(value);
        }
        tunerState.setValue(safeState().withSpectrum(data));
    }

    @Override
    public void onWaveform(@NonNull float[] samples) {
        if (!safeState().isListening()) {
            return;
        }
        List<Float> waveform = new ArrayList<>(samples.length);
        for (float sample : samples) {
            waveform.add(sample);
        }
        tunerState.setValue(safeState().withWaveform(waveform));

        if (safeState().isAutoStopEnabled()) {
            processSilenceAutoStop(TunerHelper.calculate_RMS(samples));
        }
    }

    private void processSilenceAutoStop(float rms) {
        if (!pitchDetectedInSession || !safeState().isListening()) {
            return;
        }

        if (rms > sessionPeakRms) {
            sessionPeakRms = rms;
        }

        float silenceThreshold = Math.max(MIN_SILENCE_RMS, sessionPeakRms * SILENCE_RMS_RATIO);
        if (rms < silenceThreshold) {
            silenceFrameCount++;
            if (silenceFrameCount >= SILENCE_STOP_FRAME_COUNT) {
                Log.d(TAG, "Silence auto-stop triggered after " + silenceFrameCount + " frames");
                stopListeningIfActive();
            }
        } else {
            silenceFrameCount = 0;
        }
    }

    private void stopListeningIfActive() {
        if (!safeState().isListening()) {
            return;
        }
        stopPlayback();
        listening = false;
        tunerState.setValue(safeState().withListening(false));
        audioEngine.stop();
    }

    private void processAutoStop(float measured, float targetFreq) {
        float deviation = Math.abs(TunerHelper.calculate_deviation_cent(measured, targetFreq));

        if (deviation <= STABLE_DEVIATION_CENTS) {
            stableFrequencies.add(measured);
            if (stableFrequencies.size() >= STABLE_FRAME_COUNT) {
                delayCounter++;
                if (delayCounter >= DELAY_FRAME_COUNT) {
                    Log.d(TAG, "Auto-stop triggered: stopping listening");
                    toggleListening();
                }
            }
        } else {
            resetAutoStopState();
        }
    }

    private void resetAutoStopState() {
        silenceFrameCount = 0;
        pitchDetectedInSession = false;
        sessionPeakRms = 0f;
        stableFrequencies.clear();
        delayCounter = 0;
    }

    private TunerState safeState() {
        TunerState current = tunerState.getValue();
        if (current == null) {
            current = TunerState.idle();
            tunerState.setValue(current);
        }
        return current;
    }

    public boolean isListening() { return listening; }
    public void setListening(boolean listening) { this.listening = listening; }
}
