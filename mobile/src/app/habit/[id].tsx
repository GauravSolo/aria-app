import { router, useLocalSearchParams } from 'expo-router';
import { useMemo } from 'react';
import { StyleSheet, View } from 'react-native';

import { HabitCalendar } from '@/components/habits/HabitCalendar';
import {
  Button,
  Card,
  EmptyState,
  IconButton,
  ProgressBar,
  Screen,
  StatTile,
  Stepper,
  Text,
} from '@/components/ui';
import { todayKey } from '@/lib/date';
import { buildCalendar, computeHabitStats, frequencyLabel } from '@/lib/streaks';
import { useAuth } from '@/stores/auth';
import { adjustHabitCount, toggleHabitDone, useHabitLogs, useHabits } from '@/stores/habits';
import { categoryColors, useTheme } from '@/theme';

export default function HabitDetailScreen() {
  const { colors, spacing } = useTheme();
  const { id } = useLocalSearchParams<{ id: string }>();
  const uid = useAuth((s) => s.user?.id ?? s.guestId ?? '');
  const habitItems = useHabits((s) => s.items);
  const logItems = useHabitLogs((s) => s.items);
  const today = todayKey();

  const habit = id ? habitItems[id] : undefined;

  const counts = useMemo(() => {
    const m: Record<string, number> = {};
    if (!habit) return m;
    for (const l of Object.values(logItems)) {
      if (!l.deleted_at && l.habit_id === habit.id) m[l.log_date] = l.count;
    }
    return m;
  }, [logItems, habit]);

  const stats = useMemo(
    () => (habit ? computeHabitStats(habit, counts, today) : null),
    [habit, counts, today],
  );
  const calendar = useMemo(
    () => (habit ? buildCalendar(habit, counts, 16, today) : []),
    [habit, counts, today],
  );

  if (!habit || !stats || habit.deleted_at || habit.user_id !== uid) {
    return (
      <Screen>
        <View style={styles.bar}>
          <IconButton name="chevron-back" onPress={() => router.back()} filled />
        </View>
        <EmptyState icon="alert-circle-outline" title="Habit not found" />
      </Screen>
    );
  }

  const color = habit.color ?? categoryColors[habit.category];
  const target = Math.max(1, habit.target_count || 1);

  return (
    <Screen scroll edges={['top', 'left', 'right']}>
      <View style={styles.bar}>
        <IconButton name="chevron-back" onPress={() => router.back()} filled />
        <IconButton
          name="create-outline"
          onPress={() => router.push({ pathname: '/habit-form', params: { id: habit.id } })}
          filled
        />
      </View>

      <View style={{ gap: 4 }}>
        <Text variant="display">{habit.name}</Text>
        <Text variant="callout" tone="secondary">
          {frequencyLabel(habit)} · target {target}/day
        </Text>
      </View>

      {/* Today's control */}
      <Card style={{ gap: 12 }}>
        <Text variant="caption" tone="muted">
          TODAY
        </Text>
        {!stats.scheduledToday ? (
          <Text variant="body" tone="secondary">
            Not scheduled today — enjoy your rest day.
          </Text>
        ) : target === 1 ? (
          <Button
            title={stats.doneToday ? 'Completed ✓' : 'Mark as done'}
            variant={stats.doneToday ? 'secondary' : 'primary'}
            icon={stats.doneToday ? 'checkmark-circle' : 'ellipse-outline'}
            fullWidth
            onPress={() => toggleHabitDone(habit, today)}
          />
        ) : (
          <View style={styles.countRow}>
            <View>
              <Text variant="title">
                {stats.todayCount}
                <Text variant="body" tone="muted">
                  {' '}
                  / {target}
                </Text>
              </Text>
              <Text variant="footnote" tone="secondary">
                {stats.doneToday ? 'Goal reached 🎉' : 'Keep going'}
              </Text>
            </View>
            <Stepper
              value={stats.todayCount}
              onChange={(v) => adjustHabitCount(habit, today, v - stats.todayCount)}
              min={0}
              max={target * 4}
            />
          </View>
        )}
      </Card>

      {/* Stats */}
      <View style={styles.grid}>
        <View style={styles.col}>
          <StatTile label="Current streak" value={`${stats.current}`} icon="flame" color={colors.warning} sublabel="days" />
        </View>
        <View style={styles.col}>
          <StatTile label="Longest streak" value={`${stats.longest}`} icon="trophy" color={colors.primary} sublabel="days" />
        </View>
        <View style={styles.col}>
          <StatTile label="Total done" value={`${stats.totalCompleted}`} icon="checkmark-done" color={colors.success} />
        </View>
        <View style={styles.col}>
          <StatTile label="Success rate" value={`${stats.successPct}%`} icon="trending-up" color={colors.accent} />
        </View>
      </View>

      {/* Week / month progress */}
      <Card style={{ gap: 14 }}>
        <ProgressRow
          label="This week"
          done={stats.weekDone}
          total={stats.weekTotal}
          color={color}
        />
        <ProgressRow
          label="This month"
          done={stats.monthDone}
          total={stats.monthTotal}
          color={color}
        />
      </Card>

      {/* Calendar */}
      <Card style={{ gap: 12 }}>
        <Text variant="bodyStrong">Completion calendar</Text>
        <HabitCalendar weeks={calendar} color={color} />
      </Card>

      {habit.notes ? (
        <Card style={{ gap: 6 }}>
          <Text variant="caption" tone="muted">
            NOTES
          </Text>
          <Text variant="body" tone="secondary">
            {habit.notes}
          </Text>
        </Card>
      ) : null}
      <View style={{ height: spacing.xl }} />
    </Screen>
  );
}

function ProgressRow({
  label,
  done,
  total,
  color,
}: {
  label: string;
  done: number;
  total: number;
  color: string;
}) {
  return (
    <View style={{ gap: 6 }}>
      <View style={styles.progressTop}>
        <Text variant="subhead">{label}</Text>
        <Text variant="subhead" tone="secondary">
          {done}/{total}
        </Text>
      </View>
      <ProgressBar progress={total ? done / total : 0} color={color} />
    </View>
  );
}

const styles = StyleSheet.create({
  bar: { flexDirection: 'row', alignItems: 'center', justifyContent: 'space-between' },
  grid: { flexDirection: 'row', flexWrap: 'wrap', gap: 12 },
  col: { flexGrow: 1, flexBasis: '46%' },
  countRow: { flexDirection: 'row', alignItems: 'center', justifyContent: 'space-between' },
  progressTop: { flexDirection: 'row', justifyContent: 'space-between' },
});
