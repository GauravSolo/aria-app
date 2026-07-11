import { Text as RNText, type TextProps } from 'react-native';

import { useTheme, type TypeVariant } from '@/theme';

type Tone =
  | 'default'
  | 'secondary'
  | 'muted'
  | 'primary'
  | 'danger'
  | 'success'
  | 'warning'
  | 'onPrimary';

export type AppTextProps = TextProps & {
  variant?: TypeVariant;
  tone?: Tone;
  color?: string;
  center?: boolean;
};

export function Text({
  variant = 'body',
  tone = 'default',
  color,
  center,
  style,
  ...rest
}: AppTextProps) {
  const { colors, typography } = useTheme();

  const toneColor: Record<Tone, string> = {
    default: colors.text,
    secondary: colors.textSecondary,
    muted: colors.textMuted,
    primary: colors.primary,
    danger: colors.danger,
    success: colors.success,
    warning: colors.warning,
    onPrimary: colors.onPrimary,
  };

  return (
    <RNText
      style={[
        typography[variant],
        { color: color ?? toneColor[tone] },
        center && { textAlign: 'center' },
        style,
      ]}
      {...rest}
    />
  );
}
