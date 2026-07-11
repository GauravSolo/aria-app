import { Pressable, StyleSheet, View } from 'react-native';

import { useTheme } from '@/theme';
import { Text } from './Text';

export type ChipOption<T extends string> = {
  label: string;
  value: T;
  color?: string;
};

type Props<T extends string> = {
  options: ChipOption<T>[];
  value: T;
  onChange: (v: T) => void;
};

/** Single-select wrap of chips (category, recurrence…). */
export function ChipSelect<T extends string>({ options, value, onChange }: Props<T>) {
  const { colors, radius } = useTheme();
  return (
    <View style={styles.wrap}>
      {options.map((opt) => {
        const active = opt.value === value;
        const accent = opt.color ?? colors.primary;
        return (
          <Pressable
            key={opt.value}
            onPress={() => onChange(opt.value)}
            style={[
              styles.chip,
              {
                borderRadius: radius.pill,
                backgroundColor: active ? accent : colors.surfaceAlt,
                borderColor: active ? accent : colors.border,
              },
            ]}>
            {opt.color ? (
              <View
                style={[
                  styles.dot,
                  { backgroundColor: active ? colors.onPrimary : opt.color },
                ]}
              />
            ) : null}
            <Text
              variant="subhead"
              color={active ? colors.onPrimary : colors.textSecondary}>
              {opt.label}
            </Text>
          </Pressable>
        );
      })}
    </View>
  );
}

const styles = StyleSheet.create({
  wrap: { flexDirection: 'row', flexWrap: 'wrap', gap: 8 },
  chip: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 6,
    paddingHorizontal: 14,
    paddingVertical: 9,
    borderWidth: 1,
  },
  dot: { width: 8, height: 8, borderRadius: 4 },
});
