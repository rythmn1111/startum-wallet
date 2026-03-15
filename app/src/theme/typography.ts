// ─────────────────────────────────────────────────────────────────────────────
//  Design System — Typography Tokens
//  All font families, sizes, weights, and pre-built text styles.
// ─────────────────────────────────────────────────────────────────────────────

import {
  useFonts,
  Inter_300Light,
  Inter_400Regular,
  Inter_500Medium,
  Inter_600SemiBold,
  Inter_700Bold,
  Inter_800ExtraBold,
} from '@expo-google-fonts/inter';

import {
  SpaceMono_400Regular,
  SpaceMono_700Bold,
} from '@expo-google-fonts/space-mono';

import {
  Syne_700Bold,
  Syne_800ExtraBold,
} from '@expo-google-fonts/syne';

// ── Font Family Names ──────────────────────────────────────────────────────────
export const FontFamily = {
  interLight:     'Inter_300Light',
  inter:          'Inter_400Regular',
  interMedium:    'Inter_500Medium',
  interSemiBold:  'Inter_600SemiBold',
  interBold:      'Inter_700Bold',
  interXBold:     'Inter_800ExtraBold',
  mono:           'SpaceMono_400Regular',
  monoBold:       'SpaceMono_700Bold',
  syne:           'Syne_700Bold',
  syneXBold:      'Syne_800ExtraBold',
} as const;

// ── Font Size Scale ────────────────────────────────────────────────────────────
export const FontSize = {
  xs:    10,
  sm:    12,
  base:  14,
  md:    16,
  lg:    18,
  xl:    22,
  '2xl': 28,
  '3xl': 36,
  '4xl': 42,
  '5xl': 48,
  mono:  14,
  monoSm: 12,
  monoLg: 18,
  monoXl: 22,
} as const;

// ── Pre-built Text Style Objects ───────────────────────────────────────────────
// Import these directly in StyleSheet.create() calls.
export const TextStyles = {
  // Display — Syne headlines
  display3xl: { fontFamily: FontFamily.syneXBold,     fontSize: FontSize['3xl'], lineHeight: 44 },
  display2xl: { fontFamily: FontFamily.syne,          fontSize: FontSize['2xl'], lineHeight: 36 },
  displayXl:  { fontFamily: FontFamily.syne,          fontSize: FontSize.xl,    lineHeight: 30 },
  displayLg:  { fontFamily: FontFamily.syne,          fontSize: FontSize.lg,    lineHeight: 26 },

  // Body — Inter
  bodyXl:     { fontFamily: FontFamily.interBold,     fontSize: FontSize.xl,    lineHeight: 28 },
  bodyLg:     { fontFamily: FontFamily.interSemiBold, fontSize: FontSize.lg,    lineHeight: 26 },
  bodyMd:     { fontFamily: FontFamily.interMedium,   fontSize: FontSize.md,    lineHeight: 24 },
  bodyBase:   { fontFamily: FontFamily.inter,         fontSize: FontSize.base,  lineHeight: 20 },
  bodySm:     { fontFamily: FontFamily.inter,         fontSize: FontSize.sm,    lineHeight: 18 },
  bodyXs:     { fontFamily: FontFamily.inter,         fontSize: FontSize.xs,    lineHeight: 14 },

  // Utility
  label:      { fontFamily: FontFamily.interMedium,   fontSize: FontSize.sm,    lineHeight: 16, letterSpacing: 1.2 },
  caption:    { fontFamily: FontFamily.inter,         fontSize: FontSize.sm,    lineHeight: 16 },

  // Monospace — Space Mono
  mono:       { fontFamily: FontFamily.mono,          fontSize: FontSize.mono,   lineHeight: 20 },
  monoSm:     { fontFamily: FontFamily.mono,          fontSize: FontSize.monoSm, lineHeight: 18 },
  monoLg:     { fontFamily: FontFamily.monoBold,      fontSize: FontSize.monoLg, lineHeight: 26 },
  monoXl:     { fontFamily: FontFamily.monoBold,      fontSize: FontSize.monoXl, lineHeight: 30 },
} as const;

// ── Font Loading Hook ──────────────────────────────────────────────────────────
// Call once at the root of the app.
export function useLoadFonts() {
  return useFonts({
    Inter_300Light,
    Inter_400Regular,
    Inter_500Medium,
    Inter_600SemiBold,
    Inter_700Bold,
    Inter_800ExtraBold,
    SpaceMono_400Regular,
    SpaceMono_700Bold,
    Syne_700Bold,
    Syne_800ExtraBold,
  });
}

// Re-export font objects for direct use in useFonts() calls
export {
  Inter_300Light,
  Inter_400Regular,
  Inter_500Medium,
  Inter_600SemiBold,
  Inter_700Bold,
  Inter_800ExtraBold,
  SpaceMono_400Regular,
  SpaceMono_700Bold,
  Syne_700Bold,
  Syne_800ExtraBold,
};
