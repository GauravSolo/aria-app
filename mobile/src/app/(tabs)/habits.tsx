import { router } from 'expo-router';
import { useMemo } from 'react';
import { StyleSheet, View } from 'react-native';

import { HabitCard } from '@/components/habits/HabitCard';
import { Card, EmptyState, Fab, Header, ProgressBar, Screen, Text } from '@/components/ui';
import { prettyDate, todayKey } from '@/lib/date';
import { computeHabitStats } from '@/lib/streaks';
import { toggleHabitDone, useHabitLogs, useHabits } from '@/stores/habits';
import { useAuth } from '@/stores/auth';
import type { Habit } from '@/types/db';

export default function HabitsScreen() {
  const uid = useAuth((s) => s.user?.id ?? s.guestId ?? '');
  const habitItems = useHabits((s) => s.items);
  const logItems = useHabitLogs((s) => s.items);
  const today = todayKey();

  const habits = useMemo(
    () =>
      Object.values(habitItems)
        .filter((h) => !h.deleted_at && h.user_id === uid && !h.is_archived)
        .sort((a, b) => a.sort_order - b.sort_order || a.created_at.localeCompare(b.created_at)),
    [habitItems, uid],
  );

  const countsByHabit = useMemo(() => {
    const m: Record<string, Record<string, number>> = {};
    for (const l of Object.values(logItems)) {
      if (l.deleted_at || l.user_id !== uid) continue;
      (m[l.habit_id] ??= {})[l.log_date] = l.count;
    }
    return m;
  }, [logItems, uid]);

  const rows = useMemo(
    () => habits.map((h) => ({ h, stats: computeHabitStats(h, countsByHabit[h.id] ?? {}, today) })),
    [habits, countsByHabit, today],
  );

  const scheduled = rows.filter((r) => r.stats.scheduledToday);
  const doneCount = scheduled.filter((r) => r.stats.doneToday).length;

  const openNew = () => router.push('/habit-form');
  const openDetail = (h: Habit) => router.push(`/habit/${h.id}`);

  return (
    <Screen padded={false}>
      <View style={styles.pad}>
        <Header title="Habits" subtitle={prettyDate(today)} />
      </View>

      {habits.length === 0 ? (
        <EmptyState
          icon="flame-outline"
          title="Build your first habit"
          message="Track daily routines like reading, workouts or coding — and watch your streaks grow."
        />
      ) : (
        <Screen scroll padded contentStyle={{ paddingTop: 8 }}>
          {scheduled.length > 0 ? (
            <Card style={{ gap: 8 }}>
              <View style={styles.summaryTop}>
                <Text variant="bodyStrong">Today’s habits</Text>
                <Text variant="subhead" tone="secondary">
                  {doneCount}/{scheduled.length}
                </Text>
              </View>
              <ProgressBar progress={scheduled.length ? doneCount / scheduled.length : 0} />
            </Card>
          ) : null}

          {rows.map(({ h, stats }) => (
            <HabitCard
              key={h.id}
              habit={h}
              streak={stats.current}
              count={stats.todayCount}
              target={Math.max(1, h.target_count || 1)}
              scheduledToday={stats.scheduledToday}
              onToggle={() => toggleHabitDone(h, today)}
              onPress={() => openDetail(h)}
            />
          ))}
        </Screen>
      )}

      <Fab onPress={openNew} />
    </Screen>
  );
}

const styles = StyleSheet.create({
  pad: { paddingHorizontal: 16 },
  summaryTop: { flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center' },
});
