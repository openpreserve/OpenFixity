import { useEffect, useState } from 'react';

/**
 * Client-only preferences kept in localStorage. These mirror the pattern used elsewhere in the
 * app (see SettingsPage) rather than living on the server, since they only affect this browser.
 */

const CRON_SCHEDULING_KEY = 'cronScheduling';
const SETTINGS_EVENT = 'openfixity:settings-changed';

export function isCronSchedulingEnabled(): boolean {
  return localStorage.getItem(CRON_SCHEDULING_KEY) === 'true';
}

export function setCronSchedulingEnabled(enabled: boolean): void {
  localStorage.setItem(CRON_SCHEDULING_KEY, String(enabled));
  // Notify listeners in this tab; the native 'storage' event only fires in other tabs.
  window.dispatchEvent(new Event(SETTINGS_EVENT));
}

/** Reactively track whether the advanced cron scheduling interface is enabled. */
export function useCronSchedulingEnabled(): boolean {
  const [enabled, setEnabled] = useState(isCronSchedulingEnabled);
  useEffect(() => {
    const sync = () => setEnabled(isCronSchedulingEnabled());
    window.addEventListener(SETTINGS_EVENT, sync);
    window.addEventListener('storage', sync);
    return () => {
      window.removeEventListener(SETTINGS_EVENT, sync);
      window.removeEventListener('storage', sync);
    };
  }, []);
  return enabled;
}
