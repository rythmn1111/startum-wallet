// ─────────────────────────────────────────────────────────────────────────────
//  Design System — Color Tokens
//  Single source of truth. Never use raw hex values outside this file.
// ─────────────────────────────────────────────────────────────────────────────

export const Colors = {
  // ── Core Palette ───────────────────────────────────────────────────────────
  deepDark:     '#222831',   // primary background
  surface:      '#393E46',   // cards, inputs, bottom sheets
  brandOrange:  '#FD7014',   // primary CTA accent
  offWhite:     '#EEEEEE',   // primary text / icons

  // ── Derived (calculated consistently) ─────────────────────────────────────
  surfaceElevated: '#42484F',    // cards on top of surface
  orangeDim:       '#FD701420',  // 12% opacity — glows, tag backgrounds
  orangeMid:       '#FD701440',  // 25% opacity — pressed states, borders
  textMuted:       '#EEEEEE99',  // 60% opacity — secondary text
  textFaint:       '#EEEEEE40',  // 25% opacity — placeholders, dividers
  divider:         '#EEEEEE08',  // ultra-faint dividers
  surfaceBorder:   '#EEEEEE10',  // card borders

  // ── Semantic ───────────────────────────────────────────────────────────────
  success:  '#00D68F',
  error:    '#FF4757',
  warning:  '#FFB300',

  // ── Utility ────────────────────────────────────────────────────────────────
  transparent: 'transparent' as const,
  overlay:     'rgba(34,40,49,0.93)',
  overlayMid:  'rgba(34,40,49,0.7)',

  // ── Chain accent colors ────────────────────────────────────────────────────
  eth:  '#627EEA',
  sol:  '#9945FF',
  usdc: '#2775CA',
} as const;

export type ColorKey = keyof typeof Colors;

// ── Shadow Presets ─────────────────────────────────────────────────────────────
export const Shadows = {
  card: {
    shadowColor: '#000000',
    shadowOffset: { width: 0, height: 4 },
    shadowOpacity: 0.4,
    shadowRadius: 24,
    elevation: 8,
  },
  orangeGlow: {
    shadowColor: '#FD7014',
    shadowOffset: { width: 0, height: 0 },
    shadowOpacity: 0.30,
    shadowRadius: 20,
    elevation: 12,
  },
  orangeGlowStrong: {
    shadowColor: '#FD7014',
    shadowOffset: { width: 0, height: 0 },
    shadowOpacity: 0.50,
    shadowRadius: 28,
    elevation: 16,
  },
  subtle: {
    shadowColor: '#000000',
    shadowOffset: { width: 0, height: 2 },
    shadowOpacity: 0.2,
    shadowRadius: 8,
    elevation: 3,
  },
  successGlow: {
    shadowColor: '#00D68F',
    shadowOffset: { width: 0, height: 0 },
    shadowOpacity: 0.35,
    shadowRadius: 16,
    elevation: 8,
  },
  errorGlow: {
    shadowColor: '#FF4757',
    shadowOffset: { width: 0, height: 0 },
    shadowOpacity: 0.35,
    shadowRadius: 16,
    elevation: 8,
  },
} as const;

// ── Spacing — 8px base grid ────────────────────────────────────────────────────
export const Spacing = {
  xs:   4,
  sm:   8,
  md:   16,
  lg:   24,
  xl:   32,
  xxl:  48,
  xxxl: 64,
} as const;

// ── Border Radii ───────────────────────────────────────────────────────────────
export const Radius = {
  sm:   8,
  md:   12,
  lg:   16,
  xl:   24,
  full: 9999,
} as const;
