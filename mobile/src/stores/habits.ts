import { createCollection } from '@/sync';
import type { Category, Frequency, Habit, HabitKind, HabitLog } from '@/types/db';

export const useHabits = createCollection<Habit>('habits');
export const useHabitLogs = createCollection<HabitLog>('habit_logs');

/** Active (non-archived) habits for the current user, in display order. */
export function activeHabits(): Habit[] {
  return useHabits
    .getState()
    .list()
    .filter((h) => !h.is_archived)
    .sort((a, b) => a.sort_order - b.sort_order || a.created_at.localeCompare(b.created_at));
}

/** Map of date → count for a single habit (used by streak math). */
export function logCountsFor(habitId: string): Record<string, number> {
  const out: Record<string, number> = {};
  for (const log of useHabitLogs.getState().list()) {
    if (log.habit_id === habitId) out[log.log_date] = log.count;
  }
  return out;
}

export function habitCountOn(habitId: string, key: string): number {
  const log = useHabitLogs
    .getState()
    .list()
    .find((l) => l.habit_id === habitId && l.log_date === key);
  return log?.count ?? 0;
}

/** Write the count for a habit on a day, reviving any tombstoned row (unique key). */
export function setHabitCount(habitId: string, key: string, count: number): void {
  const any = Object.values(useHabitLogs.getState().items).find(
    (l) => l.habit_id === habitId && l.log_date === key,
  );
  if (count <= 0) {
    if (any && !any.deleted_at) useHabitLogs.getState().remove(any.id);
    return;
  }
  if (any) useHabitLogs.getState().update(any.id, { count, deleted_at: null });
  else useHabitLogs.getState().create({ habit_id: habitId, log_date: key, count });
}

/** Mark a habit fully done / not-done for a day (target_count threshold). */
export function toggleHabitDone(habit: Habit, key: string): void {
  const target = Math.max(1, habit.target_count || 1);
  const done = habitCountOn(habit.id, key) >= target;
  setHabitCount(habit.id, key, done ? 0 : target);
}

/** Adjust a multi-count habit's progress for a day by delta (clamped at 0). */
export function adjustHabitCount(habit: Habit, key: string, delta: number): void {
  const next = Math.max(0, habitCountOn(habit.id, key) + delta);
  setHabitCount(habit.id, key, next);
}

export interface HabitInput {
  name: string;
  kind?: HabitKind;
  category?: Category;
  frequency?: Frequency;
  target_count?: number;
  custom_days?: number[];
  reminder_time?: string | null;
  start_date?: string;
  notes?: string | null;
  color?: string | null;
  icon?: string | null;
}

function normalize(input: HabitInput) {
  return {
    name: input.name.trim(),
    kind: input.kind ?? 'build',
    category: input.category ?? 'health',
    frequency: input.frequency ?? 'daily',
    target_count: Math.max(1, input.target_count ?? 1),
    custom_days: input.custom_days ?? [],
    reminder_time: input.reminder_time ?? null,
    notes: input.notes?.trim() || null,
    color: input.color ?? null,
    icon: input.icon ?? null,
  };
}

export function addHabit(input: HabitInput): Habit {
  return useHabits.getState().create({
    ...normalize(input),
    start_date: input.start_date ?? new Date().toISOString().slice(0, 10),
    is_archived: false,
    sort_order: useHabits.getState().list().length,
  });
}

export function updateHabit(id: string, input: HabitInput): void {
  const patch = normalize(input);
  useHabits.getState().update(id, input.start_date ? { ...patch, start_date: input.start_date } : patch);
}

export function archiveHabit(id: string): void {
  useHabits.getState().update(id, { is_archived: true });
}

export function deleteHabit(id: string): void {
  useHabits.getState().remove(id);
}
