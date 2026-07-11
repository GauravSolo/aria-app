import { StyleSheet, View } from 'react-native';

import { Card, Checkbox, Icon, Text } from '@/components/ui';
import { frequencyLabel } from '@/lib/streaks';
import { categoryColors, useTheme } from '@/theme';
import type { Habit } from '@/types/db';

type Props = {
  habit: Habit;
  streak: number;
  count: number;
  target: number;
  scheduledToday: boolean;
  onToggle: () => void;
  onPress: () => void;
};

export function HabitCard({
  habit,
  streak,
  count,
  target,
  scheduledToday,
  onToggle,
  onPress,
}: Props) {
  const { colors } = useTheme();
  const color = habit.color ?? categoryColors[habit.category];
  const done = count >= target;

  return (
    <Card onPress={onPress} style={styles.card}>
      <View style={[styles.icon, { backgroundColor: color + '22' }]}>
        <Icon name={(habit.icon as never) ?? 'ellipse'} size={18} color={color} />
      </View>

      <View style={styles.body}>
        <Text variant="bodyStrong" numberOfLines={1}>
          {habit.name}
        </Text>
        <View style={styles.meta}>
          <Icon name="repeat-outline" size={12} color={colors.textMuted} />
          <Text variant="footnote" tone="secondary">
            {frequencyLabel(habit)}
          </Text>
          {streak > 0 ? (
            <>
              <Text variant="footnote" tone="muted">
                ·
              </Text>
              <Icon name="flame" size={12} color={colors.warning} />
              <Text variant="footnote" tone="secondary">
                {streak}
              </Text>
            </>
          ) : null}
          {target > 1 ? (
            <>
              <Text variant="footnote" tone="muted">
                ·
              </Text>
              <Text variant="footnote" tone="secondary">
                {count}/{target}
              </Text>
            </>
          ) : null}
        </View>
      </View>

      {scheduledToday ? (
        <Checkbox checked={done} onPress={onToggle} color={color} />
      ) : (
        <Text variant="caption" tone="muted">
          Rest day
        </Text>
      )}
    </Card>
  );
}

const styles = StyleSheet.create({
  card: { flexDirection: 'row', alignItems: 'center', gap: 12 },
  icon: { width: 40, height: 40, borderRadius: 20, alignItems: 'center', justifyContent: 'center' },
  body: { flex: 1, gap: 3 },
  meta: { flexDirection: 'row', alignItems: 'center', gap: 5, flexWrap: 'wrap' },
});
