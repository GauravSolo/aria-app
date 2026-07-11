/**
 * Importing this module evaluates every domain store, which registers its
 * collection with the sync engine. DataProvider imports it so all collections
 * are known before the first load/sync. Add each domain store's export here as
 * it lands (steps 5–8): tasks, habits, water, reminders.
 */
export { useAuth, activeUserId } from './auth';
export { useTasks, useTaskCompletions } from './tasks';
export { useHabits, useHabitLogs } from './habits';
export { useWaterLogs, useWaterSettings } from './water';
export { useReminders, useNotificationHistory } from './reminders';
