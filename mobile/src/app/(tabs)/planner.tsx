import dayjs from 'dayjs';
import { router } from 'expo-router';
import { useMemo, useState } from 'react';
import { StyleSheet, View } from 'react-native';

import { TaskCard } from '@/components/tasks/TaskCard';
import {
  Card,
  DateStrip,
  EmptyState,
  Fab,
  Header,
  ProgressBar,
  Screen,
  Segmented,
  Text,
} from '@/components/ui';
import { prettyDate, todayKey } from '@/lib/date';
import { taskOccursOn } from '@/lib/recurrence';
import { useAuth } from '@/stores/auth';
import { isTaskDone, sortTasks, toggleTaskDone, useTaskCompletions, useTasks } from '@/stores/tasks';
import { useTheme } from '@/theme';
import type { Task } from '@/types/db';

type ViewMode = 'list' | 'timeline';

export default function PlannerScreen() {
  const { colors, spacing } = useTheme();
  const uid = useAuth((s) => s.user?.id ?? s.guestId ?? '');
  const taskItems = useTasks((s) => s.items);
  // subscribe to completions so checkboxes re-render on toggle
  useTaskCompletions((s) => s.items);

  const [selected, setSelected] = useState(todayKey());
  const [mode, setMode] = useState<ViewMode>('list');

  const dayTasks = useMemo(
    () =>
      Object.values(taskItems)
        .filter((t) => !t.deleted_at && t.user_id === uid && taskOccursOn(t, selected))
        .sort(sortTasks),
    [taskItems, uid, selected],
  );

  const marked = useMemo(() => {
    const set = new Set<string>();
    const all = Object.values(taskItems).filter((t) => !t.deleted_at && t.user_id === uid);
    for (let i = -7; i <= 30; i++) {
      const key = dayjs().add(i, 'day').format('YYYY-MM-DD');
      if (all.some((t) => taskOccursOn(t, key))) set.add(key);
    }
    return set;
  }, [taskItems, uid]);

  const completedCount = dayTasks.filter((t) => isTaskDone(t, selected)).length;
  const total = dayTasks.length;

  const openNew = () => router.push({ pathname: '/task-form', params: { date: selected } });
  const openEdit = (task: Task) =>
    router.push({ pathname: '/task-form', params: { id: task.id, date: selected } });

  const ordered = useMemo(() => {
    const withDone = dayTasks.map((t) => ({ t, done: isTaskDone(t, selected) }));
    return withDone.sort((a, b) => Number(a.done) - Number(b.done));
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [dayTasks, selected, taskItems]);

  return (
    <Screen padded={false}>
      <View style={{ paddingHorizontal: spacing.lg }}>
        <Header title="Planner" subtitle={prettyDate(selected)} />
      </View>

      <View style={{ paddingLeft: spacing.lg }}>
        <DateStrip selected={selected} onSelect={setSelected} marked={marked} />
      </View>

      <View style={{ paddingHorizontal: spacing.lg, paddingTop: spacing.md, gap: spacing.md }}>
        {total > 0 ? (
          <Card style={styles.summary}>
            <View style={{ flex: 1, gap: 8 }}>
              <View style={styles.summaryTop}>
                <Text variant="bodyStrong">
                  {completedCount} of {total} done
                </Text>
                <Text variant="subhead" tone="secondary">
                  {Math.round((completedCount / total) * 100)}%
                </Text>
              </View>
              <ProgressBar progress={total ? completedCount / total : 0} />
            </View>
          </Card>
        ) : null}

        <Segmented
          options={[
            { label: 'List', value: 'list', icon: 'list-outline' },
            { label: 'Timeline', value: 'timeline', icon: 'time-outline' },
          ]}
          value={mode}
          onChange={(v) => setMode(v as ViewMode)}
        />
      </View>

      {total === 0 ? (
        <EmptyState
          icon="checkmark-done-outline"
          title="Nothing planned"
          message="Tap the + button to add your first task for this day."
        />
      ) : (
        <Screen scroll padded contentStyle={{ paddingTop: spacing.md }}>
          {mode === 'list'
            ? ordered.map(({ t, done }) => (
                <TaskCard
                  key={t.id}
                  task={t}
                  done={done}
                  onToggle={() => toggleTaskDone(t, selected)}
                  onPress={() => openEdit(t)}
                />
              ))
            : renderTimeline(dayTasks, selected, openEdit, colors.border)}
        </Screen>
      )}

      <Fab onPress={openNew} />
    </Screen>
  );
}

function renderTimeline(
  tasks: Task[],
  selected: string,
  openEdit: (t: Task) => void,
  railColor: string,
) {
  const untimed = tasks.filter((t) => !t.start_time);
  const timed = tasks.filter((t) => t.start_time);

  return (
    <View style={{ gap: 12 }}>
      {untimed.length > 0 ? (
        <View style={styles.timelineGroup}>
          <Text variant="caption" tone="muted" style={styles.railLabel}>
            ANYTIME
          </Text>
          <View style={{ flex: 1, gap: 10 }}>
            {untimed.map((t) => (
              <TaskCard
                key={t.id}
                task={t}
                done={isTaskDone(t, selected)}
                onToggle={() => toggleTaskDone(t, selected)}
                onPress={() => openEdit(t)}
              />
            ))}
          </View>
        </View>
      ) : null}

      {timed.map((t) => (
        <View key={t.id} style={styles.timelineGroup}>
          <View style={styles.rail}>
            <Text variant="caption" tone="secondary">
              {dayjs(t.start_time).format('h:mm')}
            </Text>
            <Text variant="caption" tone="muted">
              {dayjs(t.start_time).format('A')}
            </Text>
            <View style={[styles.railLine, { backgroundColor: railColor }]} />
          </View>
          <View style={{ flex: 1 }}>
            <TaskCard
              task={t}
              done={isTaskDone(t, selected)}
              onToggle={() => toggleTaskDone(t, selected)}
              onPress={() => openEdit(t)}
            />
          </View>
        </View>
      ))}
    </View>
  );
}

const styles = StyleSheet.create({
  summary: { flexDirection: 'row', alignItems: 'center' },
  summaryTop: { flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center' },
  timelineGroup: { flexDirection: 'row', gap: 10 },
  rail: { width: 48, alignItems: 'center', paddingTop: 6 },
  railLabel: { width: 48, paddingTop: 8, textAlign: 'center' },
  railLine: { width: 2, flex: 1, marginTop: 6, borderRadius: 1 },
});
