package com.example.pianolab.ui;

import androidx.appcompat.app.AppCompatActivity;

/** 所有 Activity 的基类，确保在创建前已应用用户选择的外观模式。 */
public abstract class BaseActivity extends AppCompatActivity {
  // ThemeManager 通过 Application 在进程启动时初始化，Activity 无需额外 setTheme。
}
