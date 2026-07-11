import { Pressable, StyleSheet, View } from 'react-native';

import { useTheme } from '@/theme';
import { Icon } from './Icon';
import { Text } from './Text';

type Props = {
  value: number;
  onChange: (v: number) => void;
  min?: number;
  max?: number;
  step?: number;
  suffix?: string;
};

export function Stepper({ value, onChange, min = 0, max = 9999, step = 1, suffix }: Props) {
  const { colors, radius } = useTheme();
  const dec = () => onChange(Math.max(min, value - step));
  const inc = () => onChange(Math.min(max, value + step));

  const btn = (icon: 'remove' | 'add', onPress: () => void, disabled: boolean) => (
    <Pressable
      onPress={onPress}
      disabled={disabled}
      style={[
        styles.btn,
        { backgroundColor: colors.surface, borderRadius: radius.sm, opacity: disabled ? 0.4 : 1 },
      ]}>
      <Icon name={icon} size={20} color={colors.text} />
    </Pressable>
  );

  return (
    <View style={[styles.wrap, { backgroundColor: colors.surfaceAlt, borderRadius: radius.md }]}>
      {btn('remove', dec, value <= min)}
      <Text variant="bodyStrong" style={styles.value}>
        {value}
        {suffix ? ` ${suffix}` : ''}
      </Text>
      {btn('add', inc, value >= max)}
    </View>
  );
}

const styles = StyleSheet.create({
  wrap: { flexDirection: 'row', alignItems: 'center', padding: 4, gap: 4, alignSelf: 'flex-start' },
  btn: { width: 38, height: 38, alignItems: 'center', justifyContent: 'center' },
  value: { minWidth: 64, textAlign: 'center' },
});
