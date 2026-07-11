import { Pressable, StyleSheet, View } from 'react-native';

import { useTheme } from '@/theme';
import { Icon, type IconName } from './Icon';
import { Text } from './Text';

export type SegmentOption<T extends string> = {
  label: string;
  value: T;
  icon?: IconName;
};

type Props<T extends string> = {
  options: SegmentOption<T>[];
  value: T;
  onChange: (v: T) => void;
};

/** iOS-style segmented selector used across forms (theme, priority, frequency…). */
export function Segmented<T extends string>({ options, value, onChange }: Props<T>) {
  const { colors, radius } = useTheme();
  return (
    <View
      style={[
        styles.track,
        { backgroundColor: colors.surfaceAlt, borderRadius: radius.md, borderColor: colors.border },
      ]}>
      {options.map((opt) => {
        const active = opt.value === value;
        return (
          <Pressable
            key={opt.value}
            onPress={() => onChange(opt.value)}
            style={[
              styles.segment,
              { borderRadius: radius.sm },
              active && {
                backgroundColor: colors.surface,
                shadowColor: '#000',
                shadowOpacity: 0.08,
                shadowRadius: 4,
                shadowOffset: { width: 0, height: 1 },
                elevation: 1,
              },
            ]}>
            {opt.icon ? (
              <Icon
                name={opt.icon}
                size={16}
                color={active ? colors.primary : colors.textMuted}
              />
            ) : null}
            <Text
              variant="subhead"
              color={active ? colors.text : colors.textSecondary}
              numberOfLines={1}>
              {opt.label}
            </Text>
          </Pressable>
        );
      })}
    </View>
  );
}

const styles = StyleSheet.create({
  track: { flexDirection: 'row', padding: 4, borderWidth: 1, gap: 4 },
  segment: {
    flex: 1,
    flexDirection: 'row',
    gap: 6,
    alignItems: 'center',
    justifyContent: 'center',
    paddingVertical: 9,
    paddingHorizontal: 8,
  },
});
