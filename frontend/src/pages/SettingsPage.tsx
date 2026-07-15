import { useState, useEffect } from 'react'
import { Bell, HelpCircle, Info, Palette, RotateCcw, Server, Copy, Check, GitBranch, ExternalLink, Bug } from 'lucide-react'
import { useTheme } from '@/lib/theme'
import { hslToHex, hexToHSL, type ThemeMode, type ColorScheme } from '@/lib/colors'
import { useTutorial } from '@/lib/tutorial'
import { isCronSchedulingEnabled, setCronSchedulingEnabled } from '@/lib/settings'
import { useAppInfo } from '@/hooks/api'

function formatUptime(ms: number): string {
  const totalSeconds = Math.floor(ms / 1000)
  const hours = Math.floor(totalSeconds / 3600)
  const minutes = Math.floor((totalSeconds % 3600) / 60)
  const seconds = totalSeconds % 60
  if (hours > 0) return `${hours}h ${minutes}m`
  if (minutes > 0) return `${minutes}m ${seconds}s`
  return `${seconds}s`
}

export default function SettingsPage() {
  const tutorial = useTutorial()
  const { 
    theme, 
    setTheme, 
    resolvedTheme, 
    currentColors,
    setCurrentColors,
    resetColors 
  } = useTheme()
  const [devMode, setDevMode] = useState(false)
  const [cronScheduling, setCronScheduling] = useState(false)
  const [notifications, setNotifications] = useState<any[]>([])
  const { data: appInfo } = useAppInfo()
  const [copied, setCopied] = useState(false)

  const appUrl = `${window.location.origin}/app`

  const copyAppUrl = async () => {
    try {
      await navigator.clipboard.writeText(appUrl)
      setCopied(true)
      setTimeout(() => setCopied(false), 1500)
    } catch {
      // Clipboard may be unavailable (e.g. non-secure context); the URL is still shown to copy manually.
    }
  }

  useEffect(() => {
    document.title = 'Settings | OpenFixity';
  }, []);

  useEffect(() => {
    const savedDevMode = localStorage.getItem('devMode')
    const savedNotifications = localStorage.getItem('notifications')
    
    if (savedDevMode) setDevMode(savedDevMode === 'true')
    if (savedNotifications) setNotifications(JSON.parse(savedNotifications))
    setCronScheduling(isCronSchedulingEnabled())
  }, [])

  const handleColorChange = (key: keyof ColorScheme, hexValue: string) => {
    const hslValue = hexToHSL(hexValue)
    const updated = { ...currentColors, [key]: hslValue }
    setCurrentColors(updated)
  }
  
  const handleTutorialToggle = () => {
    tutorial.setEnabled(!tutorial.enabled)
  }
  
  const handleDevModeToggle = () => {
    const newValue = !devMode
    setDevMode(newValue)
    localStorage.setItem('devMode', String(newValue))
  }

  const handleCronSchedulingToggle = () => {
    const newValue = !cronScheduling
    setCronScheduling(newValue)
    setCronSchedulingEnabled(newValue)
  }
  
  const clearNotificationHistory = () => {
    setNotifications([])
    localStorage.removeItem('notifications')
  }
  
  const colorLabels: Record<keyof ColorScheme, string> = {
    background: 'Background',
    foreground: 'Foreground',
    card: 'Card',
    primary: 'Primary',
    accent: 'Accent',
  }
  
  const themeOptions: { value: ThemeMode; label: string }[] = [
    { value: 'system', label: 'System Default' },
    { value: 'light', label: 'Light' },
    { value: 'dark', label: 'Dark' },
    { value: 'light-high-contrast', label: 'Light High Contrast' },
    { value: 'dark-high-contrast', label: 'Dark High Contrast' },
  ]

  return (
    <div>
      <h2 className="text-3xl font-bold text-foreground mb-6">Settings & About</h2>
      
      <div className="space-y-6">
        {/* Theme Settings */}
        <div className="bg-card rounded-lg shadow border border-foreground/10 p-6">
          <h3 className="text-xl font-semibold mb-4 flex items-center gap-2 text-foreground">
            <Palette className="w-5 h-5" />
            Appearance
          </h3>
          <div className="space-y-4">
            <div>
              <label className="block text-sm font-medium text-foreground/80 mb-2">
                Theme
              </label>
              <select
                value={theme}
                onChange={(e) => setTheme(e.target.value as ThemeMode)}
                className="w-full px-4 py-2 border border-foreground/20 rounded-md bg-background text-foreground focus:ring-2 focus:ring-accent focus:border-transparent"
              >
                {themeOptions.map((option) => (
                  <option key={option.value} value={option.value}>
                    {option.label}
                  </option>
                ))}
              </select>
              <p className="text-xs text-foreground/60 mt-2">
                Currently using: {resolvedTheme.replace('-', ' ')}
              </p>
            </div>
            
            <div className="mt-6 border-t border-foreground/10 pt-6">
              <div className="flex items-center justify-between mb-4">
                <label className="block text-sm font-medium text-foreground/80">
                  Customize Colors - {resolvedTheme.replace('-', ' ')}
                </label>
                <button
                  onClick={resetColors}
                  className="flex items-center gap-2 px-3 py-1.5 text-sm border border-gray-600 rounded-md bg-gray-700 text-white hover:bg-gray-600 transition-colors"
                >
                  <RotateCcw className="w-4 h-4" />
                  Reset to Defaults
                </button>
              </div>
              
              <div className="grid grid-cols-2 gap-4">
                {(Object.keys(colorLabels) as Array<keyof ColorScheme>).map((key) => {
                  const colorValue = currentColors[key] || '0 0% 0%'
                  const hexValue = hslToHex(colorValue)
                  return (
                    <div key={key}>
                      <label className="block text-xs font-medium text-foreground/70 mb-2">
                        {colorLabels[key]}
                      </label>
                      <div className="flex gap-2">
                        <input
                          type="color"
                          value={hexValue}
                          onChange={(e) => handleColorChange(key, e.target.value)}
                          className="w-12 h-10 rounded border border-foreground/20 cursor-pointer"
                        />
                        <input
                          type="text"
                          value={hexValue}
                          onChange={(e) => handleColorChange(key, e.target.value)}
                          className="flex-1 px-2 py-1.5 text-sm font-mono border border-foreground/20 rounded bg-background text-foreground"
                          placeholder="#000000"
                        />
                      </div>
                    </div>
                  )
                })}
              </div>
              
              <p className="text-xs text-foreground/60 mt-4">
                Colors are theme-specific. Switch themes to customize each one independently.
              </p>
            </div>
          </div>
        </div>
  
        {/* Tutorial Settings */}
        <div className="bg-card rounded-lg shadow border border-foreground/10 p-6">
          <h3 className="text-xl font-semibold mb-4 flex items-center gap-2 text-foreground">
            <HelpCircle className="w-5 h-5" />
            Tutorial & Help
          </h3>
          <div className="space-y-4">
            <div className="flex items-center justify-between">
              <div>
                <p className="font-medium text-foreground">First-Run Tutorial Mode</p>
                <p className="text-sm text-foreground/70">
                  Show helpful tooltips and guidance throughout the app
                </p>
              </div>
              <label className="relative inline-flex items-center cursor-pointer">
                <input
                  type="checkbox"
                  checked={tutorial.enabled}
                  onChange={handleTutorialToggle}
                  className="sr-only peer"
                />
                <div className="w-11 h-6 bg-gray-200 dark:bg-gray-700 peer-focus:outline-none peer-focus:ring-4 peer-focus:ring-accent/30 rounded-full peer peer-checked:after:translate-x-full peer-checked:after:border-white after:content-[''] after:absolute after:top-[2px] after:left-[2px] after:bg-white after:border-gray-300 after:border after:rounded-full after:h-5 after:w-5 after:transition-all peer-checked:bg-accent"></div>
              </label>
            </div>
            <div className="flex items-center justify-between border-t border-foreground/10 pt-4">
              <div>
                <p className="font-medium text-foreground">Developer Mode</p>
                <p className="text-sm text-foreground/70">
                  Show debug logs in browser console for troubleshooting
                </p>
              </div>
              <label className="relative inline-flex items-center cursor-pointer">
                <input
                  type="checkbox"
                  checked={devMode}
                  onChange={handleDevModeToggle}
                  className="sr-only peer"
                />
                <div className="w-11 h-6 bg-gray-200 dark:bg-gray-700 peer-focus:outline-none peer-focus:ring-4 peer-focus:ring-accent/30 rounded-full peer peer-checked:after:translate-x-full peer-checked:after:border-white after:content-[''] after:absolute after:top-[2px] after:left-[2px] after:bg-white after:border-gray-300 after:border after:rounded-full after:h-5 after:w-5 after:transition-all peer-checked:bg-accent"></div>
              </label>
            </div>
            <div className="flex items-center justify-between border-t border-foreground/10 pt-4">
              <div>
                <p className="font-medium text-foreground">Guided Setup Wizard</p>
                <p className="text-sm text-foreground/70">
                  Walk through creating a collection, adding a path, scanning, and reviewing results.
                </p>
              </div>
              <div className="flex items-center gap-2">
                <button
                  onClick={() => tutorial.startTutorial()}
                  className="px-3 py-2 bg-primary text-primary-foreground rounded-lg hover:opacity-90 transition-opacity"
                >
                  Start
                </button>
                <button
                  onClick={() => tutorial.resetTutorial()}
                  className="px-3 py-2 bg-foreground/10 text-foreground rounded-lg hover:bg-foreground/20 transition-colors"
                >
                  Reset
                </button>
              </div>
            </div>
          </div>
        </div>

        {/* Notification History */}
        <div className="bg-card rounded-lg shadow border border-foreground/10 p-6">
          <h3 className="text-xl font-semibold mb-4 flex items-center gap-2 text-foreground">
            <Bell className="w-5 h-5" />
            Notification History
          </h3>
          {notifications.length === 0 ? (
            <p className="text-foreground/70">No notifications yet</p>
          ) : (
            <div className="space-y-2">
              {notifications.map((notif, idx) => (
                <div key={idx} className="p-3 bg-foreground/5 rounded border border-foreground/10">
                  <p className="text-sm text-foreground">{notif.message}</p>
                  <p className="text-xs text-foreground/60">{notif.timestamp}</p>
                </div>
              ))}
              <button
                onClick={clearNotificationHistory}
                className="text-sm text-accent hover:text-accent/80 mt-2"
              >
                Clear History
              </button>
            </div>
          )}
        </div>

        {/* System */}
        <div className="bg-card rounded-lg shadow border border-foreground/10 p-6">
          <h3 className="text-xl font-semibold mb-4 flex items-center gap-2 text-foreground">
            <Server className="w-5 h-5" />
            System
          </h3>

          <div>
            <p className="text-sm text-foreground/70 mb-1">Open in a browser</p>
            <div className="flex items-center gap-2 flex-wrap">
              <a
                href={appUrl}
                target="_blank"
                rel="noopener noreferrer"
                className="font-mono text-sm text-accent hover:underline break-all"
              >
                {appUrl}
              </a>
              <button
                type="button"
                onClick={copyAppUrl}
                className="inline-flex items-center gap-1 px-2 py-1 text-xs rounded border border-foreground/20 text-foreground hover:bg-foreground/10"
              >
                {copied ? <Check className="w-3.5 h-3.5" /> : <Copy className="w-3.5 h-3.5" />}
                {copied ? 'Copied' : 'Copy'}
              </button>
            </div>
            <p className="text-xs text-foreground/50 mt-1">
              The desktop app serves this UI on a local port. Paste this into a browser to use it there,
              including the browser dev tools.
            </p>
          </div>

          <div className="grid grid-cols-1 sm:grid-cols-2 gap-x-6 gap-y-2 text-sm border-t border-foreground/10 mt-4 pt-4">
            {[
              ['Port', window.location.port || '(default)'],
              ['Version', appInfo?.version ?? '…'],
              ['Application', appInfo?.appName ?? 'OpenFixity'],
              ['Uptime', appInfo ? formatUptime(appInfo.uptimeMillis) : '…'],
              ['Java', appInfo ? `${appInfo.javaVersion} (${appInfo.javaVendor})` : '…'],
              ['OS', appInfo ? `${appInfo.osName} ${appInfo.osArch}` : '…'],
              ['Dropwizard', appInfo?.dropwizardVersion ?? '…'],
            ].map(([label, value]) => (
              <div key={label} className="flex justify-between gap-4">
                <span className="text-foreground/60">{label}</span>
                <span className="text-foreground font-mono text-right break-all">{value}</span>
              </div>
            ))}
          </div>
        </div>

        {/* About */}
        <div className="bg-card rounded-lg shadow border border-foreground/10 p-6">
          <h3 className="text-xl font-semibold mb-4 flex items-center gap-2 text-foreground">
            <Info className="w-5 h-5" />
            About OpenFixity
          </h3>
          <div className="space-y-2 text-sm">
            <p className="text-foreground">
              <strong>OpenFixity</strong> - File integrity monitoring and verification
            </p>
            <p className="text-foreground/70">
              Monitor file collections, generate checksums, track integrity over time
            </p>
            <div className="border-t border-foreground/10 pt-4 mt-4 space-y-3">
              <div className="flex flex-wrap gap-2">
                <a
                  href="https://openpreservation.org"
                  target="_blank"
                  rel="noopener noreferrer"
                  className="inline-flex items-center gap-1.5 px-3 py-1.5 text-sm rounded-lg border border-foreground/20 text-foreground hover:bg-foreground/10 transition-colors"
                >
                  <ExternalLink className="w-3.5 h-3.5" /> Open Preservation Foundation
                </a>
                <a
                  href="https://github.com/openpreserve/OpenFixity"
                  target="_blank"
                  rel="noopener noreferrer"
                  className="inline-flex items-center gap-1.5 px-3 py-1.5 text-sm rounded-lg border border-foreground/20 text-foreground hover:bg-foreground/10 transition-colors"
                >
                  <GitBranch className="w-3.5 h-3.5" /> GitHub Repository
                </a>
              </div>
              <div className="flex flex-wrap items-center gap-3 rounded-lg border border-accent/30 bg-accent/10 px-3 py-2">
                <Bug className="w-4 h-4 text-accent flex-none" />
                <span className="text-sm text-foreground">Found a bug or have a request?</span>
                <a
                  href="https://github.com/openpreserve/OpenFixity/issues"
                  target="_blank"
                  rel="noopener noreferrer"
                  className="sm:ml-auto inline-flex items-center gap-1 px-3 py-1.5 text-sm rounded-lg bg-primary text-primary-foreground hover:opacity-90 transition-opacity"
                >
                  Report an issue <ExternalLink className="w-3.5 h-3.5" />
                </a>
              </div>
            </div>
          </div>
        </div>

        {/* Advanced Settings */}
        <div className="bg-card rounded-lg shadow border border-foreground/10 p-6">
          <h3 className="text-xl font-semibold mb-4 text-foreground">Advanced Settings</h3>
          <div className="flex items-center justify-between">
            <div>
              <p className="font-medium text-foreground">Cron Scheduling Interface</p>
              <p className="text-sm text-foreground/70">
                Add a raw Quartz cron option when creating scheduled scans, alongside the frequency presets.
              </p>
            </div>
            <label className="relative inline-flex items-center cursor-pointer">
              <input
                type="checkbox"
                checked={cronScheduling}
                onChange={handleCronSchedulingToggle}
                className="sr-only peer"
              />
              <div className="w-11 h-6 bg-gray-200 dark:bg-gray-700 peer-focus:outline-none peer-focus:ring-4 peer-focus:ring-accent/30 rounded-full peer peer-checked:after:translate-x-full peer-checked:after:border-white after:content-[''] after:absolute after:top-[2px] after:left-[2px] after:bg-white after:border-gray-300 after:border after:rounded-full after:h-5 after:w-5 after:transition-all peer-checked:bg-accent"></div>
            </label>
          </div>
        </div>
      </div>
    </div>
  )
}
