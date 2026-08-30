---
name: PianoLab
colors:
  surface: '#f8f9fa'
  surface-dim: '#d9dadb'
  surface-bright: '#f8f9fa'
  surface-container-lowest: '#ffffff'
  surface-container-low: '#f3f4f5'
  surface-container: '#edeeef'
  surface-container-high: '#e7e8e9'
  surface-container-highest: '#e1e3e4'
  on-surface: '#191c1d'
  on-surface-variant: '#414754'
  inverse-surface: '#2e3132'
  inverse-on-surface: '#f0f1f2'
  outline: '#727785'
  outline-variant: '#c1c6d6'
  surface-tint: '#005bc0'
  primary: '#005bbf'
  on-primary: '#ffffff'
  primary-container: '#1a73e8'
  on-primary-container: '#ffffff'
  inverse-primary: '#adc7ff'
  secondary: '#5b5f64'
  on-secondary: '#ffffff'
  secondary-container: '#dde0e6'
  on-secondary-container: '#5f6368'
  tertiary: '#006875'
  on-tertiary: '#ffffff'
  tertiary-container: '#008394'
  on-tertiary-container: '#000608'
  error: '#ba1a1a'
  on-error: '#ffffff'
  error-container: '#ffdad6'
  on-error-container: '#93000a'
  primary-fixed: '#d8e2ff'
  primary-fixed-dim: '#adc7ff'
  on-primary-fixed: '#001a41'
  on-primary-fixed-variant: '#004493'
  secondary-fixed: '#dfe3e8'
  secondary-fixed-dim: '#c3c7cc'
  on-secondary-fixed: '#181c20'
  on-secondary-fixed-variant: '#43474c'
  tertiary-fixed: '#9fefff'
  tertiary-fixed-dim: '#50d7ee'
  on-tertiary-fixed: '#001f24'
  on-tertiary-fixed-variant: '#004e59'
  background: '#f8f9fa'
  on-background: '#191c1d'
  surface-variant: '#e1e3e4'
typography:
  display-lg:
    fontFamily: Roboto Flex
    fontSize: 57px
    fontWeight: '400'
    lineHeight: 64px
    letterSpacing: -0.25px
  display-md:
    fontFamily: Roboto Flex
    fontSize: 45px
    fontWeight: '400'
    lineHeight: 52px
    letterSpacing: 0px
  headline-lg:
    fontFamily: Roboto Flex
    fontSize: 32px
    fontWeight: '400'
    lineHeight: 40px
    letterSpacing: 0px
  headline-lg-mobile:
    fontFamily: Roboto Flex
    fontSize: 28px
    fontWeight: '400'
    lineHeight: 36px
  title-lg:
    fontFamily: Roboto Flex
    fontSize: 22px
    fontWeight: '500'
    lineHeight: 28px
    letterSpacing: 0px
  body-lg:
    fontFamily: Roboto Flex
    fontSize: 16px
    fontWeight: '400'
    lineHeight: 24px
    letterSpacing: 0.5px
  body-md:
    fontFamily: Roboto Flex
    fontSize: 14px
    fontWeight: '400'
    lineHeight: 20px
    letterSpacing: 0.25px
  label-lg:
    fontFamily: Roboto Flex
    fontSize: 14px
    fontWeight: '500'
    lineHeight: 20px
    letterSpacing: 0.1px
  label-sm:
    fontFamily: Roboto Flex
    fontSize: 11px
    fontWeight: '500'
    lineHeight: 16px
    letterSpacing: 0.5px
rounded:
  sm: 0.25rem
  DEFAULT: 0.5rem
  md: 0.75rem
  lg: 1rem
  xl: 1.5rem
  full: 9999px
spacing:
  unit: 8px
  gutter: 16px
  margin-mobile: 16px
  margin-desktop: 24px
  container-max-width: 1200px
---

## Brand & Style
The design system for this product is rooted in the **Material 3 (M3)** philosophy, specifically optimized for a professional musical toolkit. The brand personality is precise, rhythmic, and high-utility, mirroring the technical nature of piano tuning and composition. 

The visual style follows **Modern Corporate / Android Adaptive** principles. It emphasizes clarity through generous whitespace, tonal elevation (rather than heavy shadows), and a sophisticated use of color that adapts to the content. The interface should feel like a high-end instrument: responsive, tactile, and reliable. The target audience includes professional pianists, tuners, and educators who require an interface that remains legible in varied lighting conditions (from stages to practice rooms).

## Colors
This design system utilizes the M3 dynamic color logic. The primary blue (#1A73E8) serves as the anchor for the "PianoLab Blue" tonal palette. 

- **Primary:** Used for key action buttons, active states, and prominent iconography.
- **Secondary:** A muted grey-blue used for less prominent UI elements and supporting information.
- **Tertiary:** A vibrant cyan used for accenting musical notation, metronome highlights, or specific tool states.
- **Surface Tones:** Instead of pure whites, use "Surface Container" (a subtle tint of the primary) for card backgrounds and "Surface Bright" for the main canvas. This reduces eye strain during long practice sessions.
- **Contrast:** Ensure all text-on-color combinations meet WCAG AA standards for accessibility.

## Typography
The system uses **Roboto Flex** for its high legibility and technical feel. As a variable font, it allows for fine-tuned weight adjustments which are critical for displaying musical frequencies and notations.

- **Display:** Used for large numeric values (e.g., BPM, Frequency Hz).
- **Headlines:** Used for primary screen titles.
- **Body:** Optimized for legibility in instructions and metadata.
- **Labels:** Strictly for functional UI elements like buttons, navigation items, and chip text. 

For mobile, headlines scale down to ensure content remains within the viewport without excessive wrapping.

## Layout & Spacing
The design system follows an **8px grid system**. Layouts are fluid, adapting from a 4-column structure on mobile to a 12-column structure on desktop.

- **Margins:** 16px on mobile to maximize horizontal space for keyboard/piano-related UI; 24px on larger screens.
- **Gutters:** 16px fixed to maintain a consistent rhythm between cards.
- **Reflow:** On tablets, the "Navigation Bar" should transition into a "Navigation Rail" on the left side to preserve vertical space for sheet music or visualizers.

## Elevation & Depth
Elevation is expressed primarily through **Tonal Layers** rather than shadows, consistent with the M3 spec.

- **Level 0 (Surface):** The base background.
- **Level 1 (Surface Container Low):** Main content cards. No shadow.
- **Level 2 (Surface Container):** Hover states or subtle groupings.
- **Level 3 (Surface Container High):** Modal dialogs and floating action buttons (FABs).
- **Shadows:** Use only for high-priority floating elements (FABs, Menus). Shadows are ultra-diffused, 10% opacity, tinted with the primary color to prevent a "dirty" grey look.

## Shapes
The shape language is "Extra Rounded" to provide a friendly yet modern feel.

- **Large Components (Cards, Dialogs):** Use `rounded-xl` (24px to 28px) to create a distinct, modern container.
- **Medium Components (Chips, Buttons):** Use fully rounded "pill" shapes for buttons to differentiate them from content cards.
- **Input Fields:** Use 4px to 8px rounding to maintain a professional, technical appearance amidst the softer card shapes.

## Components
- **Buttons:** Use the M3 "Filled" style for primary actions (e.g., "Start Tuning") and "Tonal" or "Outlined" for secondary actions. Always pill-shaped.
- **Navigation Bar:** A bottom navigation bar with a tinted background. Active icons use a "pill" shaped container highlight.
- **Sliders:** High-visibility M3 sliders with thick tracks. Essential for volume, tempo, and frequency adjustments. Use a primary color for the active track.
- **Cards:** Content is grouped in containers with a 24px border radius. Cards should not have borders; depth is defined by a subtle tonal shift from the background.
- **Material Symbols:** Use the "Outlined" style for a technical, precise look. Icons should use a 2px stroke weight to match the Roboto Flex typography.
- **Switches:** Standard M3 switches with a larger thumb area for easy toggling on touch screens during piano play.