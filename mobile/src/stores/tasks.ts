import { nowIso } from '@/lib/id';
import { taskOccursOn } from '@/lib/recurrence';
import { createCollection } from '@/sync';
import type { Category, Priority, Task, TaskCompletion, TaskRecurrence } from '@/types/db';

export const useTasks = createCollection<Task>('tasks');
export const useTaskCompletions = createCollection<TaskCompletion>('task_completions');

const PRIORITY_RANK: Record<Priority, number> = { high: 0, medium: 1, low: 2 };

/** Timed tasks first (by start), then by priority, then title. */
export function sortTasks(a: Task, b: Task): number {
  const at = a.start_time ?? '';
  const bt = b.start_time ?? '';
  if (at && bt && at !== bt) return at.localeCompare(bt);
  if (at && !bt) return -1;
  if (!at && bt) return 1;
  if (PRIORITY_RANK[a.priority] !== PRIORITY_RANK[b.priority]) {
    return PRIORITY_RANK[a.priority] - PRIORITY_RANK[b.priority];
  }
  return a.title.localeCompare(b.title);
}

/** All task instances occurring on a given day, sorted for display. */
export function tasksForDate(key: string): Task[] {
  return useTasks
    .getState()
    .list()
    .filter((t) => taskOccursOn(t, key))
    .sort(sortTasks);
}

export function isTaskDone(task: Task, key: string): boolean {
  if (task.recurrence === 'none') return task.is_completed;
  return useTaskCompletions
    .getState()
    .list()
    .some((c) => c.task_id === task.id && c.occurrence_date === key);
}

export function toggleTaskDone(task: Task, key: string): void {
  if (task.recurrence === 'none') {
    const done = task.is_completed;
    useTasks.getState().update(task.id, {
      is_completed: !done,
      completed_at: done ? null : nowIso(),
    });
    return;
  }
  // Search ALL rows (incl. tombstones) to respect the unique (task_id, occurrence_date).
  const any = Object.values(useTaskCompletions.getState().items).find(
    (c) => c.task_id === task.id && c.occurrence_date === key,
  );
  if (any && !any.deleted_at) {
    useTaskCompletions.getState().remove(any.id);
  } else if (any) {
    useTaskCompletions.getState().update(any.id, { deleted_at: null, completed_at: nowIso() });
  } else {
    useTaskCompletions.getState().create({
      task_id: task.id,
      occurrence_date: key,
      completed_at: nowIso(),
    });
  }
}

export interface TaskInput {
  title: string;
  description?: string | null;
  category?: Category;
  priority?: Priority;
  start_time?: string | null;
  end_time?: string | null;
  due_date?: string | null;
  recurrence?: TaskRecurrence;
  recurrence_interval?: number;
  recurrence_days?: number[];
  recurrence_end_date?: string | null;
}

function normalize(input: TaskInput) {
  return {
    title: input.title.trim(),
    description: input.description?.trim() || null,
    category: input.category ?? 'other',
    priority: input.priority ?? 'medium',
    start_time: input.start_time ?? null,
    end_time: input.end_time ?? null,
    due_date: input.due_date ?? null,
    recurrence: input.recurrence ?? 'none',
    recurrence_interval: input.recurrence_interval ?? 1,
    recurrence_days: input.recurrence_days ?? [],
    recurrence_end_date: input.recurrence_end_date ?? null,
  };
}

export function addTask(input: TaskInput): Task {
  return useTasks.getState().create({
    ...normalize(input),
    is_completed: false,
    completed_at: null,
    sort_order: 0,
  });
}

export function updateTask(id: string, input: TaskInput): void {
  useTasks.getState().update(id, normalize(input));
}

export function deleteTask(id: string): void {
  useTasks.getState().remove(id);
}
