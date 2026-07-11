export function clamp01(x: number): number {
  return Math.max(0, Math.min(1, x));
}

/**
 * Productivity score (0–100): equal-weighted average of whichever of task / habit /
 * water completion rates apply that day. Water is capped at 100% of goal.
 */
export function productivityScore(parts: {
  taskRate?: number;
  habitRate?: number;
  waterRate?: number;
}): number {
  const vals: number[] = [];
  if (parts.taskRate != null) vals.push(clamp01(parts.taskRate));
  if (parts.habitRate != null) vals.push(clamp01(parts.habitRate));
  if (parts.waterRate != null) vals.push(clamp01(parts.waterRate));
  if (vals.length === 0) return 0;
  return Math.round((vals.reduce((a, b) => a + b, 0) / vals.length) * 100);
}

export function pct(done: number, total: number): number {
  return total > 0 ? Math.round((done / total) * 100) : 0;
}

// ── Range analytics ──────────────────────────────────────────────────────────
import dayjs from 'dayjs';

import { todayKey } from './date';
import { taskOccursOn } from './recurrence';
import { computeHabitStats, isScheduledOn } from './streaks';
import { sumByDate } from './water';
import type { Habit, HabitLog, Task, TaskCompletion, WaterLog } from '@/types/db';

export interface AnalyticsInput {
  tasks: Task[];
  completions: TaskCompletion[];
  habits: Habit[];
  habitLogs: HabitLog[];
  waterLogs: WaterLog[];
  goalMl: number;
  days: number; // e.g. 7 or 30
  today?: string;
}

export interface DayPoint {
  date: string;
  label: string;
  score: number;
  ml: number;
}

export interface HabitBreakdown {
  id: string;
  name: string;
  color: string | null;
  rate: number; // 0–100 over range
  current: number;
}

export interface AnalyticsResult {
  days: number;
  goalMl: number;
  taskTotal: number;
  taskDone: number;
  taskMissed: number;
  taskRate: number;
  habitTotal: number;
  habitDone: number;
  habitRate: number;
  topCurrentStreak: number;
  topLongestStreak: number;
  waterDaysMet: number;
  waterConsistency: number;
  perDay: DayPoint[];
  bestDay: DayPoint | null;
  missedTasks: { id: string; title: string; date: string }[];
  habitBreakdown: HabitBreakdown[];
}

export function computeAnalytics(input: AnalyticsInput): AnalyticsResult {
  const today = input.today ?? todayKey();
  const start = dayjs(today).subtract(input.days - 1, 'day');

  const completionSet = new Set(
    input.completions.filter((c) => !c.deleted_at).map((c) => `${c.task_id}|${c.occurrence_date}`),
  );
  const taskDoneOn = (t: Task, key: string) =>
    t.recurrence === 'none' ? t.is_completed : completionSet.has(`${t.id}|${key}`);

  const habitCounts: Record<string, Record<string, number>> = {};
  for (const l of input.habitLogs) {
    if (l.deleted_at) continue;
    (habitCounts[l.habit_id] ??= {})[l.log_date] = l.count;
  }

  const waterByDate = sumByDate(input.waterLogs);

  let taskTotal = 0;
  let taskDone = 0;
  let taskMissed = 0;
  let habitTotal = 0;
  let habitDone = 0;
  let waterDaysMet = 0;
  const missedTasks: { id: string; title: string; date: string }[] = [];
  const habitAcc: Record<string, { total: number; done: number }> = {};
  const perDay: DayPoint[] = [];

  for (let i = 0; i < input.days; i++) {
    const d = start.add(i, 'day');
    const key = d.format('YYYY-MM-DD');
    const isPast = key < today;

    let dayTaskTotal = 0;
    let dayTaskDone = 0;
    for (const t of input.tasks) {
      if (!taskOccursOn(t, key)) continue;
      taskTotal++;
      dayTaskTotal++;
      if (taskDoneOn(t, key)) {
        taskDone++;
        dayTaskDone++;
      } else if (isPast) {
        taskMissed++;
        missedTasks.push({ id: t.id, title: t.title, date: key });
      }
    }

    let dayHabitTotal = 0;
    let dayHabitDone = 0;
    for (const h of input.habits) {
      if (!isScheduledOn(h, key)) continue;
      const target = Math.max(1, h.target_count || 1);
      const done = (habitCounts[h.id]?.[key] ?? 0) >= target;
      habitTotal++;
      dayHabitTotal++;
      const acc = (habitAcc[h.id] ??= { total: 0, done: 0 });
      acc.total++;
      if (done) {
        habitDone++;
        dayHabitDone++;
        acc.done++;
      }
    }

    const ml = waterByDate[key] ?? 0;
    if (input.goalMl > 0 && ml >= input.goalMl) waterDaysMet++;

    const score = productivityScore({
      taskRate: dayTaskTotal ? dayTaskDone / dayTaskTotal : undefined,
      habitRate: dayHabitTotal ? dayHabitDone / dayHabitTotal : undefined,
      waterRate: input.goalMl ? ml / input.goalMl : undefined,
    });
    perDay.push({ date: key, label: d.format(input.days <= 7 ? 'dd' : 'D'), score, ml });
  }

  let bestDay: DayPoint | null = null;
  for (const p of perDay) if (p.score > 0 && (!bestDay || p.score > bestDay.score)) bestDay = p;

  let topCurrent = 0;
  let topLongest = 0;
  const habitBreakdown: HabitBreakdown[] = input.habits.map((h) => {
    const stats = computeHabitStats(h, habitCounts[h.id] ?? {}, today);
    topCurrent = Math.max(topCurrent, stats.current);
    topLongest = Math.max(topLongest, stats.longest);
    const acc = habitAcc[h.id] ?? { total: 0, done: 0 };
    return { id: h.id, name: h.name, color: h.color, rate: pct(acc.done, acc.total), current: stats.current };
  });

  return {
    days: input.days,
    goalMl: input.goalMl,
    taskTotal,
    taskDone,
    taskMissed,
    taskRate: pct(taskDone, taskTotal),
    habitTotal,
    habitDone,
    habitRate: pct(habitDone, habitTotal),
    topCurrentStreak: topCurrent,
    topLongestStreak: topLongest,
    waterDaysMet,
    waterConsistency: pct(waterDaysMet, input.days),
    perDay,
    bestDay,
    missedTasks: missedTasks.slice(-12).reverse(),
    habitBreakdown,
  };
}
