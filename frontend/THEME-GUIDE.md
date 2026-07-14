# OpenFixity Theme System Guide

## Overview
OpenFixity uses a CSS variable-based theme system with 4 customizable colors per theme. All UI elements automatically adapt to the selected theme.

## Theme Colors

### Background
- **What it is**: The main page background color
- **Usage**: `bg-background`
- **Example**: Page body, card backgrounds with `/50` opacity

### Foreground
- **What it is**: Primary text and border color
- **Usage**: `text-foreground`, `border-foreground/10` (with opacity)
- **Example**: Body text, headings, borders

### Primary
- **What it is**: Main brand/action color (OPF purple in light, OPF blue in dark)
- **Usage**: `bg-primary`, `text-primary-foreground`
- **Example**: Active navigation items, primary buttons

### Accent
- **What it is**: Secondary highlight color (matches primary by default)
- **Usage**: `bg-accent`, `text-accent`
- **Example**: Toggle switches, links, secondary actions

## Usage Guidelines

### DO ✓
```tsx
// Use theme colors with semantic names
<div className="bg-background text-foreground">
<button className="bg-primary text-primary-foreground">
<a className="text-accent hover:text-accent/80">

// Use opacity for subtle effects
<div className="border border-foreground/10">
<div className="bg-foreground/5">
```

### DON'T ✗
```tsx
// Avoid hardcoded Tailwind colors
<div className="bg-white dark:bg-gray-800"> // ✗ NO
<p className="text-gray-900 dark:text-white"> // ✗ NO
<button className="bg-blue-600"> // ✗ NO
```

## Available Themes

1. **System Default** - Follows OS preference (light/dark)
2. **Light** - Gray background, dark text, purple accent
3. **Dark** - Dark background, light text, blue accent
4. **Light High Contrast** - Pure white bg, black text, dark purple (WCAG AAA)
5. **Dark High Contrast** - Pure black bg, white text, bright cyan (WCAG AAA)

## High Contrast Themes
High contrast themes meet WCAG AAA accessibility standards:
- Light HC: 21:1 contrast ratio minimum
- Dark HC: 21:1 contrast ratio minimum
- Brighter, more saturated accent colors for visibility

## Customization
Users can customize all 4 colors per theme via Settings > Appearance. Colors are stored in localStorage per theme (e.g., `customColors_light`, `customColors_dark`).

## Future Components
When adding new UI components:
1. Use `bg-background` for backgrounds
2. Use `text-foreground` for text
3. Use `bg-primary` for main actions
4. Use `text-accent` for links/highlights
5. Use opacity modifiers (`/10`, `/50`, `/80`) for subtle effects
6. Never hardcode colors - always use theme variables
