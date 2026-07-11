import dayjs from 'dayjs';
import { router } from 'expo-router';
import { useMemo } from 'react';
import { Pressable, StyleSheet, View } from 'react-native';

import { TaskCard } from '@/components/tasks/TaskCard';
import {
  Card,
  CircularProgress,
  Checkbox,
  Icon,
  IconButton,
  ProgressBar,
  Screen,
  Text,
  type IconName,
} from '@/components/ui';
import { productivityScore } from '@/lib/analytics';
import { greeting, prettyDate, todayKey } from '@/lib/date';
import { taskOccursOn } from '@/lib/recurrence';
import { nextTriggerDate, reminderSummary } from '@/lib/reminders';
import { computeHabitStats } from '@/lib/streaks';
import { totalForDate, formatMl } from '@/lib/water';
import { useAuth } from '@/stores/auth';
import { toggleHabitDone, useHabitLogs, useHabits } from '@/stores/habits';
import { useReminders } from '@/stores/reminders';
import { isTaskDone, toggleTaskDone, useTaskCompletions, useTasks } from '@/stores/tasks';
import { logWater, useWaterLogs, useWaterSettings } from '@/stores/water';
import { categoryColors, useTheme } from '@/theme';

export default function TodayScreen() {
  const { colors } = useTheme();
  const uid = useAuth((s) => s.user?.id ?? s.guestId ?? '');
  const taskItems = useTasks((s) => s.items);
  useTaskCompletions((s) => s.items);
  const habitItems = useHabits((s) => s.items);
  const logItems = useHabitLogs((s) => s.items);
  const waterItems = useWaterLogs((s) => s.items);
  useWaterSettings((s) => s.byUser);
  const reminderItems = useReminders((s) => s.items);
  const today = todayKey();
  const settings = useWaterSettings.getState().current();

  // Tasks
  const dayTasks = useMemo(
    () =>
      Object.values(taskItems)
        .filter((t) => !t.deleted_at && t.user_id === uid && taskOccursOn(t, today))
        .sort((a, b) => (a.start_time ?? '').localeCompare(b.start_time ?? '')),
    [taskItems, uid, today],
  );
  const tasksDone = dayTasks.filter((t) => isTaskDone(t, today)).length;
  const pendingTasks = dayTasks.filter((t) => !isTaskDone(t, today)).slice(0, 3);

  // Habits
  const countsByHabit = useMemo(() => {
    const m: Record<string, Record<string, number>> = {};
    for (const l of Object.values(logItems)) {
      if (l.deleted_at || l.user_id !== uid) continue;
      (m[l.habit_id] ??= {})[l.log_date] = l.count;
    }
    return m;
  }, [logItems, uid]);
  const habitRows = useMemo(() => {
    return Object.values(habitItems)
      .filter((h) => !h.deleted_at && h.user_id === uid && !h.is_archived)
      .map((h) => ({ h, stats: computeHabitStats(h, countsByHabit[h.id] ?? {}, today) }));
  }, [habitItems, uid, countsByHabit, today]);
  const scheduledHabits = habitRows.filter((r) => r.stats.scheduledToday);
  const habitsDone = scheduledHabits.filter((r) => r.stats.doneToday).length;
  const topStreak = habitRows.reduce((m, r) => Math.max(m, r.stats.current), 0);

  // Water
  const waterLogs = useMemo(
    () => Object.values(waterItems).filter((l) => !l.deleted_at && l.user_id === uid),
    [waterItems, uid],
  );
  const waterMl = totalForDate(waterLogs, today);
  const goal = settings.daily_goal_ml;

  // Reminders (upcoming)
  const upcoming = useMemo(() => {
    return Object.values(reminderItems)
      .filter((r) => !r.deleted_at && r.user_id === uid && r.is_enabled)
      .map((r) => ({ r, next: nextTriggerDate(r) }))
      .filter((x) => x.next)
      .sort((a, b) => (a.next as Date).getTime() - (b.next as Date).getTime())
      .slice(0, 3);
  }, [reminderItems, uid]);

  const score = productivityScore({
    taskRate: dayTasks.length ? tasksDone / dayTasks.length : undefined,
    habitRate: scheduledHabits.length ? habitsDone / scheduledHabits.length : undefined,
    waterRate: goal ? waterMl / goal : undefined,
  });
  const scoreLabel = score >= 80 ? 'On fire 🔥' : score >= 50 ? 'Nice progress' : 'Let’s get going';

  return (
    <Screen scroll>
      <Header />

      {/* Productivity score */}
      <Card style={styles.scoreCard}>
        <CircularProgress progress={score / 100} size={92} strokeWidth={9} color={colors.primary}>
          <Text variant="title2" color={colors.primary}>
            {score}
          </Text>
        </CircularProgress>
        <View style={{ flex: 1, gap: 6 }}>
          <Text variant="bodyStrong">Productivity today</Text>
          <Text variant="footnote" tone="secondary">
            {scoreLabel}
          </Text>
          <View style={styles.scoreMeta}>
            <MiniStat icon="checkmark-done" label={`${tasksDone}/${dayTasks.length}`} color={colors.primary} />
            <MiniStat icon="flame" label={`${habitsDone}/${scheduledHabits.length}`} color={colors.warning} />
            <MiniStat icon="water" label={`${Math.round(goal ? (waterMl / goal) * 100 : 0)}%`} color={colors.info} />
          </View>
        </View>
      </Card>

      {/* Quick add */}
      <View style={styles.quickRow}>
        <QuickAdd icon="add-circle" label="Task" color={colors.primary} onPress={() => router.push({ pathname: '/task-form', params: { date: today } })} />
        <QuickAdd icon="flame" label="Habit" color={colors.warning} onPress={() => router.push('/habit-form')} />
        <QuickAdd icon="water" label="Water" color={colors.info} onPress={() => logWater(settings.glass_size_ml)} />
        <QuickAdd icon="notifications" label="Remind" color={colors.accent} onPress={() => router.push('/reminder-form')} />
      </View>

      {/* Tasks */}
      <SectionHeader title="Today’s tasks" right={`${tasksDone}/${dayTasks.length}`} onPress={() => router.push('/planner')} />
      {dayTasks.length === 0 ? (
        <Card>
          <Text variant="subhead" tone="secondary">
            No tasks today. Tap “Task” to plan your day.
          </Text>
        </Card>
      ) : pendingTasks.length === 0 ? (
        <Card>
          <Text variant="subhead" tone="success">
            All tasks done — great work! 🎉
          </Text>
        </Card>
      ) : (
        pendingTasks.map((t) => (
          <TaskCard
            key={t.id}
            task={t}
            done={false}
            onToggle={() => toggleTaskDone(t, today)}
            onPress={() => router.push({ pathname: '/task-form', params: { id: t.id, date: today } })}
          />
        ))
      )}

      {/* Habits */}
      {scheduledHabits.length > 0 ? (
        <>
          <SectionHeader title="Today’s habits" right={`${habitsDone}/${scheduledHabits.length}`} onPress={() => router.push('/habits')} />
          <Card style={{ gap: 12 }}>
            {scheduledHabits.slice(0, 4).map(({ h, stats }) => {
              const color = h.color ?? categoryColors[h.category];
              return (
                <Pressable key={h.id} style={styles.habitRow} onPress={() => router.push(`/habit/${h.id}`)}>
                  <Checkbox
                    checked={stats.doneToday}
                    color={color}
                    onPress={() => toggleHabitDone(h, today)}
                  />
                  <Text variant="body" style={{ flex: 1 }} numberOfLines={1}>
                    {h.name}
                  </Text>
                  {stats.current > 0 ? (
                    <View style={styles.streakChip}>
                      <Icon name="flame" size={13} color={colors.warning} />
                      <Text variant="footnote" tone="secondary">
                        {stats.current}
                      </Text>
                    </View>
                  ) : null}
                </Pressable>
              );
            })}
          </Card>
        </>
      ) : null}

      {/* Water */}
      <SectionHeader title="Water" right={`${formatMl(waterMl)} / ${formatMl(goal)}`} onPress={() => router.push('/water')} />
      <Card style={styles.waterCard}>
        <CircularProgress progress={goal ? waterMl / goal : 0} size={64} strokeWidth={7} color={colors.info}>
          <Icon name="water" size={22} color={colors.info} />
        </CircularProgress>
        <View style={{ flex: 1, gap: 6 }}>
          <ProgressBar progress={goal ? waterMl / goal : 0} color={colors.info} />
          <Text variant="footnote" tone="secondary">
            {waterMl >= goal ? 'Goal reached 💧' : `${formatMl(Math.max(0, goal - waterMl))} to go`}
          </Text>
        </View>
        <IconButton name="add-circle" color={colors.info} size={30} onPress={() => logWater(settings.glass_size_ml)} />
      </Card>

      {/* Upcoming reminders */}
      {upcoming.length > 0 ? (
        <>
          <SectionHeader title="Upcoming reminders" onPress={() => router.push('/reminders')} />
          <Card style={{ gap: 12 }}>
            {upcoming.map(({ r }) => (
              <View key={r.id} style={styles.reminderRow}>
                <View style={[styles.remDot, { backgroundColor: colors.accent }]} />
                <View style={{ flex: 1 }}>
                  <Text variant="subhead" numberOfLines={1}>
                    {r.title}
                  </Text>
                  <Text variant="caption" tone="muted">
                    {reminderSummary(r)}
                  </Text>
                </View>
              </View>
            ))}
          </Card>
        </>
      ) : null}

      <View style={{ height: 8 }} />
    </Screen>
  );

  function Header() {
    return (
      <View style={styles.header}>
        <View style={{ flex: 1, gap: 2 }}>
          <Text variant="caption" tone="muted">
            {prettyDate(today).toUpperCase()}
          </Text>
          <Text variant="title">{greeting()}</Text>
        </View>
        <IconButton name="settings-outline" onPress={() => router.push('/settings')} />
      </View>
    );
  }

  function SectionHeader({ title, right, onPress }: { title: string; right?: string; onPress?: () => void }) {
    return (
      <Pressable style={styles.sectionHeader} onPress={onPress} disabled={!onPress}>
        <Text variant="title2">{title}</Text>
        <View style={styles.sectionRight}>
          {right ? (
            <Text variant="subhead" tone="secondary">
              {right}
            </Text>
          ) : null}
          {onPress ? <Icon name="chevron-forward" size={16} color={colors.textMuted} /> : null}
        </View>
      </Pressable>
    );
  }

  function MiniStat({ icon, label, color }: { icon: IconName; label: string; color: string }) {
    return (
      <View style={styles.miniStat}>
        <Icon name={icon} size={14} color={color} />
        <Text variant="footnote" tone="secondary">
          {label}
        </Text>
      </View>
    );
  }
}

function QuickAdd({
  icon,
  label,
  color,
  onPress,
}: {
  icon: IconName;
  label: string;
  color: string;
  onPress: () => void;
}) {
  const { colors, radius } = useTheme();
  return (
    <Pressable onPress={onPress} style={({ pressed }) => [styles.quick, { opacity: pressed ? 0.8 : 1 }]}>
      <View style={[styles.quickIcon, { backgroundColor: color + '22', borderRadius: radius.lg }]}>
        <Icon name={icon} size={24} color={color} />
      </View>
      <Text variant="caption" color={colors.textSecondary}>
        {label}
      </Text>
    </Pressable>
  );
}

const styles = StyleSheet.create({
  header: { flexDirection: 'row', alignItems: 'center', paddingTop: 8 },
  scoreCard: { flexDirection: 'row', alignItems: 'center', gap: 16 },
  scoreMeta: { flexDirection: 'row', gap: 14, marginTop: 2 },
  miniStat: { flexDirection: 'row', alignItems: 'center', gap: 4 },
  quickRow: { flexDirection: 'row', justifyContent: 'space-between' },
  quick: { alignItems: 'center', gap: 6, flex: 1 },
  quickIcon: { width: 56, height: 56, alignItems: 'center', justifyContent: 'center' },
  sectionHeader: { flexDirection: 'row', alignItems: 'center', justifyContent: 'space-between', marginTop: 4 },
  sectionRight: { flexDirection: 'row', alignItems: 'center', gap: 4 },
  habitRow: { flexDirection: 'row', alignItems: 'center', gap: 12 },
  streakChip: { flexDirection: 'row', alignItems: 'center', gap: 3 },
  waterCard: { flexDirection: 'row', alignItems: 'center', gap: 14 },
  reminderRow: { flexDirection: 'row', alignItems: 'center', gap: 12 },
  remDot: { width: 8, height: 8, borderRadius: 4 },
});
