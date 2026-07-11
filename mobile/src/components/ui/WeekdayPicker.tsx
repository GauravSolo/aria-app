import { Pressable, StyleSheet, View } from 'react-native';

import { useTheme } from '@/theme';
import { Text } from './Text';

const LABELS = ['S', 'M', 'T', 'W', 'T', 'F', 'S'];

type Props = {
  value: number[]; // 0=Sun … 6=Sat
  onChange: (days: number[]) => void;
};

export function WeekdayPicker({ value, onChange }: Props) {
  const { colors } = useTheme();
  const toggle = (d: number) =>
    onChange(value.includes(d) ? value.filter((x) => x !== d) : [...value, d].sort());

  return (
    <View style={styles.row}>
      {LABELS.map((label, d) => {
        const active = value.includes(d);
        return (
          <Pressable
            key={d}
            onPress={() => toggle(d)}
            style={[
              styles.day,
              {
                backgroundColor: active ? colors.primary : colors.surfaceAlt,
                borderColor: active ? colors.primary : colors.border,
              },
            ]}>
            <Text variant="subhead" color={active ? colors.onPrimary : colors.textSecondary}>
              {label}
            </Text>
          </Pressable>
        );
      })}
    </View>
  );
}

const styles = StyleSheet.create({
  row: { flexDirection: 'row', gap: 6, justifyContent: 'space-between' },
  day: {
    flex: 1,
    aspectRatio: 1,
    borderRadius: 999,
    borderWidth: 1,
    alignItems: 'center',
    justifyContent: 'center',
  },
});
