package com.example.pianolab.feature.virtual_piano.viewmodel;
        
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

public class VirtualPianoViewModel extends ViewModel {
    private final MutableLiveData<Boolean> showNoteNames = new MutableLiveData<>(true);
    private final MutableLiveData<Boolean> sustainEnabled = new MutableLiveData<>(false);

    public LiveData<Boolean> getShowNoteNames() {
        return showNoteNames;
    }

    public void setShowNoteNames(boolean show) {
        Boolean current = showNoteNames.getValue();
        if (current != null && current == show) return;
        showNoteNames.setValue(show);
    }
    public LiveData<Boolean> getSustainEnabled() {
        return sustainEnabled;
    }

    public void setSustainEnabled(boolean enabled) {
        Boolean current = sustainEnabled.getValue();
        if (current != null && current == enabled) return;
        sustainEnabled.setValue(enabled);
    }
}