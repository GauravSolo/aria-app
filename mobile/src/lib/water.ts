import dayjs from 'dayjs';

import type { WaterLog } from '@/types/db';
import { todayKey } from './date';

/** Sum ml per day → { 'YYYY-MM-DD': ml }. Ignores soft-deleted rows. */
export function sumByDate(logs: WaterLog[]): Record<string, number> {
  const out: Record<string, number> = {};
  for (const l of logs) {
    if (l.deleted_at) continue;
    out[l.log_date] = (out[l.log_date] ?? 0) + l.amount_ml;
  }
  return out;
}

export function totalForDate(logs: WaterLog[], key: string): number {
  return logs.reduce((sum, l) => (!l.deleted_at && l.log_date === key ? sum + l.amount_ml : sum), 0);
}

export interface DaySeriesPoint {
  date: string;
  label: string;
  ml: number;
}

/** ml per day for the current week (Sun→Sat). */
export function weekSeries(logs: WaterLog[], today: string = todayKey()): DaySeriesPoint[] {
  const byDate = sumByDate(logs);
  const start = dayjs(today).startOf('week');
  return Array.from({ length: 7 }, (_, i) => {
    const d = start.add(i, 'day');
    const key = d.format('YYYY-MM-DD');
    return { date: key, label: d.format('dd'), ml: byDate[key] ?? 0 };
  });
}

export interface RangeStats {
  total: number;
  average: number; // per day with any activity counted across days in range
  daysMetGoal: number;
  daysTracked: number;
}

/** Stats for the current calendar month up to today. */
export function monthStats(
  logs: WaterLog[],
  goalMl: number,
  today: string = todayKey(),
): RangeStats {
  const byDate = sumByDate(logs);
  const start = dayjs(today).startOf('month');
  const end = dayjs(today);
  let total = 0;
  let daysMetGoal = 0;
  let daysTracked = 0;
  const numDays = end.diff(start, 'day') + 1;
  for (let i = 0; i < numDays; i++) {
    const key = start.add(i, 'day').format('YYYY-MM-DD');
    const ml = byDate[key] ?? 0;
    total += ml;
    if (ml > 0) daysTracked++;
    if (goalMl > 0 && ml >= goalMl) daysMetGoal++;
  }
  return {
    total,
    average: numDays > 0 ? Math.round(total / numDays) : 0,
    daysMetGoal,
    daysTracked,
  };
}

export function formatMl(ml: number): string {
  if (ml >= 1000) return `${(ml / 1000).toFixed(ml % 1000 === 0 ? 0 : 1)} L`;
  return `${ml} ml`;
}
