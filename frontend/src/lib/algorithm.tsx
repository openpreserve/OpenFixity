import type { AlgorithmInfo } from '@/types/api'

/**
 * Checksum-algorithm helpers shared across the scan UIs.
 *
 * Background: the Java backend only honors a chosen algorithm on the per-path
 * scan endpoint (`POST /api/paths/{id}/scan/{algorithm}/`). It has no app-wide
 * or per-collection algorithm setting, and its default is `Algorithms.DEFAULT`
 * — the first declared-and-available algorithm, i.e. SHA-1. The algorithm list
 * from `GET /api/digests/algorithms/` is returned in preference order with that
 * default first, so `algorithms[0]` is the backend default.
 */

/** Backend default (Algorithms.DEFAULT); fallback before the list has loaded. */
export const DEFAULT_ALGORITHM = 'SHA-1'

/** The algorithm a scan should default to: the backend default (first in the
 * preference-ordered list), falling back to SHA-1 if the list isn't loaded. */
export function defaultAlgorithm(algorithms?: AlgorithmInfo[]): string {
  return algorithms?.[0]?.id ?? DEFAULT_ALGORITHM
}

/**
 * Inline note shown beneath an algorithm picker. Scanning a path with a
 * different algorithm than before can't be compared to earlier scans, so those
 * files show as "Unverified" until the path is re-scanned with the new one.
 */
export function AlgorithmHint({ className = '' }: { className?: string }) {
  return (
    <p className={`text-xs text-foreground/50 ${className}`}>
      Tip: scan a path with the same algorithm each time. A different algorithm can’t be compared
      to earlier scans, so those files show as <span className="font-medium">Unverified</span> until re-scanned.
    </p>
  )
}
