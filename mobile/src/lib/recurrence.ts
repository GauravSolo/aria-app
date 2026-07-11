import dayjs from 'dayjs';

import type { Task } from '@/types/db';
import { dateKey } from './date';

/** The first day a task is relevant: its due date, else the day it was created. */
export function taskAnchorDate(task: Task): string {
  return task.due_date ?? dateKey(task.created_at);
}

/**
 * Whether a task should appear on a given local day (YYYY-MM-DD), expanding its
 * recurrence rule. Date keys compare correctly as strings (ISO order).
 */
export function taskOccursOn(task: Task, key: string): boolean {
  if (task.deleted_at) return false;
  const anchor = taskAnchorDate(task);

  if (task.recurrence === 'none') return key === anchor;
  if (key < anchor) return false;
  if (task.recurrence_end_date && key > task.recurrence_end_date) return false;

  const a = dayjs(anchor);
  const d = dayjs(key);

  switch (task.recurrence) {
    case 'daily':
      return true;
    case 'weekly': {
      const days = task.recurrence_days.length ? task.recurrence_days : [a.day()];
      return days.includes(d.day());
    }
    case 'monthly':
      return a.date() === d.date();
    case 'custom': {
      const interval = Math.max(1, task.recurrence_interval || 1);
      return d.diff(a, 'day') % interval === 0;
    }
    default:
      return false;
  }
}

const RECURRENCE_LABEL: Record<Task['recurrence'], string> = {
  none: 'One-time',
  daily: 'Every day',
  weekly: 'Weekly',
  monthly: 'Monthly',
  custom: 'Custom',
};

export function recurrenceLabel(task: Task): string {
  if (task.recurrence === 'custom') {
    const n = Math.max(1, task.recurrence_interval || 1);
    return n === 1 ? 'Every day' : `Every ${n} days`;
  }
  return RECURRENCE_LABEL[task.recurrence];
}
