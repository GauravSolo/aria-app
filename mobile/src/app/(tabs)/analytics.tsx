import dayjs from 'dayjs';
import { useMemo, useState } from 'react';
import { StyleSheet, View } from 'react-native';

import {
  BarChart,
  Card,
  EmptyState,
  Header,
  ProgressBar,
  Screen,
  Segmented,
  StatTile,
  Text,
} from '@/components/ui';
import { computeAnalytics } from '@/lib/analytics';
import { todayKey } from '@/lib/date';
import { formatMl } from '@/lib/water';
import { useAuth } from '@/stores/auth';
import { useHabitLogs, useHabits } from '@/stores/habits';
import { useTaskCompletions, useTasks } from '@/stores/tasks';
import { useWaterLogs, useWaterSettings } from '@/stores/water';
import { useTheme } from '@/theme';

export default function AnalyticsScreen() {
  const { colors } = useTheme();
  const uid = useAuth((s) => s.user?.id ?? s.guestId ?? '');
  const taskItems = useTasks((s) => s.items);
  const completionItems = useTaskCompletions((s) => s.items);
  const habitItems = useHabits((s) => s.items);
  const logItems = useHabitLogs((s) => s.items);
  const waterItems = useWaterLogs((s) => s.items);
  useWaterSettings((s) => s.byUser);
  const today = todayKey();
  const goal = useWaterSettings.getState().current().daily_goal_ml;

  const [days, setDays] = useState<7 | 30>(7);

  const mine = <T extends { deleted_at: string | null; user_id: string }>(items: Record<string, T>) =>
    Object.values(items).filter((r) => !r.deleted_at && r.user_id === uid);

  const result = useMemo(
    () =>
      computeAnalytics({
        tasks: mine(taskItems),
        completions: mine(completionItems),
        habits: mine(habitItems),
        habitLogs: mine(logItems),
        waterLogs: mine(waterItems),
        goalMl: goal,
        days,
        today,
      }),
    // eslint-disable-next-line react-hooks/exhaustive-deps
    [taskItems, completionItems, habitItems, logItems, waterItems, goal, days, today, uid],
  );

  const hasData =
    result.taskTotal > 0 || result.habitTotal > 0 || result.perDay.some((p) => p.ml > 0);

  const scoreBars = result.perDay.map((p) => ({
    label: p.label,
    value: p.score,
    highlight: p.date === today,
  }));
  const waterBars = result.perDay.map((p) => ({
    label: p.label,
    value: p.ml,
    highlight: p.date === today,
  }));

  return (
    <Screen scroll>
      <Header title="Stats" subtitle="Insights" />

      <Segmented
        options={[
          { label: 'Week', value: '7' },
          { label: 'Month', value: '30' },
        ]}
        value={String(days)}
        onChange={(v) => setDays(v === '30' ? 30 : 7)}
      />

      {!hasData ? (
        <EmptyState
          icon="stats-chart-outline"
          title="No data yet"
          message="Complete tasks, habits and log water to see your trends and summaries here."
        />
      ) : (
        <>
          {/* Productivity trend */}
          <Card style={{ gap: 14 }}>
            <View style={styles.cardHead}>
              <Text variant="bodyStrong">Productivity trend</Text>
              <Text variant="footnote" tone="secondary">
                last {days} days
              </Text>
            </View>
            <BarChart data={scoreBars} color={colors.primary} goal={100} />
          </Card>

          {/* Key rates */}
          <View style={styles.grid}>
            <View style={styles.col}>
              <StatTile label="Task completion" value={`${result.taskRate}%`} icon="checkmark-done" color={colors.primary} sublabel={`${result.taskDone}/${result.taskTotal} tasks`} />
            </View>
            <View style={styles.col}>
              <StatTile label="Habit completion" value={`${result.habitRate}%`} icon="flame" color={colors.warning} sublabel={`${result.habitDone}/${result.habitTotal} done`} />
            </View>
            <View style={styles.col}>
              <StatTile label="Water consistency" value={`${result.waterConsistency}%`} icon="water" color={colors.info} sublabel={`${result.waterDaysMet} days met goal`} />
            </View>
            <View style={styles.col}>
              <StatTile
                label="Best day"
                value={result.bestDay ? `${result.bestDay.score}` : '—'}
                icon="ribbon"
                color={colors.success}
                sublabel={result.bestDay ? dayjs(result.bestDay.date).format('ddd, MMM D') : 'No data'}
              />
            </View>
          </View>

          {/* Streaks + tasks */}
          <View style={styles.grid}>
            <View style={styles.col}>
              <StatTile label="Top current streak" value={result.topCurrentStreak} icon="flame" color={colors.warning} sublabel="days" />
            </View>
            <View style={styles.col}>
              <StatTile label="Longest streak" value={result.topLongestStreak} icon="trophy" color={colors.accent} sublabel="days" />
            </View>
            <View style={styles.col}>
              <StatTile label="Tasks completed" value={result.taskDone} icon="checkmark-circle" color={colors.success} />
            </View>
            <View style={styles.col}>
              <StatTile label="Tasks missed" value={result.taskMissed} icon="close-circle" color={colors.danger} />
            </View>
          </View>

          {/* Water chart */}
          <Card style={{ gap: 14 }}>
            <View style={styles.cardHead}>
              <Text variant="bodyStrong">Water intake</Text>
              <Text variant="footnote" tone="secondary">
                goal {formatMl(goal)}/day
              </Text>
            </View>
            <BarChart data={waterBars} color={colors.info} goal={goal} />
          </Card>

          {/* Habit breakdown */}
          {result.habitBreakdown.length > 0 ? (
            <Card style={{ gap: 14 }}>
              <Text variant="bodyStrong">Habit progress</Text>
              {result.habitBreakdown.map((h) => (
                <View key={h.id} style={{ gap: 6 }}>
                  <View style={styles.habitRow}>
                    <Text variant="subhead" numberOfLines={1} style={{ flex: 1 }}>
                      {h.name}
                    </Text>
                    <Text variant="footnote" tone="secondary">
                      {h.rate}% · 🔥 {h.current}
                    </Text>
                  </View>
                  <ProgressBar progress={h.rate / 100} color={h.color ?? colors.primary} />
                </View>
              ))}
            </Card>
          ) : null}

          {/* Missed tasks */}
          {result.missedTasks.length > 0 ? (
            <Card style={{ gap: 10 }}>
              <Text variant="bodyStrong">Missed tasks</Text>
              {result.missedTasks.map((m, i) => (
                <View key={`${m.id}-${i}`} style={styles.missedRow}>
                  <Text variant="subhead" numberOfLines={1} style={{ flex: 1 }}>
                    {m.title}
                  </Text>
                  <Text variant="caption" tone="muted">
                    {dayjs(m.date).format('MMM D')}
                  </Text>
                </View>
              ))}
            </Card>
          ) : null}
        </>
      )}
      <View style={{ height: 8 }} />
    </Screen>
  );
}

const styles = StyleSheet.create({
  cardHead: { flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center' },
  grid: { flexDirection: 'row', flexWrap: 'wrap', gap: 12 },
  col: { flexGrow: 1, flexBasis: '46%' },
  habitRow: { flexDirection: 'row', alignItems: 'center', gap: 8 },
  missedRow: { flexDirection: 'row', alignItems: 'center', justifyContent: 'space-between', gap: 8 },
});
