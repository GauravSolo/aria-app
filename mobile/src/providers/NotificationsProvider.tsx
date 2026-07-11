import * as Notifications from 'expo-notifications';
import { useEffect } from 'react';

import {
  configureNotifications,
  ensureAndroidChannel,
  ensurePermissions,
  reconcileSchedules,
  scheduleReconcile,
} from '@/lib/notifications';
import { useAuth } from '@/stores/auth';
import { useHabits } from '@/stores/habits';
import { recordNotification, useReminders } from '@/stores/reminders';
import { useWaterSettings } from '@/stores/water';
import type { ReminderKind } from '@/types/db';

configureNotifications();

export function NotificationsProvider({ children }: { children?: React.ReactNode }) {
  const status = useAuth((s) => s.status);

  useEffect(() => {
    if (status !== 'signedIn') return;
    let mounted = true;

    (async () => {
      await ensureAndroidChannel();
      await ensurePermissions();
      if (mounted) await reconcileSchedules();
    })();

    // Re-schedule when any source of reminders changes.
    const unsubReminders = useReminders.subscribe(() => scheduleReconcile());
    const unsubHabits = useHabits.subscribe(() => scheduleReconcile());
    const unsubWater = useWaterSettings.subscribe(() => scheduleReconcile());

    // Log notifications that arrive while the app is running.
    const received = Notifications.addNotificationReceivedListener((n) => {
      const data = (n.request.content.data ?? {}) as {
        reminderId?: string;
        kind?: ReminderKind;
        title?: string;
        body?: string;
      };
      recordNotification({
        reminderId: data.reminderId ?? null,
        title: data.title ?? n.request.content.title ?? 'Reminder',
        body: data.body ?? n.request.content.body ?? null,
        kind: data.kind ?? 'custom',
        status: 'delivered',
      });
    });

    return () => {
      mounted = false;
      unsubReminders();
      unsubHabits();
      unsubWater();
      received.remove();
    };
  }, [status]);

  return <>{children}</>;
}
