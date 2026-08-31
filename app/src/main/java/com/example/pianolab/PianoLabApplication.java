package com.example.pianolab;

import android.app.Application;
import com.example.pianolab.utils.ThemeManager;

public class PianoLabApplication extends Application {

  @Override
  public void onCreate() {
    super.onCreate();
    ThemeManager.init(this);
  }
}
