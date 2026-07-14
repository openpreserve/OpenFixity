import { cn } from '@/lib/utils'

/**
 * OpenFixity wordmark: an animated "radar sweep" SVG spinner that forms the
 * "O" of "OpenFixity" (Nunito, small-caps). Theme-aware via CSS variables —
 * the arc keeps the brand green while the track and text follow --foreground,
 * so it reads correctly on both light and dark themes.
 *
 * Styling lives in index.css under the `.logo` / `.spinner__*` classes.
 * `size` (any CSS length) drives both the ring and the wordmark size.
 */
export default function Logo({ size, className }: { size?: string; className?: string }) {
  return (
    <div
      className={cn('logo', className)}
      style={size ? ({ ['--of-spinner-size' as string]: size }) : undefined}
    >
      <svg className="logo__mark" xmlns="http://www.w3.org/2000/svg" viewBox="0 0 200 200" aria-hidden="true">
        <radialGradient
          id="of-spinner-gradient"
          cx=".66"
          fx=".66"
          cy=".3125"
          fy=".3125"
          gradientTransform="translate(1 1) scale(-1.5)"
        >
          <stop className="svg__stop-color" offset="0" stopOpacity="1" />
          <stop className="svg__stop-color" offset=".3" stopOpacity=".9" />
          <stop className="svg__stop-color" offset=".6" stopOpacity=".6" />
          <stop className="svg__stop-color" offset=".8" stopOpacity=".3" />
          <stop className="svg__stop-color" offset="1" stopOpacity="0" />
        </radialGradient>

        {/* faint full track */}
        <circle className="spinner__track" fill="none" strokeLinecap="round" cx="100" cy="100" r="70" />

        {/* the rotating arc — ~45% coverage of the 439.8 circumference */}
        <circle
          className="spinner__arc"
          fill="none"
          stroke="url(#of-spinner-gradient)"
          strokeLinecap="round"
          strokeDasharray="197.9 439.8"
          strokeDashoffset="0"
          cx="100"
          cy="100"
          r="70"
        />
      </svg>
      <span className="logo__text" aria-label="OpenFixity">
        <span className="logo__o">O</span>penFixity
      </span>
    </div>
  )
}
