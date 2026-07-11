import { useEffect } from 'react';
import { AppState } from 'react-native';

import '@/stores'; // side-effect: registers all domain collections with the engine
import { useAuth } from '@/stores/auth';
import { useHabitLogs, useHabits } from '@/stores/habits';
import { useReminders } from '@/stores/reminders';
import { useTaskCompletions, useTasks } from '@/stores/tasks';
import { useWaterLogs, useWaterSettings } from '@/stores/water';
import { loadAllCollections, scheduleSync, syncAll } from '@/sync';
import { updateAriaWidget } from '@/widgets/register';

/**
 * Hydrates local data once the user is signed in (account or guest) and keeps it
 * synced: initial sync after load, and a sync whenever the app returns to foreground.
 */
export function DataProvider({ children }: { children: React.ReactNode }) {
  const status = useAuth((s) => s.status);
  const userId = useAuth((s) => s.user?.id ?? s.guestId ?? null);

  useEffect(() => {
    if (status !== 'signedIn') return;
    let active = true;
    loadAllCollections().then(() => {
      if (active) void syncAll();
    });
    return () => {
      active = false;
    };
  }, [status, userId]);

  useEffect(() => {
    const sub = AppState.addEventListener('change', (s) => {
      if (s === 'active') scheduleSync(0);
    });
    return () => sub.remove();
  }, []);

  // Keep the home-screen widget in sync with local data (debounced; Android only).
  useEffect(() => {
    let t: ReturnType<typeof setTimeout> | undefined;
    const trigger = () => {
      if (t) clearTimeout(t);
      t = setTimeout(() => void updateAriaWidget(), 1200);
    };
    const unsubs = [useTasks, useTaskCompletions, useHabits, useHabitLogs, useWaterLogs, useWaterSettings, useReminders].map(
      (s) => s.subscribe(trigger),
    );
    trigger();
    return () => {
      if (t) clearTimeout(t);
      unsubs.forEach((u) => u());
    };
  }, []);

  return <>{children}</>;
}
