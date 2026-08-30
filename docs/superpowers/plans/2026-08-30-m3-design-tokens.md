# M3 Design Tokens (Phase A) Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Establish Material 3 design tokens (colors, typography, shapes, widget defaults) with dual-theme support, without changing layout structure.

**Architecture:** Modular XML resources under `res/values/`; `Theme.PianoLab` (light M3) for Home/Beat; `Theme.PianoLab.Tool` (dark M3) for Tuner/Chord/VirtualPiano. Legacy color aliases preserve existing layout compatibility.

**Tech Stack:** Android Views, DataBinding, Material Components 1.12.0, Google Fonts Downloadable Font

## Global Constraints

- Material library version: `1.12.0`
- No Compose migration
- No layout XML structure changes in Phase A
- Color values copied verbatim from `design/DESIGN.md` and `docs/superpowers/specs/2026-08-30-m3-design-tokens-design.md`
- Tool activities keep `ImmersiveUiHelper` behavior unchanged
- `minSdk = 30`, `compileSdk = 36`

---

### Task 1: Upgrade Material Dependency

**Files:**
- Modify: `gradle/libs.versions.toml`

**Interfaces:**
- Produces: `material` catalog entry at version `1.12.0`

- [ ] **Step 1: Update version**

In `gradle/libs.versions.toml`, change:
```toml
material = "1.12.0"
```

- [ ] **Step 2: Sync and verify**

Run: `./gradlew :app:dependencies --configuration releaseRuntimeClasspath 2>&1 | findstr material`
Expected: `com.google.android.material:material:1.12.0`

---

### Task 2: Light M3 Color Tokens

**Files:**
- Modify: `app/src/main/res/values/colors.xml` (full rewrite)

**Interfaces:**
- Produces: `md_theme_light_*` color resources and legacy alias mappings

- [ ] **Step 1: Write colors.xml**

Replace `colors.xml` with all M3 light roles. Key entries:

```xml
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <!-- M3 Light Color Roles (from design/DESIGN.md) -->
    <color name="md_theme_light_primary">#005bbf</color>
    <color name="md_theme_light_onPrimary">#ffffff</color>
    <color name="md_theme_light_primaryContainer">#1a73e8</color>
    <color name="md_theme_light_onPrimaryContainer">#ffffff</color>
    <color name="md_theme_light_inversePrimary">#adc7ff</color>
    <color name="md_theme_light_secondary">#5b5f64</color>
    <color name="md_theme_light_onSecondary">#ffffff</color>
    <color name="md_theme_light_secondaryContainer">#dde0e6</color>
    <color name="md_theme_light_onSecondaryContainer">#5f6368</color>
    <color name="md_theme_light_tertiary">#006875</color>
    <color name="md_theme_light_onTertiary">#ffffff</color>
    <color name="md_theme_light_tertiaryContainer">#008394</color>
    <color name="md_theme_light_onTertiaryContainer">#000608</color>
    <color name="md_theme_light_error">#ba1a1a</color>
    <color name="md_theme_light_onError">#ffffff</color>
    <color name="md_theme_light_errorContainer">#ffdad6</color>
    <color name="md_theme_light_onErrorContainer">#93000a</color>
    <color name="md_theme_light_background">#f8f9fa</color>
    <color name="md_theme_light_onBackground">#191c1d</color>
    <color name="md_theme_light_surface">#f8f9fa</color>
    <color name="md_theme_light_onSurface">#191c1d</color>
    <color name="md_theme_light_surfaceVariant">#e1e3e4</color>
    <color name="md_theme_light_onSurfaceVariant">#414754</color>
    <color name="md_theme_light_surfaceDim">#d9dadb</color>
    <color name="md_theme_light_surfaceBright">#f8f9fa</color>
    <color name="md_theme_light_surfaceContainerLowest">#ffffff</color>
    <color name="md_theme_light_surfaceContainerLow">#f3f4f5</color>
    <color name="md_theme_light_surfaceContainer">#edeeef</color>
    <color name="md_theme_light_surfaceContainerHigh">#e7e8e9</color>
    <color name="md_theme_light_surfaceContainerHighest">#e1e3e4</color>
    <color name="md_theme_light_inverseSurface">#2e3132</color>
    <color name="md_theme_light_inverseOnSurface">#f0f1f2</color>
    <color name="md_theme_light_outline">#727785</color>
    <color name="md_theme_light_outlineVariant">#c1c6d6</color>
    <color name="md_theme_light_surfaceTint">#005bc0</color>
    <color name="md_theme_light_primaryFixed">#d8e2ff</color>
    <color name="md_theme_light_primaryFixedDim">#adc7ff</color>
    <color name="md_theme_light_onPrimaryFixed">#001a41</color>
    <color name="md_theme_light_onPrimaryFixedVariant">#004493</color>
    <color name="md_theme_light_secondaryFixed">#dfe3e8</color>
    <color name="md_theme_light_secondaryFixedDim">#c3c7cc</color>
    <color name="md_theme_light_onSecondaryFixed">#181c20</color>
    <color name="md_theme_light_onSecondaryFixedVariant">#43474c</color>
    <color name="md_theme_light_tertiaryFixed">#9fefff</color>
    <color name="md_theme_light_tertiaryFixedDim">#50d7ee</color>
    <color name="md_theme_light_onTertiaryFixed">#001f24</color>
    <color name="md_theme_light_onTertiaryFixedVariant">#004e59</color>

    <!-- Legacy aliases (backward compatibility) -->
    <color name="piano_primary">@color/md_theme_light_primaryContainer</color>
    <color name="piano_primary_light">@color/md_theme_light_primaryFixedDim</color>
    <color name="piano_primary_dark">@color/md_theme_light_primary</color>
    <color name="piano_accent">@color/md_theme_light_tertiaryContainer</color>
    <color name="piano_accent_light">@color/md_theme_light_tertiaryFixedDim</color>
    <color name="piano_background">@color/md_theme_light_background</color>
    <color name="piano_surface">@color/md_theme_light_surfaceContainerLowest</color>
    <color name="text_primary">@color/md_theme_light_onSurface</color>
    <color name="text_secondary">@color/md_theme_light_onSurfaceVariant</color>
    <color name="text_on_primary">@color/md_theme_light_onPrimary</color>

    <!-- Functional colors (unchanged) -->
    <color name="black">#FF000000</color>
    <color name="white">#FFFFFFFF</color>
    <color name="tuner_cursor_green">#4CAF50</color>
    <color name="tuner_cursor_yellow">#FFC107</color>
    <color name="tuner_cursor_red">#F44336</color>

    <!-- Legacy template aliases -->
    <color name="purple_200">@color/md_theme_light_tertiaryFixedDim</color>
    <color name="purple_500">@color/md_theme_light_primaryContainer</color>
    <color name="purple_700">@color/md_theme_light_primary</color>
    <color name="teal_200">@color/md_theme_light_tertiaryFixedDim</color>
    <color name="teal_700">@color/md_theme_light_primary</color>
    <color name="piano_black">@color/md_theme_light_onSurface</color>
    <color name="piano_dark_gray">@color/md_theme_light_surfaceContainerLowest</color>
    <color name="piano_light_gray">@color/md_theme_light_background</color>
    <color name="elegant_gold">@color/md_theme_light_tertiaryContainer</color>
    <color name="elegant_gold_dark">@color/md_theme_light_tertiaryContainer</color>
</resources>
```

- [ ] **Step 2: Verify resource linking**

Run: `./gradlew :app:assembleDebug`
Expected: BUILD SUCCESSFUL

---

### Task 3: Tool Dark Color Tokens

**Files:**
- Create: `app/src/main/res/values/colors_tool.xml`

**Interfaces:**
- Produces: `md_theme_tool_*` color resources

- [ ] **Step 1: Write colors_tool.xml**

```xml
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <color name="md_theme_tool_primary">#adc7ff</color>
    <color name="md_theme_tool_onPrimary">#001a41</color>
    <color name="md_theme_tool_primaryContainer">#1a73e8</color>
    <color name="md_theme_tool_onPrimaryContainer">#ffffff</color>
    <color name="md_theme_tool_secondary">#c3c7cc</color>
    <color name="md_theme_tool_onSecondary">#181c20</color>
    <color name="md_theme_tool_secondaryContainer">#43474c</color>
    <color name="md_theme_tool_onSecondaryContainer">#dfe3e8</color>
    <color name="md_theme_tool_tertiary">#50d7ee</color>
    <color name="md_theme_tool_onTertiary">#001f24</color>
    <color name="md_theme_tool_tertiaryContainer">#008394</color>
    <color name="md_theme_tool_onTertiaryContainer">#9fefff</color>
    <color name="md_theme_tool_error">#ffb4ab</color>
    <color name="md_theme_tool_onError">#690005</color>
    <color name="md_theme_tool_background">#1a1c1e</color>
    <color name="md_theme_tool_onBackground">#f0f1f2</color>
    <color name="md_theme_tool_surface">#1a1c1e</color>
    <color name="md_theme_tool_onSurface">#f0f1f2</color>
    <color name="md_theme_tool_surfaceVariant">#414754</color>
    <color name="md_theme_tool_onSurfaceVariant">#c1c6d6</color>
    <color name="md_theme_tool_surfaceContainer">#2e3132</color>
    <color name="md_theme_tool_surfaceContainerHigh">#383b3c</color>
    <color name="md_theme_tool_surfaceContainerHighest">#434547</color>
    <color name="md_theme_tool_outline">#727785</color>
    <color name="md_theme_tool_outlineVariant">#414754</color>
    <color name="md_theme_tool_surfaceTint">#adc7ff</color>
</resources>
```

---

### Task 4: Typography, Shapes, and Dimens

**Files:**
- Create: `app/src/main/res/font/roboto_flex.xml`
- Create: `app/src/main/res/values/typography.xml`
- Create: `app/src/main/res/values/shapes.xml`
- Create: `app/src/main/res/values/dimens.xml`

**Interfaces:**
- Produces: `TextAppearance.PianoLab.*`, `ShapeAppearance.PianoLab.*`, spacing dimens

- [ ] **Step 1: Create font provider**

`app/src/main/res/font/roboto_flex.xml`:
```xml
<?xml version="1.0" encoding="utf-8"?>
<font-family xmlns:app="http://schemas.android.com/apk/res-auto"
    app:fontProviderAuthority="com.google.android.gms.fonts"
    app:fontProviderPackage="com.google.android.gms"
    app:fontProviderQuery="Roboto Flex"
    app:fontProviderCerts="@array/com_google_android_gms_fonts_certs" />
```

Note: If cert array is missing, use bundled approach or `android:fontFamily="sans-serif"` fallback in typography styles.

- [ ] **Step 2: Create typography.xml**

```xml
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <style name="TextAppearance.PianoLab.DisplayLarge" parent="TextAppearance.Material3.DisplayLarge">
        <item name="android:fontFamily">@font/roboto_flex</item>
        <item name="android:textSize">57sp</item>
        <item name="android:lineHeight">64sp</item>
        <item name="android:letterSpacing">-0.0044</item>
    </style>
    <style name="TextAppearance.PianoLab.DisplayMedium" parent="TextAppearance.Material3.DisplayMedium">
        <item name="android:fontFamily">@font/roboto_flex</item>
        <item name="android:textSize">45sp</item>
        <item name="android:lineHeight">52sp</item>
    </style>
    <style name="TextAppearance.PianoLab.HeadlineLarge" parent="TextAppearance.Material3.HeadlineLarge">
        <item name="android:fontFamily">@font/roboto_flex</item>
        <item name="android:textSize">28sp</item>
        <item name="android:lineHeight">36sp</item>
    </style>
    <style name="TextAppearance.PianoLab.TitleLarge" parent="TextAppearance.Material3.TitleLarge">
        <item name="android:fontFamily">@font/roboto_flex</item>
        <item name="android:textSize">22sp</item>
        <item name="android:textStyle">bold</item>
        <item name="android:lineHeight">28sp</item>
    </style>
    <style name="TextAppearance.PianoLab.BodyLarge" parent="TextAppearance.Material3.BodyLarge">
        <item name="android:fontFamily">@font/roboto_flex</item>
        <item name="android:textSize">16sp</item>
        <item name="android:lineHeight">24sp</item>
        <item name="android:letterSpacing">0.03125</item>
    </style>
    <style name="TextAppearance.PianoLab.BodyMedium" parent="TextAppearance.Material3.BodyMedium">
        <item name="android:fontFamily">@font/roboto_flex</item>
        <item name="android:textSize">14sp</item>
        <item name="android:lineHeight">20sp</item>
        <item name="android:letterSpacing">0.01786</item>
    </style>
    <style name="TextAppearance.PianoLab.LabelLarge" parent="TextAppearance.Material3.LabelLarge">
        <item name="android:fontFamily">@font/roboto_flex</item>
        <item name="android:textSize">14sp</item>
        <item name="android:textStyle">bold</item>
        <item name="android:lineHeight">20sp</item>
        <item name="android:letterSpacing">0.00714</item>
    </style>
    <style name="TextAppearance.PianoLab.LabelSmall" parent="TextAppearance.Material3.LabelSmall">
        <item name="android:fontFamily">@font/roboto_flex</item>
        <item name="android:textSize">11sp</item>
        <item name="android:textStyle">bold</item>
        <item name="android:lineHeight">16sp</item>
        <item name="android:letterSpacing">0.04545</item>
    </style>
</resources>
```

- [ ] **Step 3: Create shapes.xml**

```xml
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <style name="ShapeAppearance.PianoLab.LargeComponent" parent="ShapeAppearance.Material3.LargeComponent">
        <item name="cornerFamily">rounded</item>
        <item name="cornerSize">24dp</item>
    </style>
    <style name="ShapeAppearance.PianoLab.MediumComponent" parent="ShapeAppearance.Material3.MediumComponent">
        <item name="cornerFamily">rounded</item>
        <item name="cornerSize">12dp</item>
    </style>
    <style name="ShapeAppearance.PianoLab.SmallComponent" parent="ShapeAppearance.Material3.SmallComponent">
        <item name="cornerFamily">rounded</item>
        <item name="cornerSize">50%</item>
    </style>
    <style name="ShapeAppearance.PianoLab.ExtraSmall" parent="ShapeAppearance.Material3.ExtraSmall">
        <item name="cornerFamily">rounded</item>
        <item name="cornerSize">4dp</item>
    </style>
</resources>
```

- [ ] **Step 4: Create dimens.xml**

```xml
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <dimen name="spacing_unit">8dp</dimen>
    <dimen name="spacing_gutter">16dp</dimen>
    <dimen name="spacing_margin_mobile">16dp</dimen>
    <dimen name="spacing_margin_desktop">24dp</dimen>
    <dimen name="card_corner_radius">24dp</dimen>
    <dimen name="button_corner_radius">999dp</dimen>
</resources>
```

---

### Task 5: Widget Default Styles

**Files:**
- Create: `app/src/main/res/values/styles_widgets.xml`

**Interfaces:**
- Produces: `Widget.PianoLab.CardView`, `Widget.PianoLab.Button`

- [ ] **Step 1: Write styles_widgets.xml**

```xml
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <style name="Widget.PianoLab.CardView" parent="Widget.Material3.CardView.Elevated">
        <item name="cardCornerRadius">@dimen/card_corner_radius</item>
        <item name="cardElevation">0dp</item>
        <item name="strokeWidth">0dp</item>
        <item name="cardBackgroundColor">?attr/colorSurfaceContainerLow</item>
        <item name="shapeAppearance">@style/ShapeAppearance.PianoLab.LargeComponent</item>
    </style>

    <style name="Widget.PianoLab.Button" parent="Widget.Material3.Button">
        <item name="cornerRadius">@dimen/button_corner_radius</item>
        <item name="backgroundTint">?attr/colorPrimaryContainer</item>
        <item name="android:textColor">?attr/colorOnPrimaryContainer</item>
        <item name="shapeAppearance">@style/ShapeAppearance.PianoLab.SmallComponent</item>
    </style>
</resources>
```

---

### Task 6: Light Theme (Theme.PianoLab)

**Files:**
- Modify: `app/src/main/res/values/themes.xml` (full rewrite)
- Modify: `app/src/main/res/values-night/themes.xml`

**Interfaces:**
- Produces: `Theme.PianoLab` style referencing all M3 color roles

- [ ] **Step 1: Rewrite values/themes.xml**

```xml
<resources>
    <style name="Theme.PianoLab" parent="Theme.Material3.Light.NoActionBar">
        <!-- Primary -->
        <item name="colorPrimary">@color/md_theme_light_primary</item>
        <item name="colorOnPrimary">@color/md_theme_light_onPrimary</item>
        <item name="colorPrimaryContainer">@color/md_theme_light_primaryContainer</item>
        <item name="colorOnPrimaryContainer">@color/md_theme_light_onPrimaryContainer</item>
        <item name="colorPrimaryFixed">@color/md_theme_light_primaryFixed</item>
        <item name="colorPrimaryFixedDim">@color/md_theme_light_primaryFixedDim</item>
        <item name="colorOnPrimaryFixed">@color/md_theme_light_onPrimaryFixed</item>
        <item name="colorOnPrimaryFixedVariant">@color/md_theme_light_onPrimaryFixedVariant</item>
        <!-- Secondary -->
        <item name="colorSecondary">@color/md_theme_light_secondary</item>
        <item name="colorOnSecondary">@color/md_theme_light_onSecondary</item>
        <item name="colorSecondaryContainer">@color/md_theme_light_secondaryContainer</item>
        <item name="colorOnSecondaryContainer">@color/md_theme_light_onSecondaryContainer</item>
        <!-- Tertiary -->
        <item name="colorTertiary">@color/md_theme_light_tertiary</item>
        <item name="colorOnTertiary">@color/md_theme_light_onTertiary</item>
        <item name="colorTertiaryContainer">@color/md_theme_light_tertiaryContainer</item>
        <item name="colorOnTertiaryContainer">@color/md_theme_light_onTertiaryContainer</item>
        <!-- Error -->
        <item name="colorError">@color/md_theme_light_error</item>
        <item name="colorOnError">@color/md_theme_light_onError</item>
        <item name="colorErrorContainer">@color/md_theme_light_errorContainer</item>
        <item name="colorOnErrorContainer">@color/md_theme_light_onErrorContainer</item>
        <!-- Surface -->
        <item name="colorSurface">@color/md_theme_light_surface</item>
        <item name="colorOnSurface">@color/md_theme_light_onSurface</item>
        <item name="colorSurfaceVariant">@color/md_theme_light_surfaceVariant</item>
        <item name="colorOnSurfaceVariant">@color/md_theme_light_onSurfaceVariant</item>
        <item name="colorSurfaceDim">@color/md_theme_light_surfaceDim</item>
        <item name="colorSurfaceBright">@color/md_theme_light_surfaceBright</item>
        <item name="colorSurfaceContainerLowest">@color/md_theme_light_surfaceContainerLowest</item>
        <item name="colorSurfaceContainerLow">@color/md_theme_light_surfaceContainerLow</item>
        <item name="colorSurfaceContainer">@color/md_theme_light_surfaceContainer</item>
        <item name="colorSurfaceContainerHigh">@color/md_theme_light_surfaceContainerHigh</item>
        <item name="colorSurfaceContainerHighest">@color/md_theme_light_surfaceContainerHighest</item>
        <item name="colorSurfaceTint">@color/md_theme_light_surfaceTint</item>
        <item name="colorOutline">@color/md_theme_light_outline</item>
        <item name="colorOutlineVariant">@color/md_theme_light_outlineVariant</item>
        <!-- Background -->
        <item name="android:colorBackground">@color/md_theme_light_background</item>
        <item name="colorOnBackground">@color/md_theme_light_onBackground</item>
        <!-- Inverse -->
        <item name="colorSurfaceInverse">@color/md_theme_light_inverseSurface</item>
        <item name="colorOnSurfaceInverse">@color/md_theme_light_inverseOnSurface</item>
        <item name="colorPrimaryInverse">@color/md_theme_light_inversePrimary</item>
        <!-- System bars -->
        <item name="android:statusBarColor">@color/md_theme_light_surface</item>
        <item name="android:navigationBarColor">@color/md_theme_light_surface</item>
        <item name="android:windowLightStatusBar">true</item>
        <item name="android:windowLightNavigationBar">true</item>
        <item name="android:windowBackground">@color/md_theme_light_background</item>
        <!-- Typography defaults -->
        <item name="textAppearanceDisplayLarge">@style/TextAppearance.PianoLab.DisplayLarge</item>
        <item name="textAppearanceHeadlineLarge">@style/TextAppearance.PianoLab.HeadlineLarge</item>
        <item name="textAppearanceTitleLarge">@style/TextAppearance.PianoLab.TitleLarge</item>
        <item name="textAppearanceBodyLarge">@style/TextAppearance.PianoLab.BodyLarge</item>
        <item name="textAppearanceBodyMedium">@style/TextAppearance.PianoLab.BodyMedium</item>
        <item name="textAppearanceLabelLarge">@style/TextAppearance.PianoLab.LabelLarge</item>
        <item name="textAppearanceLabelSmall">@style/TextAppearance.PianoLab.LabelSmall</item>
        <!-- Shape defaults -->
        <item name="shapeAppearanceCornerLarge">@style/ShapeAppearance.PianoLab.LargeComponent</item>
        <item name="shapeAppearanceCornerMedium">@style/ShapeAppearance.PianoLab.MediumComponent</item>
        <item name="shapeAppearanceCornerSmall">@style/ShapeAppearance.PianoLab.SmallComponent</item>
        <item name="shapeAppearanceCornerExtraSmall">@style/ShapeAppearance.PianoLab.ExtraSmall</item>
        <!-- Widget defaults -->
        <item name="materialCardViewStyle">@style/Widget.PianoLab.CardView</item>
        <item name="materialButtonStyle">@style/Widget.PianoLab.Button</item>
    </style>
</resources>
```

- [ ] **Step 2: Update values-night/themes.xml**

Set parent to `Theme.Material3.DayNight.NoActionBar` with same color mappings (inherits light tokens; system dark mode gets M3 structure). Minimal change:

```xml
<resources>
    <style name="Theme.PianoLab" parent="Theme.Material3.DayNight.NoActionBar">
        <!-- Inherit all items from values/themes.xml via same structure,
             or reference Theme.PianoLab as parent if only status bar differs -->
    </style>
</resources>
```

Simplest approach: make `values-night/themes.xml` identical to `values/themes.xml` for Phase A (light tokens always apply to Home/Beat; Tool theme is fixed dark).

---

### Task 7: Tool Dark Theme

**Files:**
- Create: `app/src/main/res/values/themes_tool.xml`

**Interfaces:**
- Produces: `Theme.PianoLab.Tool` style

- [ ] **Step 1: Write themes_tool.xml**

```xml
<resources>
    <style name="Theme.PianoLab.Tool" parent="Theme.Material3.Dark.NoActionBar">
        <item name="colorPrimary">@color/md_theme_tool_primary</item>
        <item name="colorOnPrimary">@color/md_theme_tool_onPrimary</item>
        <item name="colorPrimaryContainer">@color/md_theme_tool_primaryContainer</item>
        <item name="colorOnPrimaryContainer">@color/md_theme_tool_onPrimaryContainer</item>
        <item name="colorSecondary">@color/md_theme_tool_secondary</item>
        <item name="colorOnSecondary">@color/md_theme_tool_onSecondary</item>
        <item name="colorSecondaryContainer">@color/md_theme_tool_secondaryContainer</item>
        <item name="colorOnSecondaryContainer">@color/md_theme_tool_onSecondaryContainer</item>
        <item name="colorTertiary">@color/md_theme_tool_tertiary</item>
        <item name="colorOnTertiary">@color/md_theme_tool_onTertiary</item>
        <item name="colorTertiaryContainer">@color/md_theme_tool_tertiaryContainer</item>
        <item name="colorOnTertiaryContainer">@color/md_theme_tool_onTertiaryContainer</item>
        <item name="colorError">@color/md_theme_tool_error</item>
        <item name="colorOnError">@color/md_theme_tool_onError</item>
        <item name="colorSurface">@color/md_theme_tool_surface</item>
        <item name="colorOnSurface">@color/md_theme_tool_onSurface</item>
        <item name="colorSurfaceVariant">@color/md_theme_tool_surfaceVariant</item>
        <item name="colorOnSurfaceVariant">@color/md_theme_tool_onSurfaceVariant</item>
        <item name="colorSurfaceContainer">@color/md_theme_tool_surfaceContainer</item>
        <item name="colorSurfaceContainerHigh">@color/md_theme_tool_surfaceContainerHigh</item>
        <item name="colorSurfaceContainerHighest">@color/md_theme_tool_surfaceContainerHighest</item>
        <item name="colorOutline">@color/md_theme_tool_outline</item>
        <item name="colorOutlineVariant">@color/md_theme_tool_outlineVariant</item>
        <item name="colorSurfaceTint">@color/md_theme_tool_surfaceTint</item>
        <item name="android:colorBackground">@color/md_theme_tool_background</item>
        <item name="colorOnBackground">@color/md_theme_tool_onBackground</item>
        <item name="android:statusBarColor">@android:color/transparent</item>
        <item name="android:navigationBarColor">@android:color/transparent</item>
        <item name="android:windowBackground">@color/md_theme_tool_surface</item>
        <!-- Typography + widget defaults (shared with light theme) -->
        <item name="textAppearanceBodyLarge">@style/TextAppearance.PianoLab.BodyLarge</item>
        <item name="textAppearanceBodyMedium">@style/TextAppearance.PianoLab.BodyMedium</item>
        <item name="textAppearanceLabelLarge">@style/TextAppearance.PianoLab.LabelLarge</item>
        <item name="materialCardViewStyle">@style/Widget.PianoLab.CardView</item>
        <item name="materialButtonStyle">@style/Widget.PianoLab.Button</item>
    </style>
</resources>
```

---

### Task 8: Manifest Theme Binding

**Files:**
- Modify: `app/src/main/AndroidManifest.xml`

**Interfaces:**
- Consumes: `Theme.PianoLab.Tool` from Task 7

- [ ] **Step 1: Add theme to tool activities**

Add `android:theme="@style/Theme.PianoLab.Tool"` to:
- `TunerActivity`
- `ChordActivity`
- `VirtualPianoActivity`

Leave `<application android:theme="@style/Theme.PianoLab">` unchanged.

---

### Task 9: Build Verification

**Files:** None (verification only)

- [ ] **Step 1: Full debug build**

Run: `./gradlew :app:assembleDebug`
Expected: BUILD SUCCESSFUL

- [ ] **Step 2: Resource lint check**

Run: `./gradlew :app:lintDebug 2>&1 | findstr -i "error"`
Expected: No resource linking errors related to colors/themes

- [ ] **Step 3: Manual smoke test checklist**

| Screen | Check |
|--------|-------|
| Home | Background #f8f9fa, header blue #1a73e8, cards white/low-container |
| Beat | Light background, primary-colored labels |
| Tuner | Dark background, immersive mode works, switches visible |
| Chord | Dark background, immersive mode works |
| Virtual Piano | Dark background, immersive mode works |

---

## Plan Self-Review

- **Spec coverage:** All color roles, typography, shapes, dual themes, manifest binding, legacy aliases, and acceptance criteria mapped to Tasks 1–9. ✓
- **Placeholder scan:** No TBD/TODO entries. ✓
- **Type consistency:** Color resource names consistent across colors.xml, themes.xml, themes_tool.xml. ✓
