import dayjs from 'dayjs';

import { nowIso } from '@/lib/id';
import { createCollection } from '@/sync';
import type {
  NotificationHistory,
  NotifStatus,
  Reminder,
  ReminderKind,
  ReminderRepeat,
} from '@/types/db';

export const useReminders = createCollection<Reminder>('reminders');
export const useNotificationHistory = createCollection<NotificationHistory>('notification_history');

export function activeReminders(): Reminder[] {
  return useReminders
    .getState()
    .list()
    .sort((a, b) => a.created_at.localeCompare(b.created_at));
}

export interface ReminderInput {
  title: string;
  body?: string | null;
  kind?: ReminderKind;
  ref_id?: string | null;
  repeat?: ReminderRepeat;
  repeat_days?: number[];
  interval_min?: number | null;
  time_of_day?: string | null; // HH:mm
  next_trigger_at?: string | null;
}

function normalize(input: ReminderInput) {
  return {
    title: input.title.trim(),
    body: input.body?.trim() || null,
    kind: input.kind ?? 'custom',
    ref_id: input.ref_id ?? null,
    repeat: input.repeat ?? 'once',
    repeat_days: input.repeat_days ?? [],
    interval_min: input.interval_min ?? null,
    time_of_day: input.time_of_day ?? null,
    next_trigger_at: input.next_trigger_at ?? null,
  };
}

export function addReminder(input: ReminderInput): Reminder {
  return useReminders.getState().create({
    ...normalize(input),
    is_enabled: true,
    snooze_until: null,
    local_notification_id: null,
  });
}

export function updateReminder(id: string, input: ReminderInput): void {
  useReminders.getState().update(id, { ...normalize(input), snooze_until: null });
}

export function deleteReminder(id: string): void {
  useReminders.getState().remove(id);
}

export function toggleReminder(id: string): void {
  const r = useReminders.getState().getById(id);
  if (r) useReminders.getState().update(id, { is_enabled: !r.is_enabled });
}

export function snoozeReminder(id: string, minutes = 10): void {
  useReminders.getState().update(id, {
    snooze_until: dayjs().add(minutes, 'minute').toISOString(),
    is_enabled: true,
  });
  recordNotification({ reminderId: id, title: titleOf(id), kind: 'custom', status: 'snoozed' });
}

export function markReminderDone(id: string): void {
  const r = useReminders.getState().getById(id);
  if (!r) return;
  recordNotification({ reminderId: id, title: r.title, kind: r.kind, status: 'done' });
  // One-time reminders are finished once done.
  if (r.repeat === 'once') useReminders.getState().update(id, { is_enabled: false });
}

function titleOf(id: string): string {
  return useReminders.getState().getById(id)?.title ?? 'Reminder';
}

export function recordNotification(args: {
  reminderId?: string | null;
  title: string;
  body?: string | null;
  kind: ReminderKind;
  status?: NotifStatus;
}): void {
  useNotificationHistory.getState().create({
    reminder_id: args.reminderId ?? null,
    title: args.title,
    body: args.body ?? null,
    kind: args.kind,
    fired_at: nowIso(),
    status: args.status ?? 'delivered',
  });
}

/** History entries, newest first. */
export function notificationHistory(): NotificationHistory[] {
  return useNotificationHistory
    .getState()
    .list()
    .sort((a, b) => b.fired_at.localeCompare(a.fired_at));
}
