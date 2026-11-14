---
name: BiliSearch Fixer
description: Local search filtering and re-ranking for the official Bilibili Android app.
colors:
  brand-pink: "#FB7299"
  on-brand: "#FFFFFF"
  deep-ink: "#0B1020"
  surface-light: "#FFFBFE"
  on-surface-light: "#1C1B1F"
  surface-dark: "#1C1B1F"
  on-surface-dark: "#E6E1E5"
  secondary-container-light: "#E8DEF8"
  error-container-light: "#F9DEDC"
rounded:
  card: "16dp"
  field: "4dp"
  button: "20dp"
spacing:
  xs: "4dp"
  sm: "8dp"
  md: "12dp"
  lg: "16dp"
  xl: "24dp"
typography:
  toolbar:
    fontFamily: "Roboto, sans-serif"
    fontSize: "22sp"
    fontWeight: 400
  card-title:
    fontFamily: "Roboto, sans-serif"
    fontSize: "16sp"
    fontWeight: 700
  body:
    fontFamily: "Roboto, sans-serif"
    fontSize: "14sp"
    fontWeight: 400
  caption:
    fontFamily: "Roboto, sans-serif"
    fontSize: "13sp"
    fontWeight: 400
  label-small:
    fontFamily: "Roboto, sans-serif"
    fontSize: "12sp"
    fontWeight: 400
components:
  card:
    backgroundColor: "{colors.surface-light}"
    rounded: "{rounded.card}"
    padding: "{spacing.lg}"
  preset-button:
    backgroundColor: "{colors.secondary-container-light}"
    textColor: "{colors.on-surface-light}"
    rounded: "{rounded.button}"
    height: "40dp"
  text-field:
    backgroundColor: "transparent"
    textColor: "{colors.on-surface-light}"
    rounded: "{rounded.field}"
    height: "56dp"
  status-refresh:
    textColor: "{colors.brand-pink}"
    typography: "{typography.label-small}"
  bottom-nav:
    backgroundColor: "{colors.surface-light}"
    textColor: "{colors.on-surface-light}"
    typography: "{typography.label-small}"
    height: "80dp"
---

# Design System: BiliSearch Fixer

## Overview

**Creative North Star: "The Search Lens"**

BiliSearch Fixer is a precision instrument, not a social surface. It sits inside the official Bilibili app, removes what does not belong in a search result set, and then gets out of the way. The interface should feel like a clean lens: calm, exact, and quiet enough that the user trusts the result without studying the mechanism.

The visual language is Material 3 with the brand reduced to a single warm pink signal. Density is low, hierarchy is carried by grouping and typography rather than decoration, and every screen is judged by whether the user can tell what is on, what it removed, and what it cannot do. The surface is organized into three search-only destinations: 过滤 (strength and filter types), 规则 (block lists and search syntax), 状态 (runtime evidence and about). Flat tonal surfaces keep the page fast and native; shadows are not part of the world.

**Key Characteristics:**
- One accent, used sparingly.
- Flat cards, generous separation, no decorative depth.
- Result-oriented language over implementation detail.
- Native Material 3 controls, never custom imitations.
- Three destinations, all inside the search problem; no home, player, or account surface.
- Every advanced decision is explicit and reversible.

## Colors

The palette is a warm signal on a neutral surface: Bilibili pink for active state, deep ink for the icon world, and Material 3 surface roles for everything structural.

### Primary
- **Bilibili Signal** (#FB7299): active switches, selected preset state, refresh action, and the active bottom-navigation indicator. It is the only saturated color in the working surface.
- **Signal Ink** (#0B1020): icon background and the darkest brand moment; never used as body text on light surfaces.

### Neutral
- **Paper** (#FFFBFE light / #1C1B1F dark): the page and card surface, resolved through `colorSurface` so light and dark stay consistent.
- **Ink** (#1C1B1F light / #E6E1E5 dark): primary text, resolved through `colorOnSurface`.
- **Muted Ink** (`colorOnSurfaceVariant`): helper lines, captions, secondary status. Never a raw gray.
- **Selected Lavender** (#E8DEF8): the Material 3 secondary container used by the selected preset button and the bottom-navigation active indicator.
- **Warning Wash** (#F9DEDC): the disconnected-service banner, resolved through `colorErrorContainer`.

### Named Rules

**The Ten Percent Pink Rule.** Bilibili Signal appears on at most one dominant control per card. It marks state, not decoration; never fill a large surface with it.

## Typography

**Display Font:** Roboto (system sans-serif)
**Body Font:** Roboto (system sans-serif)

**Character:** Neutral, compact, and legible at a glance. The type does the hierarchy work so the cards do not need borders or shadows.

### Hierarchy
- **Toolbar** (400, 22sp): current destination title; one per screen.
- **Card title** (700, 16sp): the only bold step in the content area.
- **Body** (400, 14sp): labels, status summary, primary reading text.
- **Caption** (400, 13sp): secondary status, aggregate numbers, field hints.
- **Label small** (400, 12sp): helper text, syntax notes, limitations, bottom-nav labels.

All sizes are `sp`; the layout must survive a 1.3× font scale without clipping.

### Named Rules

**The One Bold Step Rule.** Only card titles and the app title use a heavier weight. Body, captions, and helper text stay regular; emphasis comes from placement, not extra weights.

## Layout

A single-column, vertically scrolling surface with a fixed bottom navigation bar. Three destinations share one scroll container; only the selected destination's content is visible. Sixteen dp outer padding, twelve dp between cards, sixteen dp inside each card. The primary task (strength and filter choices) comes first; rules follow; runtime status sits last so it never competes with configuration.

Touch targets are at least 48dp, with switches and rows separated by the Material minimum. The root layout applies system-bar insets so the toolbar never sits under the status bar, and the bottom inset keeps the bottom navigation clear of the gesture bar. There is no multi-column tablet layout; the surface stays a readable single column at every width.

## Elevation & Depth

**The Flat-By-Default Rule.** Cards are flat at rest: `cardElevation = 0dp`, no borders, no drop shadows. Depth is conveyed only by tonal surface layering and spacing. There is no shadow vocabulary in this system; if a future component needs separation, use a surface-container step before reaching for a shadow.

## Shapes

Cards use a 16dp corner radius, text fields use the Material outlined 4dp corner, and buttons use the Material pill (20dp). The form language is rounded but not playful: one card radius, one control family, no mixed silhouettes. Avoid nested cards; a card inside a card reads as a bug.

## Components

### Cards / Containers
- **Corner Style:** 16dp.
- **Background:** `colorSurface`; no border.
- **Shadow Strategy:** none; see Elevation.
- **Internal Padding:** 16dp.
- **Behavior:** one topic per card; a card is a group, not a page section.

### Buttons
- **Primary:** Material text button for low-weight actions such as refresh; brand pink text on surface.
- **Preset group:** outlined Material buttons in a single-selection group; selected state uses the secondary container, not a filled pink block.
- **Shape:** Material pill (20dp).
- **States:** disabled when the LSPosed service is unavailable; selected preset reflects the stored strength.

### Switches
- **Style:** Material 3 switch, full row, 48dp minimum height.
- **State:** checked uses the brand pink; unchecked stays neutral. Never use a custom track.
- **Copy:** a short verb phrase; supporting text goes below, not inside the switch label.

### Strength Selector
- **Shape:** three outlined Material buttons in a single-selection group: 宽松 / 标准 / 严格.
- **Modes:** 标准 is the recommended default; no raw percentage is exposed.
- **Explanation:** a 13sp helper line changes with the selected mode; a 12sp footer says "不知道选哪个就用标准。删多了往宽松调，删少了往严格调。"
- **State:** selected uses the secondary container; disabled when the LSPosed service is unavailable.

### Inputs / Fields
- **Style:** Material 3 outlined text field, 4dp radius, 56dp minimum height.
- **Focus:** standard Material outline; no custom glow.
- **Error:** minimum play count shows an inline error for negative or non-numeric input.
- **Save behavior:** text rules save on focus loss and on `onPause`; there is no separate save button.
- **Hint:** keep hints short enough to fit one line; move long explanations to helper text below.

### Navigation
- **Top app bar:** current destination title plus `BiliSearch Fixer · vX.Y.Z` subtitle.
- **Bottom navigation:** three labeled destinations — 过滤, 规则, 状态 — with 24dp icons and always-visible labels.
- **Selected state:** secondary-container pill behind the icon; brand pink is reserved for switches and active controls.
- **Behavior:** switching destinations clears focus, scrolls to the top, and saves any pending rule text.

### Status Card
- **Location:** first card on the 状态 destination, never on top of configuration.
- **Copy:** result first ("搜索过滤已生效"), evidence second (counts, last search), implementation detail last or omitted.
- **Refresh:** low-weight text button, not a filled primary action.

## Do's and Don'ts

### Do:
- **Do** keep one topic per card and one card gap (12dp) across all three destinations.
- **Do** put the result before the mechanism: "删除 18 条" before "安装 2 个 Hook".
- **Do** use Material 3 controls and let the theme resolve light/dark surfaces.
- **Do** keep text-rule hints one line and move explanation below the field.
- **Do** preserve 48dp touch targets and system-bar insets.

### Don't:
- **Don't** put a second card inside a card or add shadows to create hierarchy.
- **Don't** use raw grays for helper text; use `colorOnSurfaceVariant`.
- **Don't** fill large surfaces with Bilibili Signal; it is a state color.
- **Don't** promise actions the hook cannot perform (for example, hiding the bottom related-search block).
- **Don't** mix autosave and explicit-save models on the same screen.
- **Don't** expose a 0-100 relevance slider; users choose a named mode with a plain-language explanation.
