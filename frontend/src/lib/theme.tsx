import { createContext, useContext, useEffect, useState } from 'react'
import type { ReactNode } from 'react'
import { 
  applyColorScheme, 
  loadCustomColors, 
  saveCustomColors,
  resetToDefaults,
  themeColors,
  type ColorScheme,
  type ThemeMode
} from './colors'

interface ThemeContextType {
  theme: ThemeMode
  setTheme: (theme: ThemeMode) => void
  resolvedTheme: Exclude<ThemeMode, 'system'>
  currentColors: ColorScheme
  setCurrentColors: (colors: ColorScheme) => void
  resetColors: () => void
}

const ThemeContext = createContext<ThemeContextType | undefined>(undefined)

export function ThemeProvider({ children }: { children: ReactNode }) {
  const [theme, setThemeState] = useState<ThemeMode>(() => {
    const stored = localStorage.getItem('theme') as ThemeMode
    return stored || 'system'
  })

  const [resolvedTheme, setResolvedTheme] = useState<Exclude<ThemeMode, 'system'>>('light')
  const [currentColors, setCurrentColorsState] = useState<ColorScheme>(themeColors.light)

  useEffect(() => {
    const root = window.document.documentElement
    
    const updateTheme = () => {
      let resolved: Exclude<ThemeMode, 'system'>
      
      if (theme === 'system') {
        resolved = window.matchMedia('(prefers-color-scheme: dark)').matches ? 'dark' : 'light'
      } else {
        resolved = theme
      }
      
      setResolvedTheme(resolved)
      
      // Update dark mode class
      const isDark = resolved === 'dark' || resolved === 'dark-high-contrast'
      root.classList.remove('light', 'dark')
      root.classList.add(isDark ? 'dark' : 'light')
      
      // Load custom or default colors for this theme
      const customColors = loadCustomColors(resolved)
      const colors = customColors || themeColors[resolved]
      setCurrentColorsState(colors)
      applyColorScheme(colors)
    }

    updateTheme()

    if (theme === 'system') {
      const mediaQuery = window.matchMedia('(prefers-color-scheme: dark)')
      mediaQuery.addEventListener('change', updateTheme)
      return () => mediaQuery.removeEventListener('change', updateTheme)
    }
  }, [theme])

  const setTheme = (newTheme: ThemeMode) => {
    setThemeState(newTheme)
    localStorage.setItem('theme', newTheme)
  }
  
  const setCurrentColors = (colors: ColorScheme) => {
    setCurrentColorsState(colors)
    saveCustomColors(resolvedTheme, colors)
    applyColorScheme(colors)
  }
  
  const resetColors = () => {
    resetToDefaults(resolvedTheme)
    const defaults = themeColors[resolvedTheme]
    setCurrentColorsState(defaults)
    applyColorScheme(defaults)
  }

  return (
    <ThemeContext.Provider value={{ 
      theme, 
      setTheme, 
      resolvedTheme,
      currentColors,
      setCurrentColors,
      resetColors
    }}>
      {children}
    </ThemeContext.Provider>
  )
}

export function useTheme() {
  const context = useContext(ThemeContext)
  if (!context) {
    throw new Error('useTheme must be used within ThemeProvider')
  }
  return context
}
