import { StyleSheet, View } from 'react-native';

import { useTheme } from '@/theme';
import { Card } from './Card';
import { Icon, type IconName } from './Icon';
import { Text } from './Text';

type Props = {
  label: string;
  value: string | number;
  icon?: IconName;
  color?: string;
  sublabel?: string;
};

/** Compact metric card used in habit detail + analytics grids. */
export function StatTile({ label, value, icon, color, sublabel }: Props) {
  const { colors } = useTheme();
  const tint = color ?? colors.primary;
  return (
    <Card style={styles.card}>
      {icon ? (
        <View style={[styles.bubble, { backgroundColor: colors.primarySoft }]}>
          <Icon name={icon} size={18} color={tint} />
        </View>
      ) : null}
      <Text variant="title" color={tint}>
        {value}
      </Text>
      <Text variant="footnote" tone="secondary">
        {label}
      </Text>
      {sublabel ? (
        <Text variant="caption" tone="muted">
          {sublabel}
        </Text>
      ) : null}
    </Card>
  );
}

const styles = StyleSheet.create({
  card: { gap: 3, minWidth: 0 },
  bubble: {
    width: 34,
    height: 34,
    borderRadius: 17,
    alignItems: 'center',
    justifyContent: 'center',
    marginBottom: 4,
  },
});
