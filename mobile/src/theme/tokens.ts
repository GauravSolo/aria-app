import type { TextStyle } from 'react-native';

/**
 * Aria design tokens — Indigo / Violet, light + dark.
 * Single source of truth for color/spacing/radius/typography across the app.
 */

export type ColorScheme = 'light' | 'dark';

export type Palette = {
  background: string;
  surface: string;
  surfaceAlt: string;
  surfaceElevated: string;
  border: string;
  borderStrong: string;
  text: string;
  textSecondary: string;
  textMuted: string;
  primary: string;
  primaryStrong: string;
  primarySoft: string;
  onPrimary: string;
  accent: string;
  accentSoft: string;
  success: string;
  successSoft: string;
  warning: string;
  warningSoft: string;
  danger: string;
  dangerSoft: string;
  info: string;
  infoSoft: string;
  track: string;
  overlay: string;
};

export const palette: Record<ColorScheme, Palette> = {
  light: {
    background: '#F6F7F9',
    surface: '#FFFFFF',
    surfaceAlt: '#F0F1F4',
    surfaceElevated: '#FFFFFF',
    border: '#E6E8EC',
    borderStrong: '#D5D8DF',

    text: '#16181D',
    textSecondary: '#5B616E',
    textMuted: '#8A909C',

    primary: '#6366F1',
    primaryStrong: '#4F46E5',
    primarySoft: '#EEF0FF',
    onPrimary: '#FFFFFF',

    accent: '#8B5CF6',
    accentSoft: '#F2ECFE',

    success: '#10B981',
    successSoft: '#E6F7F0',
    warning: '#F59E0B',
    warningSoft: '#FEF3E2',
    danger: '#EF4444',
    dangerSoft: '#FDECEC',
    info: '#3B82F6',
    infoSoft: '#E8F1FE',

    track: '#E6E8EC',
    overlay: 'rgba(16,18,24,0.45)',
  },
  dark: {
    background: '#0B0C0F',
    surface: '#15171C',
    surfaceAlt: '#1C1F26',
    surfaceElevated: '#1A1D24',
    border: '#262A33',
    borderStrong: '#333845',

    text: '#F2F3F5',
    textSecondary: '#A6ACB8',
    textMuted: '#6E7480',

    primary: '#818CF8',
    primaryStrong: '#6366F1',
    primarySoft: '#1E1B3A',
    onPrimary: '#FFFFFF',

    accent: '#A78BFA',
    accentSoft: '#241E3D',

    success: '#34D399',
    successSoft: '#0F2A22',
    warning: '#FBBF24',
    warningSoft: '#2C2410',
    danger: '#F87171',
    dangerSoft: '#311A1A',
    info: '#60A5FA',
    infoSoft: '#15233A',

    track: '#262A33',
    overlay: 'rgba(0,0,0,0.6)',
  },
};

/** Spacing scale (4pt base). */
export const spacing = {
  xs: 4,
  sm: 8,
  md: 12,
  lg: 16,
  xl: 20,
  xxl: 24,
  xxxl: 32,
  huge: 48,
} as const;

export const radius = {
  sm: 8,
  md: 12,
  lg: 16,
  xl: 20,
  xxl: 28,
  pill: 999,
} as const;

export type TypeVariant =
  | 'display'
  | 'title'
  | 'title2'
  | 'headline'
  | 'body'
  | 'bodyStrong'
  | 'callout'
  | 'subhead'
  | 'footnote'
  | 'caption';

export const typography: Record<TypeVariant, TextStyle> = {
  display: { fontSize: 32, lineHeight: 38, fontWeight: '800', letterSpacing: -0.5 },
  title: { fontSize: 26, lineHeight: 32, fontWeight: '700', letterSpacing: -0.3 },
  title2: { fontSize: 20, lineHeight: 26, fontWeight: '700', letterSpacing: -0.2 },
  headline: { fontSize: 17, lineHeight: 22, fontWeight: '600' },
  body: { fontSize: 16, lineHeight: 22, fontWeight: '400' },
  bodyStrong: { fontSize: 16, lineHeight: 22, fontWeight: '600' },
  callout: { fontSize: 15, lineHeight: 20, fontWeight: '500' },
  subhead: { fontSize: 14, lineHeight: 19, fontWeight: '500' },
  footnote: { fontSize: 13, lineHeight: 17, fontWeight: '400' },
  caption: { fontSize: 12, lineHeight: 16, fontWeight: '600', letterSpacing: 0.2 },
};

/** Domain colors used consistently across tasks / habits / charts. */
export const categoryColors: Record<string, string> = {
  work: '#6366F1',
  study: '#8B5CF6',
  health: '#10B981',
  personal: '#F59E0B',
  other: '#64748B',
};

export const priorityColors: Record<string, string> = {
  low: '#10B981',
  medium: '#F59E0B',
  high: '#EF4444',
};
