import AsyncStorage from '@react-native-async-storage/async-storage';
import dayjs from 'dayjs';

import { todayKey } from '@/lib/date';
import { taskOccursOn } from '@/lib/recurrence';
import { computeHabitStats } from '@/lib/streaks';
import { totalForDate } from '@/lib/water';
import type { Habit, HabitLog, Task, TaskCompletion, WaterLog, WaterSettings } from '@/types/db';

/**
 * Snapshot read directly from AsyncStorage (the same data the stores persist), so the
 * widget's headless background task can render without booting the full app/auth graph.
 * Single-user device: we don't filter by user_id here (any lingering guest rows are few).
 */

const DEFAULT_GOAL_ML = 4000;

async function readCollection<T>(table: string): Promise<T[]> {
  try {
    const raw = await AsyncStorage.getItem(`aria.col.${table}`);
    if (!raw) return [];
    const parsed = JSON.parse(raw) as { items?: Record<string, T> };
    return Object.values(parsed.items ?? {});
  } catch {
    return [];
  }
}

export interface WidgetSnapshot {
  generatedAt: string;
  nextTaskTitle: string | null;
  nextTaskTime: string | null;
  pendingTasks: number;
  totalTasks: number;
  waterMl: number;
  waterGoal: number;
  waterPct: number;
  habitsDone: number;
  habitsTotal: number;
  topStreak: number;
}

export function emptySnapshot(): WidgetSnapshot {
  return {
    generatedAt: new Date().toISOString(),
    nextTaskTitle: null,
    nextTaskTime: null,
    pendingTasks: 0,
    totalTasks: 0,
    waterMl: 0,
    waterGoal: DEFAULT_GOAL_ML,
    waterPct: 0,
    habitsDone: 0,
    habitsTotal: 0,
    topStreak: 0,
  };
}

export async function loadWidgetSnapshot(): Promise<WidgetSnapshot> {
  const today = todayKey();
  const [tasks, completions, habits, habitLogs, waterLogs] = await Promise.all([
    readCollection<Task>('tasks'),
    readCollection<TaskCompletion>('task_completions'),
    readCollection<Habit>('habits'),
    readCollection<HabitLog>('habit_logs'),
    readCollection<WaterLog>('water_logs'),
  ]);

  let goal = DEFAULT_GOAL_ML;
  try {
    const raw = await AsyncStorage.getItem('aria.water_settings');
    if (raw) {
      const map = JSON.parse(raw) as Record<string, WaterSettings>;
      const first = Object.values(map)[0];
      if (first?.daily_goal_ml) goal = first.daily_goal_ml;
    }
  } catch {
    // keep default
  }

  const live = <T extends { deleted_at: string | null }>(arr: T[]) => arr.filter((r) => !r.deleted_at);
  const t = live(tasks);
  const c = live(completions);
  const h = live(habits).filter((x) => !x.is_archived);
  const hl = live(habitLogs);
  const wl = live(waterLogs);

  const completionSet = new Set(c.map((x) => `${x.task_id}|${x.occurrence_date}`));
  const isDone = (task: Task) =>
    task.recurrence === 'none' ? task.is_completed : completionSet.has(`${task.id}|${today}`);
  const dayTasks = t
    .filter((x) => taskOccursOn(x, today))
    .sort((a, b) => (a.start_time ?? '').localeCompare(b.start_time ?? ''));
  const pending = dayTasks.filter((x) => !isDone(x));
  const next = pending[0] ?? null;

  const counts: Record<string, Record<string, number>> = {};
  for (const l of hl) (counts[l.habit_id] ??= {})[l.log_date] = l.count;
  let habitsDone = 0;
  let habitsTotal = 0;
  let topStreak = 0;
  for (const hb of h) {
    const st = computeHabitStats(hb, counts[hb.id] ?? {}, today);
    if (st.scheduledToday) {
      habitsTotal++;
      if (st.doneToday) habitsDone++;
    }
    topStreak = Math.max(topStreak, st.current);
  }

  const waterMl = totalForDate(wl, today);

  return {
    generatedAt: new Date().toISOString(),
    nextTaskTitle: next?.title ?? null,
    nextTaskTime: next?.start_time ? dayjs(next.start_time).format('h:mm A') : null,
    pendingTasks: pending.length,
    totalTasks: dayTasks.length,
    waterMl,
    waterGoal: goal,
    waterPct: goal ? Math.round((waterMl / goal) * 100) : 0,
    habitsDone,
    habitsTotal,
    topStreak,
  };
}
