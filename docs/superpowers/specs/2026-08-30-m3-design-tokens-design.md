# PianoLab M3 Design Tokens (Phase A) — Design Spec

**Date:** 2026-08-30  
**Status:** Approved  
**Scope:** Design token layer only; no layout restructure  
**Constraint:** View-based (no Compose migration)

---

## Goal

Establish a Material 3 design token foundation for PianoLab using the color palette and typography defined in `design/DESIGN.md`. Existing layouts remain unchanged and inherit new tokens via theme attributes and backward-compatible color aliases.

Subsequent phases (B, C, …) will refactor individual screens (Home, Beat, Tuner, Chord, Virtual Piano) one at a time.

---

## Architecture

### Dual-Theme Strategy

| Theme | Parent | Used By | Mode |
|-------|--------|---------|------|
| `Theme.PianoLab` | `Theme.Material3.Light.NoActionBar` | HomeActivity, BeatActivity | Light M3 surfaces |
| `Theme.PianoLab.Tool` | `Theme.Material3.Dark.NoActionBar` | TunerActivity, ChordActivity, VirtualPianoActivity | Dark tool surfaces |

Tool activities retain immersive full-screen behavior via existing `ImmersiveUiHelper`; only the theme declaration changes.

### Modular Resource Files

```
app/src/main/res/
  values/
    colors.xml           # M3 light color roles (from DESIGN.md)
    colors_tool.xml      # Tool dark color roles
    themes.xml           # Theme.PianoLab
    themes_tool.xml      # Theme.PianoLab.Tool
    typography.xml       # TextAppearance styles
    shapes.xml           # ShapeAppearance styles
    dimens.xml           # 8px grid spacing
    styles_widgets.xml   # Default widget styles (Card, Button)
  values-night/
    themes.xml           # Sync parent to M3 (minimal; Tool theme is fixed dark)
  font/
    roboto_flex.xml      # Google Fonts downloadable font provider
```

### Dependency

- `com.google.android.material:material` upgraded from `1.10.0` → `1.12.0`

---

## Color Tokens (Light — from DESIGN.md)

All values taken verbatim from `design/DESIGN.md` frontmatter:

| Token | Hex |
|-------|-----|
| primary | `#005bbf` |
| on-primary | `#ffffff` |
| primary-container | `#1a73e8` |
| on-primary-container | `#ffffff` |
| inverse-primary | `#adc7ff` |
| secondary | `#5b5f64` |
| on-secondary | `#ffffff` |
| secondary-container | `#dde0e6` |
| on-secondary-container | `#5f6368` |
| tertiary | `#006875` |
| on-tertiary | `#ffffff` |
| tertiary-container | `#008394` |
| on-tertiary-container | `#000608` |
| error | `#ba1a1a` |
| on-error | `#ffffff` |
| error-container | `#ffdad6` |
| on-error-container | `#93000a` |
| background | `#f8f9fa` |
| on-background | `#191c1d` |
| surface | `#f8f9fa` |
| on-surface | `#191c1d` |
| surface-variant | `#e1e3e4` |
| on-surface-variant | `#414754` |
| surface-dim | `#d9dadb` |
| surface-bright | `#f8f9fa` |
| surface-container-lowest | `#ffffff` |
| surface-container-low | `#f3f4f5` |
| surface-container | `#edeeef` |
| surface-container-high | `#e7e8e9` |
| surface-container-highest | `#e1e3e4` |
| inverse-surface | `#2e3132` |
| inverse-on-surface | `#f0f1f2` |
| outline | `#727785` |
| outline-variant | `#c1c6d6` |
| surface-tint | `#005bc0` |
| primary-fixed | `#d8e2ff` |
| primary-fixed-dim | `#adc7ff` |
| on-primary-fixed | `#001a41` |
| on-primary-fixed-variant | `#004493` |
| secondary-fixed | `#dfe3e8` |
| secondary-fixed-dim | `#c3c7cc` |
| on-secondary-fixed | `#181c20` |
| on-secondary-fixed-variant | `#43474c` |
| tertiary-fixed | `#9fefff` |
| tertiary-fixed-dim | `#50d7ee` |
| on-tertiary-fixed | `#001f24` |
| on-tertiary-fixed-variant | `#004e59` |

Android resource naming: `md_theme_light_<role>` mapped to `colorPrimary`, `colorSurfaceContainerLow`, etc. in theme.

### Backward-Compatible Aliases

Existing layouts reference legacy names. All must remain resolvable:

| Legacy Name | Maps To |
|-------------|---------|
| `piano_primary` | `md_theme_light_primaryContainer` (#1a73e8) |
| `piano_primary_light` | `md_theme_light_primary_fixed_dim` (#adc7ff) |
| `piano_primary_dark` | `md_theme_light_primary` (#005bbf) |
| `piano_accent` | `md_theme_light_tertiary_container` (#008394) |
| `piano_accent_light` | `md_theme_light_tertiary_fixed_dim` (#50d7ee) |
| `piano_background` | `md_theme_light_background` |
| `piano_surface` | `md_theme_light_surface_container_lowest` |
| `text_primary` | `md_theme_light_on_surface` |
| `text_secondary` | `md_theme_light_on_surface_variant` |
| `text_on_primary` | `md_theme_light_on_primary` |
| `purple_200/500/700`, `teal_200/700` | Existing alias chain preserved |
| `piano_black`, `piano_dark_gray`, etc. | Remapped to nearest M3 token |

Functional colors (`tuner_cursor_green/yellow/red`) remain unchanged.

---

## Color Tokens (Tool Dark Theme)

Derived from DESIGN.md inverse roles; tuned for immersive tool screens:

| Role | Hex | Source |
|------|-----|--------|
| surface | `#1a1c1e` | Near-black canvas for piano/tuner focus |
| on-surface | `#f0f1f2` | inverse-on-surface |
| surface-container | `#2e3132` | inverse-surface |
| surface-container-high | `#383b3c` | +1 elevation step |
| on-surface-variant | `#c1c6d6` | outline-variant (readable on dark) |
| primary | `#adc7ff` | inverse-primary |
| primary-container | `#1a73e8` | primary-container (action highlight) |
| on-primary | `#001a41` | on-primary-fixed |
| tertiary | `#50d7ee` | tertiary-fixed-dim |
| outline | `#727785` | outline |
| background | `#1a1c1e` | Same as surface |

---

## Typography

Font family: **Roboto Flex** via Google Fonts Downloadable Font (`res/font/roboto_flex.xml`).

| Style Name | Size | Weight | Line Height | Letter Spacing |
|------------|------|--------|-------------|----------------|
| DisplayLarge | 57sp | 400 | 64sp | -0.25sp |
| DisplayMedium | 45sp | 400 | 52sp | 0 |
| HeadlineLarge | 28sp | 400 | 36sp | 0 |
| TitleLarge | 22sp | 500 | 28sp | 0 |
| BodyLarge | 16sp | 400 | 24sp | 0.5sp |
| BodyMedium | 14sp | 400 | 20sp | 0.25sp |
| LabelLarge | 14sp | 500 | 20sp | 0.1sp |
| LabelSmall | 11sp | 500 | 16sp | 0.5sp |

Theme sets `android:fontFamily` default via `textAppearanceBodyLarge` etc.

---

## Shapes

| Style | Corner Radius | Used For |
|-------|---------------|----------|
| ShapeAppearance.PianoLab.LargeComponent | 24dp | Cards, dialogs |
| ShapeAppearance.PianoLab.MediumComponent | 12dp | Chips |
| ShapeAppearance.PianoLab.SmallComponent | 50% (pill) | Buttons |
| ShapeAppearance.PianoLab.ExtraSmall | 4dp | Input fields |

---

## Spacing (dimens.xml)

| Token | Value |
|-------|-------|
| spacing_unit | 8dp |
| spacing_gutter | 16dp |
| spacing_margin_mobile | 16dp |
| spacing_margin_desktop | 24dp |
| card_corner_radius | 24dp |
| button_corner_radius | 999dp (pill) |

---

## Component Default Styles

### MaterialCardView (`Widget.PianoLab.CardView`)
- `cardCornerRadius`: 24dp
- `cardElevation`: 0dp
- `cardBackgroundColor`: `?attr/colorSurfaceContainerLow`
- `strokeWidth`: 0dp

### MaterialButton (`Widget.PianoLab.Button`)
- `cornerRadius`: pill (999dp)
- Style: filled, primary container color

### SwitchMaterial
- Inherits M3 defaults; `colorPrimary` from active theme

---

## Manifest Changes

```xml
<!-- Tool activities only -->
android:theme="@style/Theme.PianoLab.Tool"
```

Applied to: `TunerActivity`, `ChordActivity`, `VirtualPianoActivity`.

HomeActivity and BeatActivity inherit `Theme.PianoLab` from `<application>`.

---

## Out of Scope (Phase A)

- Layout XML structure changes
- Custom View internal colors (PianoView, WaveformView, StaffView, etc.)
- Bottom Navigation / Navigation Rail
- Full `values-night` light/dark system toggle (Tool theme is fixed dark)
- Material Symbols icon migration
- Compose migration

---

## Acceptance Criteria

1. Project compiles and runs without resource errors
2. Home / Beat display M3 light surfaces (#f8f9fa background, #1a73e8 primary actions)
3. Tuner / Chord / Virtual Piano use Tool dark theme; immersive mode unchanged
4. All legacy color references (`piano_primary`, `text_primary`, etc.) resolve correctly
5. MaterialCardView and SwitchMaterial inherit M3 styling from theme defaults

---

## Future Phases

| Phase | Screen | Work |
|-------|--------|------|
| B1 | Home | Replace hardcoded colors with `?attr/`, apply typography, remove header banner in favor of M3 top app bar |
| B2 | Beat | M3 sliders, tonal cards, pill buttons |
| B3 | Tuner | Tonal control panels, M3 switches |
| B4 | Chord | Staff area + M3 tool chrome |
| B5 | Virtual Piano | M3 control bar, surface containers |
