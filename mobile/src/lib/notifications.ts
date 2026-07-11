import * as Notifications from 'expo-notifications';
import { Platform } from 'react-native';

import { useHabits } from '@/stores/habits';
import { useReminders } from '@/stores/reminders';
import { useWaterSettings } from '@/stores/water';
import type { Habit, Reminder, WaterSettings } from '@/types/db';

/**
 * Local notification scheduling. Strategy: a single `reconcileSchedules()` cancels all
 * OS notifications and re-creates them from current app state (reminders + habit
 * reminder times + water interval within active hours). Idempotent, offline-capable,
 * matches the "device reconciles from the synced schedule" model in ARCHITECTURE.md.
 *
 * NOTE: cannot be verified in the dev sandbox — needs a real device / dev build.
 */

export function configureNotifications(): void {
  Notifications.setNotificationHandler({
    handleNotification: async () => ({
      shouldShowBanner: true,
      shouldShowList: true,
      shouldPlaySound: true,
      shouldSetBadge: false,
    }),
  });
}

export async function ensureAndroidChannel(): Promise<void> {
  if (Platform.OS !== 'android') return;
  await Notifications.setNotificationChannelAsync('default', {
    name: 'Aria reminders',
    importance: Notifications.AndroidImportance.DEFAULT,
    vibrationPattern: [0, 250, 250, 250],
    lightColor: '#6366F1',
  });
}

export async function ensurePermissions(): Promise<boolean> {
  if (Platform.OS === 'web') return false;
  const current = await Notifications.getPermissionsAsync();
  if (current.granted) return true;
  if (!current.canAskAgain) return false;
  const req = await Notifications.requestPermissionsAsync();
  return req.granted;
}

function parseHM(hm: string): { hour: number; minute: number } {
  const [h, m] = hm.split(':').map((n) => parseInt(n, 10));
  return { hour: h || 0, minute: m || 0 };
}

const jsDayToExpoWeekday = (d: number) => d + 1; // expo: 1=Sun … 7=Sat

type Content = { title: string; body?: string | null; data?: Record<string, unknown> };

async function scheduleDaily(c: Content, hour: number, minute: number) {
  await Notifications.scheduleNotificationAsync({
    content: { title: c.title, body: c.body ?? undefined, data: c.data },
    trigger: { type: Notifications.SchedulableTriggerInputTypes.DAILY, hour, minute },
  });
}

async function scheduleWeekly(c: Content, weekday: number, hour: number, minute: number) {
  await Notifications.scheduleNotificationAsync({
    content: { title: c.title, body: c.body ?? undefined, data: c.data },
    trigger: { type: Notifications.SchedulableTriggerInputTypes.WEEKLY, weekday, hour, minute },
  });
}

async function scheduleAt(c: Content, date: Date) {
  await Notifications.scheduleNotificationAsync({
    content: { title: c.title, body: c.body ?? undefined, data: c.data },
    trigger: { type: Notifications.SchedulableTriggerInputTypes.DATE, date },
  });
}

async function scheduleInterval(c: Content, seconds: number) {
  await Notifications.scheduleNotificationAsync({
    content: { title: c.title, body: c.body ?? undefined, data: c.data },
    trigger: {
      type: Notifications.SchedulableTriggerInputTypes.TIME_INTERVAL,
      seconds,
      repeats: true,
    },
  });
}

async function scheduleReminder(r: Reminder) {
  const content: Content = {
    title: r.title,
    body: r.body,
    data: { reminderId: r.id, kind: r.kind, title: r.title, body: r.body },
  };

  if (r.snooze_until && new Date(r.snooze_until) > new Date()) {
    await scheduleAt(content, new Date(r.snooze_until));
    return;
  }

  switch (r.repeat) {
    case 'once':
      if (r.next_trigger_at && new Date(r.next_trigger_at) > new Date()) {
        await scheduleAt(content, new Date(r.next_trigger_at));
      }
      break;
    case 'daily':
      if (r.time_of_day) {
        const { hour, minute } = parseHM(r.time_of_day);
        await scheduleDaily(content, hour, minute);
      }
      break;
    case 'weekly':
      if (r.time_of_day && r.repeat_days.length) {
        const { hour, minute } = parseHM(r.time_of_day);
        for (const d of r.repeat_days) await scheduleWeekly(content, jsDayToExpoWeekday(d), hour, minute);
      }
      break;
    case 'interval':
      if (r.interval_min) await scheduleInterval(content, r.interval_min * 60);
      break;
  }
}

async function scheduleHabit(h: Habit) {
  if (!h.reminder_time) return;
  const { hour, minute } = parseHM(h.reminder_time);
  const content: Content = {
    title: `Habit: ${h.name}`,
    body: 'Time to keep your streak going 🔥',
    data: { kind: 'habit', refId: h.id, title: h.name },
  };
  if (h.frequency === 'daily') {
    await scheduleDaily(content, hour, minute);
  } else {
    const days = h.custom_days.length ? h.custom_days : [];
    for (const d of days) await scheduleWeekly(content, jsDayToExpoWeekday(d), hour, minute);
  }
}

async function scheduleWater(ws: WaterSettings) {
  const start = parseHM(ws.active_start);
  const end = parseHM(ws.active_end);
  const step = Math.max(15, ws.reminder_interval_min);
  let t = start.hour * 60 + start.minute;
  const endMin = end.hour * 60 + end.minute;
  const content: Content = {
    title: 'Time to hydrate 💧',
    body: 'Have a glass of water to stay on track.',
    data: { kind: 'water', title: 'Water reminder' },
  };
  let count = 0;
  while (t <= endMin && count < 30) {
    await scheduleDaily(content, Math.floor(t / 60), t % 60);
    t += step;
    count++;
  }
}

let reconciling = false;

export async function reconcileSchedules(): Promise<void> {
  if (Platform.OS === 'web') return;
  const perms = await Notifications.getPermissionsAsync();
  if (!perms.granted) return;
  if (reconciling) return;
  reconciling = true;
  try {
    await Notifications.cancelAllScheduledNotificationsAsync();
    for (const r of useReminders.getState().list()) {
      if (r.is_enabled) await scheduleReminder(r);
    }
    for (const h of useHabits.getState().list()) {
      if (!h.is_archived && h.reminder_time) await scheduleHabit(h);
    }
    const ws = useWaterSettings.getState().current();
    if (ws.reminder_enabled) await scheduleWater(ws);
  } finally {
    reconciling = false;
  }
}

let timer: ReturnType<typeof setTimeout> | undefined;
export function scheduleReconcile(delayMs = 800): void {
  if (timer) clearTimeout(timer);
  timer = setTimeout(() => {
    void reconcileSchedules();
  }, delayMs);
}
