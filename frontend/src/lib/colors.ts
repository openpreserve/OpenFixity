export interface ColorScheme {
  background: string
  foreground: string
  card: string
  primary: string
  accent: string
}

export type ThemeMode = 'light' | 'dark' | 'light-high-contrast' | 'dark-high-contrast' | 'system'

export const themeColors: Record<Exclude<ThemeMode, 'system'>, ColorScheme> = {
  light: {
    background: '220 17% 97%',     // gray-50
    foreground: '222 47% 11%',     // gray-900
    card: '0 0% 100%',             // white - slightly lighter than background
    primary: '334 60% 45%',        // OPF purple #c73079
    accent: '334 60% 45%',         // OPF purple
  },
  dark: {
    background: '222 47% 11%',     // gray-900
    foreground: '220 17% 97%',     // gray-50
    card: '215 28% 17%',           // gray-800 - slightly lighter than background
    primary: '196 78% 36%',        // OPF blue #137da4
    accent: '196 78% 36%',         // OPF blue
  },
  'light-high-contrast': {
    background: '0 0% 100%',       // pure white
    foreground: '0 0% 0%',         // pure black
    card: '0 0% 95%',              // very light gray for subtle contrast
    primary: '334 100% 25%',       // very dark purple for WCAG AAA
    accent: '334 100% 30%',        // dark purple
  },
  'dark-high-contrast': {
    background: '0 0% 0%',         // pure black
    foreground: '0 0% 100%',       // pure white
    card: '0 0% 10%',              // very dark gray for subtle contrast
    primary: '196 100% 88%',       // bright cyan #c2efff for maximum visibility
    accent: '196 100% 85%',        // slightly darker cyan
  },
}

function hexToHSL(hex: string): string {
  const r = parseInt(hex.slice(1, 3), 16) / 255
  const g = parseInt(hex.slice(3, 5), 16) / 255
  const b = parseInt(hex.slice(5, 7), 16) / 255

  const max = Math.max(r, g, b)
  const min = Math.min(r, g, b)
  let h = 0, s = 0, l = (max + min) / 2

  if (max !== min) {
    const d = max - min
    s = l > 0.5 ? d / (2 - max - min) : d / (max + min)
    
    switch (max) {
      case r: h = ((g - b) / d + (g < b ? 6 : 0)) / 6; break
      case g: h = ((b - r) / d + 2) / 6; break
      case b: h = ((r - g) / d + 4) / 6; break
    }
  }

  return `${Math.round(h * 360)} ${Math.round(s * 100)}% ${Math.round(l * 100)}%`
}

function hslToHex(hsl: string): string {
  if (!hsl || typeof hsl !== 'string') {
    return '#000000' // fallback to black
  }
  
  const parts = hsl.split(' ')
  if (parts.length < 3) {
    return '#000000' // fallback to black
  }
  
  const [h, s, l] = parts.map((v, i) => 
    i === 0 ? parseInt(v) : parseInt(v) / 100
  )
  
  if (isNaN(h) || isNaN(s) || isNaN(l)) {
    return '#000000' // fallback to black
  }
  
  const a = s * Math.min(l, 1 - l)
  const f = (n: number) => {
    const k = (n + h / 30) % 12
    const color = l - a * Math.max(Math.min(k - 3, 9 - k, 1), -1)
    return Math.round(255 * color).toString(16).padStart(2, '0')
  }
  
  return `#${f(0)}${f(8)}${f(4)}`
}

function getContrastColor(hsl: string): string {
  // Parse HSL values
  const [, s, l] = hsl.split(' ').map((v, i) => 
    i === 0 ? parseInt(v) : parseInt(v) / 100
  )
  
  // If lightness > 50%, use dark text (0% lightness), otherwise use white (100% lightness)
  // For high saturation colors, adjust threshold
  const threshold = s > 0.5 ? 0.6 : 0.5
  
  return l > threshold ? '0 0% 0%' : '0 0% 100%'
}

export function applyColorScheme(scheme: ColorScheme) {
  const root = document.documentElement
  const devMode = localStorage.getItem('devMode') === 'true'
  
  if (devMode) {
    console.log('Applying color scheme:', scheme)
  }
  
  // Calculate contrasting text colors for primary and accent
  const primaryForeground = getContrastColor(scheme.primary)
  const accentForeground = getContrastColor(scheme.accent)
  
  root.style.setProperty('--background', scheme.background)
  root.style.setProperty('--foreground', scheme.foreground)
  root.style.setProperty('--card', scheme.card)
  root.style.setProperty('--primary', scheme.primary)
  root.style.setProperty('--primary-foreground', primaryForeground)
  root.style.setProperty('--accent', scheme.accent)
  root.style.setProperty('--accent-foreground', accentForeground)
  
  if (devMode) {
    console.log('Card color set to:', scheme.card)
  }
}

export function loadCustomColors(theme: Exclude<ThemeMode, 'system'>): ColorScheme | null {
  const stored = localStorage.getItem(`customColors_${theme}`)
  return stored ? JSON.parse(stored) : null
}

export function saveCustomColors(theme: Exclude<ThemeMode, 'system'>, colors: ColorScheme) {
  localStorage.setItem(`customColors_${theme}`, JSON.stringify(colors))
}

export function resetToDefaults(theme: Exclude<ThemeMode, 'system'>) {
  localStorage.removeItem(`customColors_${theme}`)
}

export { hexToHSL, hslToHex }
