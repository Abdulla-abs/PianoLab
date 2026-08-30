package com.example.pianolab.feature.home.viewmodel;

import android.content.Context;
import android.content.Intent;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import com.example.pianolab.R;
import com.example.pianolab.feature.beat.view.BeatActivity;
import com.example.pianolab.feature.chord.view.ChordActivity;
import com.example.pianolab.feature.home.model.FeatureItem;
import com.example.pianolab.feature.tuner.view.TunerActivity;
import com.example.pianolab.feature.virtual_piano.view.VirtualPianoActivity;
import java.util.ArrayList;
import java.util.List;

public class HomeViewModel extends ViewModel {
  private final MutableLiveData<List<FeatureItem>> featureItems = new MutableLiveData<>();

  public HomeViewModel() {
    initFeatureItems();
  }

  private void initFeatureItems() {
    List<FeatureItem> items = new ArrayList<>();

    items.add(
        new FeatureItem(
            R.drawable.ic_feature_beat,
            R.string.home_feature_metronome,
            R.string.home_feature_metronome_desc,
            R.drawable.bg_feature_card_primary_ripple,
            R.drawable.bg_feature_icon_circle_primary,
            R.color.md_theme_light_onPrimaryFixedVariant,
            BeatActivity.class));

    items.add(
        new FeatureItem(
            R.drawable.ic_feature_piano,
            R.string.home_feature_piano,
            R.string.home_feature_piano_desc,
            R.drawable.bg_feature_card_tertiary_ripple,
            R.drawable.bg_feature_icon_circle_tertiary,
            R.color.md_theme_light_tertiary,
            VirtualPianoActivity.class));

    items.add(
        new FeatureItem(
            R.drawable.ic_feature_tuner,
            R.string.home_feature_tuner,
            R.string.home_feature_tuner_desc,
            R.drawable.bg_feature_card_error_ripple,
            R.drawable.bg_feature_icon_circle_error,
            R.color.md_theme_light_error,
            TunerActivity.class));

    items.add(
        new FeatureItem(
            R.drawable.ic_feature_chord,
            R.string.home_feature_chord,
            R.string.home_feature_chord_desc,
            R.drawable.bg_feature_card_secondary_ripple,
            R.drawable.bg_feature_icon_circle_secondary,
            R.color.md_theme_light_secondary,
            ChordActivity.class));

    featureItems.setValue(items);
  }

  public LiveData<List<FeatureItem>> getFeatureItems() {
    return featureItems;
  }

  public void onFeatureItemClick(Context context, FeatureItem item) {
    if (context == null || item == null) {
      return;
    }
    Intent intent = new Intent(context, item.getTargetCls());
    context.startActivity(intent);
  }
}
