/**
 * TypeScript mirror of docs/DATA_MODEL.md. Keep in sync with backend/schema.sql
 * and the Swift models. Enums and field names must match exactly.
 */

// ── Enums ────────────────────────────────────────────────────────────────────
export const CATEGORIES = ['work', 'study', 'health', 'personal', 'other'] as const;
export type Category = (typeof CATEGORIES)[number];

export const PRIORITIES = ['low', 'medium', 'high'] as const;
export type Priority = (typeof PRIORITIES)[number];

export const TASK_RECURRENCES = ['none', 'daily', 'weekly', 'monthly', 'custom'] as const;
export type TaskRecurrence = (typeof TASK_RECURRENCES)[number];

export const HABIT_KINDS = ['build', 'quit'] as const;
export type HabitKind = (typeof HABIT_KINDS)[number];

export const FREQUENCIES = ['daily', 'weekly', 'custom'] as const;
export type Frequency = (typeof FREQUENCIES)[number];

export const REMINDER_KINDS = ['task', 'habit', 'water', 'custom'] as const;
export type ReminderKind = (typeof REMINDER_KINDS)[number];

export const REMINDER_REPEATS = ['once', 'daily', 'weekly', 'interval'] as const;
export type ReminderRepeat = (typeof REMINDER_REPEATS)[number];

export const NOTIF_STATUSES = ['delivered', 'done', 'snoozed', 'dismissed'] as const;
export type NotifStatus = (typeof NOTIF_STATUSES)[number];

/** 0 = Sunday … 6 = Saturday */
export type Weekday = 0 | 1 | 2 | 3 | 4 | 5 | 6;

// ── Shared sync columns ──────────────────────────────────────────────────────
export interface SyncColumns {
  created_at: string; // ISO
  updated_at: string; // ISO
  deleted_at: string | null; // ISO | null (soft delete)
}

export interface OwnedRow extends SyncColumns {
  id: string;
  user_id: string;
}

// ── Tables ───────────────────────────────────────────────────────────────────
export interface Profile {
  id: string;
  display_name: string | null;
  theme: 'system' | 'light' | 'dark';
  created_at: string;
  updated_at: string;
}

export interface Task extends OwnedRow {
  title: string;
  description: string | null;
  category: Category;
  priority: Priority;
  start_time: string | null; // ISO
  end_time: string | null; // ISO
  due_date: string | null; // YYYY-MM-DD
  recurrence: TaskRecurrence;
  recurrence_interval: number;
  recurrence_days: number[];
  recurrence_end_date: string | null; // YYYY-MM-DD
  is_completed: boolean;
  completed_at: string | null;
  sort_order: number;
}

export interface TaskCompletion extends OwnedRow {
  task_id: string;
  occurrence_date: string; // YYYY-MM-DD
  completed_at: string;
}

export interface Habit extends OwnedRow {
  name: string;
  kind: HabitKind;
  category: Category;
  frequency: Frequency;
  target_count: number;
  custom_days: number[];
  reminder_time: string | null; // HH:mm[:ss]
  start_date: string; // YYYY-MM-DD
  notes: string | null;
  color: string | null;
  icon: string | null;
  is_archived: boolean;
  sort_order: number;
}

export interface HabitLog extends OwnedRow {
  habit_id: string;
  log_date: string; // YYYY-MM-DD
  count: number;
}

export interface WaterSettings {
  user_id: string;
  daily_goal_ml: number;
  glass_size_ml: number;
  reminder_interval_min: number;
  reminder_enabled: boolean;
  active_start: string; // HH:mm[:ss]
  active_end: string; // HH:mm[:ss]
  updated_at: string;
}

export interface WaterLog extends OwnedRow {
  log_date: string; // YYYY-MM-DD
  amount_ml: number;
  logged_at: string; // ISO
}

export interface Reminder extends OwnedRow {
  title: string;
  body: string | null;
  kind: ReminderKind;
  ref_id: string | null;
  repeat: ReminderRepeat;
  repeat_days: number[];
  interval_min: number | null;
  time_of_day: string | null; // HH:mm[:ss]
  next_trigger_at: string | null; // ISO
  is_enabled: boolean;
  snooze_until: string | null; // ISO
  local_notification_id: string | null;
}

export interface NotificationHistory extends OwnedRow {
  reminder_id: string | null;
  title: string;
  body: string | null;
  kind: ReminderKind;
  fired_at: string; // ISO
  status: NotifStatus;
}

// ── Display labels (UI dropdowns) ────────────────────────────────────────────
export const CATEGORY_LABEL: Record<Category, string> = {
  work: 'Work',
  study: 'Study',
  health: 'Health',
  personal: 'Personal',
  other: 'Other',
};

export const PRIORITY_LABEL: Record<Priority, string> = {
  low: 'Low',
  medium: 'Medium',
  high: 'High',
};

export const WEEKDAY_SHORT = ['Sun', 'Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat'] as const;
