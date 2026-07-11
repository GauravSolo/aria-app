import dayjs from 'dayjs';

import type { Habit } from '@/types/db';
import { todayKey } from './date';

/**
 * Habit scheduling + streak math (pure, mirrored in Swift). Model:
 * - target_count = completions needed on a scheduled day for it to count as "done"
 *   (stored in habit_logs.count).
 * - Scheduled days: daily = every day; weekly/custom = the weekdays in custom_days
 *   (weekly defaults to the habit's start weekday if none chosen).
 * - "Today" is never counted as a miss until it has passed (grace period), so an
 *   un-done-yet today doesn't break the current streak.
 */

export function isScheduledOn(habit: Habit, key: string): boolean {
  if (key < habit.start_date) return false;
  if (habit.frequency === 'daily') return true;
  const days = habit.custom_days.length ? habit.custom_days : [dayjs(habit.start_date).day()];
  return days.includes(dayjs(key).day());
}

export interface HabitStats {
  current: number;
  longest: number;
  totalCompleted: number;
  missed: number;
  successPct: number; // 0–100, based on past scheduled days
  weekDone: number;
  weekTotal: number;
  monthDone: number;
  monthTotal: number;
  doneToday: boolean;
  scheduledToday: boolean;
  todayCount: number;
}

export function emptyStats(): HabitStats {
  return {
    current: 0,
    longest: 0,
    totalCompleted: 0,
    missed: 0,
    successPct: 0,
    weekDone: 0,
    weekTotal: 0,
    monthDone: 0,
    monthTotal: 0,
    doneToday: false,
    scheduledToday: false,
    todayCount: 0,
  };
}

export function computeHabitStats(
  habit: Habit,
  counts: Record<string, number>,
  today: string = todayKey(),
): HabitStats {
  const target = Math.max(1, habit.target_count || 1);
  const isDone = (key: string) => (counts[key] ?? 0) >= target;
  const stats = emptyStats();
  stats.todayCount = counts[today] ?? 0;

  if (habit.start_date > today) return stats;

  const end = dayjs(today);
  const weekStart = end.startOf('week').format('YYYY-MM-DD');
  const monthStart = end.startOf('month').format('YYYY-MM-DD');

  // Cap history scan to ~2 years for performance.
  let cur = dayjs(habit.start_date);
  const minStart = end.subtract(730, 'day');
  if (cur.isBefore(minStart)) cur = minStart;

  const scheduled: { date: string; done: boolean }[] = [];
  let scheduledPast = 0;
  let completedPast = 0;

  while (cur.isBefore(end) || cur.isSame(end, 'day')) {
    const key = cur.format('YYYY-MM-DD');
    if (isScheduledOn(habit, key)) {
      const done = isDone(key);
      scheduled.push({ date: key, done });
      if (done) stats.totalCompleted++;
      if (key < today) {
        scheduledPast++;
        if (done) completedPast++;
        else stats.missed++;
      } else {
        stats.scheduledToday = true;
        stats.doneToday = done;
      }
      if (key >= weekStart) {
        stats.weekTotal++;
        if (done) stats.weekDone++;
      }
      if (key >= monthStart) {
        stats.monthTotal++;
        if (done) stats.monthDone++;
      }
    }
    cur = cur.add(1, 'day');
  }

  // Longest run over scheduled days.
  let run = 0;
  for (const s of scheduled) {
    if (s.done) {
      run++;
      if (run > stats.longest) stats.longest = run;
    } else {
      run = 0;
    }
  }

  // Current streak — drop a not-yet-done today (pending, not a miss).
  const arr = scheduled.slice();
  if (arr.length && arr[arr.length - 1].date === today && !arr[arr.length - 1].done) arr.pop();
  let c = 0;
  for (let i = arr.length - 1; i >= 0; i--) {
    if (arr[i].done) c++;
    else break;
  }
  stats.current = c;

  stats.successPct =
    scheduledPast > 0 ? Math.round((completedPast / scheduledPast) * 100) : stats.doneToday ? 100 : 0;

  return stats;
}

export type DayStatus = 'completed' | 'missed' | 'pending' | 'off' | 'future';

export interface DayCell {
  date: string;
  status: DayStatus;
  count: number;
}

/** Contribution-style grid: array of weeks (columns), each 7 days (Sun→Sat). */
export function buildCalendar(
  habit: Habit,
  counts: Record<string, number>,
  weeks = 16,
  today: string = todayKey(),
): DayCell[][] {
  const target = Math.max(1, habit.target_count || 1);
  const end = dayjs(today).endOf('week'); // Saturday of current week
  let d = end.subtract(weeks * 7 - 1, 'day'); // a Sunday

  const cellFor = (day: dayjs.Dayjs): DayCell => {
    const key = day.format('YYYY-MM-DD');
    const count = counts[key] ?? 0;
    let status: DayStatus;
    if (key > today) status = 'future';
    else if (!isScheduledOn(habit, key)) status = 'off';
    else if (count >= target) status = 'completed';
    else if (key === today) status = 'pending';
    else status = 'missed';
    return { date: key, status, count };
  };

  const result: DayCell[][] = [];
  for (let w = 0; w < weeks; w++) {
    const col: DayCell[] = [];
    for (let i = 0; i < 7; i++) {
      col.push(cellFor(d));
      d = d.add(1, 'day');
    }
    result.push(col);
  }
  return result;
}

export function frequencyLabel(habit: Habit): string {
  if (habit.frequency === 'daily') return 'Every day';
  const days = habit.custom_days.length ? habit.custom_days : [dayjs(habit.start_date).day()];
  const names = ['Sun', 'Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat'];
  if (days.length === 7) return 'Every day';
  return days
    .slice()
    .sort((a, b) => a - b)
    .map((d) => names[d])
    .join(', ');
}
