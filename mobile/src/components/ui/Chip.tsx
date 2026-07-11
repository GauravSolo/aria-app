import { StyleSheet, View } from 'react-native';

import { useTheme } from '@/theme';
import { Icon, type IconName } from './Icon';
import { Text } from './Text';

type Props = {
  label: string;
  color?: string;
  icon?: IconName;
};

/** Small read-only pill (category, recurrence, priority…) with an optional dot. */
export function Chip({ label, color, icon }: Props) {
  const { colors, radius } = useTheme();
  return (
    <View
      style={[
        styles.chip,
        { backgroundColor: colors.surfaceAlt, borderRadius: radius.pill },
      ]}>
      {color ? <View style={[styles.dot, { backgroundColor: color }]} /> : null}
      {icon ? <Icon name={icon} size={12} color={colors.textSecondary} /> : null}
      <Text variant="caption" tone="secondary">
        {label}
      </Text>
    </View>
  );
}

const styles = StyleSheet.create({
  chip: { flexDirection: 'row', alignItems: 'center', gap: 5, paddingHorizontal: 9, paddingVertical: 4 },
  dot: { width: 7, height: 7, borderRadius: 4 },
});
