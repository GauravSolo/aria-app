import dayjs from 'dayjs';

import { WEEKDAY_SHORT, type Reminder } from '@/types/db';

export const fmtTime = (hm: string) => dayjs(`2000-01-01T${hm}`).format('h:mm A');

function parseHM(hm: string): { hour: number; minute: number } {
  const [h, m] = hm.split(':').map((n) => parseInt(n, 10));
  return { hour: h || 0, minute: m || 0 };
}

export function reminderSummary(r: Reminder): string {
  switch (r.repeat) {
    case 'once':
      return r.next_trigger_at ? dayjs(r.next_trigger_at).format('MMM D · h:mm A') : 'One-time';
    case 'daily':
      return r.time_of_day ? `Every day · ${fmtTime(r.time_of_day)}` : 'Every day';
    case 'weekly': {
      const days = r.repeat_days.map((d) => WEEKDAY_SHORT[d]).join(', ');
      return `${days || 'Weekly'}${r.time_of_day ? ` · ${fmtTime(r.time_of_day)}` : ''}`;
    }
    case 'interval':
      return `Every ${r.interval_min ?? 0} min`;
    default:
      return '';
  }
}

/** Best-effort next fire time, used to sort "upcoming" reminders. */
export function nextTriggerDate(r: Reminder, now: dayjs.Dayjs = dayjs()): Date | null {
  if (!r.is_enabled) return null;
  if (r.snooze_until && new Date(r.snooze_until) > now.toDate()) return new Date(r.snooze_until);

  if (r.repeat === 'once') return r.next_trigger_at ? new Date(r.next_trigger_at) : null;
  if (r.repeat === 'interval') return r.interval_min ? now.add(r.interval_min, 'minute').toDate() : null;
  if (!r.time_of_day) return null;

  const { hour, minute } = parseHM(r.time_of_day);
  if (r.repeat === 'daily') {
    let t = now.hour(hour).minute(minute).second(0);
    if (t.isBefore(now)) t = t.add(1, 'day');
    return t.toDate();
  }
  if (r.repeat === 'weekly') {
    for (let offset = 0; offset <= 7; offset++) {
      const day = now.add(offset, 'day');
      if (r.repeat_days.includes(day.day())) {
        const cand = day.hour(hour).minute(minute).second(0);
        if (cand.isAfter(now)) return cand.toDate();
      }
    }
  }
  return null;
}
