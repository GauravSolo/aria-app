import dayjs from 'dayjs';

/** Local calendar-day key (YYYY-MM-DD) used for *_date columns. */
export function todayKey(): string {
  return dayjs().format('YYYY-MM-DD');
}

export function dateKey(d: dayjs.ConfigType): string {
  return dayjs(d).format('YYYY-MM-DD');
}

/** e.g. "Tuesday, Jun 16" */
export function prettyDate(d?: dayjs.ConfigType): string {
  return dayjs(d).format('dddd, MMM D');
}

/** 0 = Sunday … 6 = Saturday */
export function weekdayIndex(d?: dayjs.ConfigType): number {
  return dayjs(d).day();
}

export function greeting(d: dayjs.Dayjs = dayjs()): string {
  const h = d.hour();
  if (h < 12) return 'Good morning';
  if (h < 17) return 'Good afternoon';
  return 'Good evening';
}
