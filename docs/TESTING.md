# TESTING.md — manual test checklist

No automated tests yet (see FUTURE.md). This is the manual pass to confirm a build works.
`npx tsc --noEmit` (in `mobile/`) and the bundle export are the automated safety nets used
during development.

## Setup
- [ ] `backend/schema.sql` runs with no errors; tables + RLS visible in Supabase.
- [ ] `mobile/.env` has URL + anon key; `npx expo start` boots with no red screen.
- [ ] Mac: `xcodegen generate` succeeds; project builds in Xcode; `Config.swift` filled.

## Auth & sync
- [ ] Sign up creates an account; a `profiles` + `water_settings` row appears (signup trigger).
- [ ] Sign out → sign in works.
- [ ] “Continue without an account” (guest) lets you use the app fully.
- [ ] Same account on phone + Mac → create a task on one, it appears on the other after a sync.
- [ ] Airplane mode: create/edit/complete still works; reconnect → changes sync up.

## Planner / tasks
- [ ] Add task (title, category, priority, date, start/end time) → shows on its day.
- [ ] Complete / uncomplete toggles and persists.
- [ ] Recurring task (daily / weekly-by-day / monthly / custom interval) appears on the right days.
- [ ] Completing a recurring task only marks that day (other days stay open).
- [ ] Edit + delete work. List vs Timeline views both render. Date strip marks days with tasks.

## Habits & streaks
- [ ] Create habit (daily and weekly variants); toggle done today.
- [ ] Current streak increases on consecutive completed days; resets after a missed scheduled day.
- [ ] Longest streak, total, success %, week/month progress look right.
- [ ] Completion calendar colors past/missed/today/future correctly.
- [ ] Multi-count habit (target > 1) increments with the stepper; “done” at target.

## Water
- [ ] One-tap glass adds the configured amount; custom amount modal works; undo removes last.
- [ ] Ring + % update; weekly bar chart shows today highlighted with goal line.
- [ ] Monthly stats (total, average, days met goal, days tracked) update.
- [ ] Settings: change goal / glass / interval / active hours → persists and re-renders.

## Reminders & notifications (dev build / device required)
- [ ] Create once / daily / weekly / interval reminders.
- [ ] Permission prompt appears on first run; granting it schedules notifications.
- [ ] A daily reminder fires at the set time; appears in History.
- [ ] Snooze reschedules; “mark done” logs to History and disables one-time reminders.
- [ ] Water reminders fire within the active-hours window at the interval.

## Dashboard
- [ ] Productivity score reflects task/habit/water completion.
- [ ] Quick-add buttons: Task/Habit/Reminder open forms; Water logs a glass instantly.
- [ ] Today's tasks, habit toggles, water mini-ring, upcoming reminders all reflect live data.

## Analytics
- [ ] Week/Month toggle recomputes.
- [ ] Completion rates, missed tasks, streaks, water consistency, best day, habit breakdown render.

## Widgets (build on device)
- [ ] **Android:** place the “Aria — Today” widget; it shows water %, tasks left, habits, streak,
      next task; updates after you change data / reopen the app.
- [ ] **Mac:** add the Aria widget (small/medium/large); shows the same snapshot; tap opens the app.

## Theme
- [ ] Light/Dark/System toggle in Settings switches the whole app.
- [ ] Layout looks right on phone sizes and a resized Mac window.
