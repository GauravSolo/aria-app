# PROGRESS.md — Aria build ledger

> Live status of the build. Update this **after every commit**. Legend:
> ✅ done · 🚧 in progress · ⬜ todo. Newest notes at the bottom of each section.

## Step checklist

| # | Step | Status |
|---|------|--------|
| 1 | Repo + scaffold (CLAUDE.md, docs, .gitignore, monorepo) | ✅ |
| 2 | Backend: Supabase schema + RLS + indexes | ✅ |
| 3 | Mobile core: Expo scaffold, theme, nav, Supabase client, auth | ✅ |
| 4 | Mobile data layer: types, Zustand stores, persistence, sync engine | ✅ |
| 5 | Daily Planner: tasks CRUD, timeline, priority/category/recurring | ✅ |
| 6 | Habits & Streaks: form, streak logic, calendar, stats | ✅ |
| 7 | Water tracking: goal, intervals, logging, progress, summaries | ✅ |
| 8 | Reminders + notifications: schedules, snooze, history | ✅ |
| 9 | Dashboard: aggregated home + quick-add | ✅ |
| 10 | Analytics: rates, trends, charts | ✅ |
| 11 | Android widget: config plugin + Kotlin/Glance | ✅ |
| 12 | Mac app: SwiftUI scaffold + auth + feature screens | ✅ |
| 13 | Mac WidgetKit widget | ✅ |
| 14 | Docs: setup, run, testing checklist, future ideas | ✅ |

## Current focus
✅ ALL 14 STEPS COMPLETE — MVP built. Remaining real-world work is on the user's machine:
add Supabase keys, run a mobile dev build (for notifications + Android widget), and build
the Mac app/widget in Xcode (see docs/SETUP.md). Git commits still paused pending the
user's personal git identity (see decisions log).

Step 14 done — docs/SETUP.md (full backend + phone + Mac build/run), docs/TESTING.md
(manual checklist), docs/FUTURE.md (improvement ideas); README + folder READMEs updated.

Steps 12–13 done. Mac app + widget (macos/):
- XcodeGen `project.yml` → Aria app + AriaWidgetExtension targets, App Group, supabase-swift.
- `Aria/Models.swift` + `Aria/Logic.swift` — Swift mirror of the data contract +
  recurrence/streak/water/score logic. ✅ type-checks with `swiftc -typecheck` (verified).
- `Aria/Supabase.swift` (client w/ snake_case coding + AuthModel), `Aria/AppStore.swift`
  (online-first load + mutations + widget snapshot publish), `Aria/Config.swift` (keys).
- Views: Auth, Root/MainView (NavigationSplitView sidebar), Dashboard, Tasks (+add sheet),
  Habits (+add sheet), Water (+settings). `Aria/Theme.swift`, `Views/Components.swift`.
- `AriaShared/WidgetBridge.swift` (App Group snapshot) + `ColorHex.swift`.
- `AriaWidget/` — WidgetKit small/medium/large reading the shared snapshot; bundle @main.
- ⚠️ SwiftUI/Supabase/WidgetKit layers authored, NOT compiled here (needs full Xcode +
  package resolution). Online-first MVP; build steps will be in docs/SETUP.md.

Step 11 done. Android widget via react-native-android-widget (chosen over hand-rolled
Glance so it actually assembles with Expo):
- app.json: widget config (Aria, 2x2, resizable, 30-min refresh).
- `src/widgets/snapshot.ts` — reads offline AsyncStorage directly + reuses pure libs to
  compute next task / pending / water% / habits today / top streak (no auth/store graph).
- `src/widgets/AriaWidget.tsx` — FlexWidget/TextWidget dark indigo card (compact + full).
- `src/widgets/widgetTaskHandler.tsx` — headless render on ADDED/UPDATE/RESIZED.
- `src/widgets/register.ts` + `register.android.tsx` — platform-guarded register + update.
- DataProvider triggers debounced `updateAriaWidget()` on data changes; registered in root.
- ⚠️ Needs an Expo dev build to place/verify on a device.

MOBILE APP FEATURE-COMPLETE (steps 1–10). Full Android bundle exports clean (5.1MB).
- Step 9 Dashboard: productivity score ring, quick-add (task/habit/water/reminder),
  today's tasks (top pending + toggle), today's habits (toggle + streak), water mini
  ring + add, upcoming reminders. `src/lib/analytics.ts` (productivityScore, pct).
- Step 10 Analytics: `computeAnalytics` over 7/30-day range (task & habit completion
  rates, missed tasks, top/longest streaks, water consistency, per-day productivity +
  water series, best day, per-habit breakdown). Screen with Week/Month toggle + charts.
- Shared `src/lib/reminders.ts` (reminderSummary, nextTriggerDate); reminders screen refactored.

Step 8 done. Reminders + notifications:
- `src/stores/reminders.ts` — useReminders + useNotificationHistory; add/update/delete,
  toggle, snooze, markReminderDone, recordNotification, notificationHistory.
- `src/lib/notifications.ts` — configure handler, Android channel, permissions, and
  `reconcileSchedules()` (cancel-all + reschedule from reminders + habit reminder_time +
  water interval slots within active hours). DATE/DAILY/WEEKLY/TIME_INTERVAL triggers.
- `src/providers/NotificationsProvider.tsx` — perms + reconcile on sign-in, reconcile on
  store changes (subscribe), log received notifications to history. Wired in root layout.
- Screens: `reminders` (Reminders + History tabs, enable Switch, snooze, mark-done) +
  `reminder-form` modal (once/daily/weekly+days/interval). Settings → Reminders link.
- app.json: added expo-notifications plugin (color + Android 13 permission).
- ⚠️ Notifications authored but NOT verifiable in sandbox — test on a device/dev build.

## Validated (updated)
- `expo export --platform android` clean through step 8 (5.1MB bundle, svg + notifications).

Step 7 done. Water tracking:
- `src/stores/water.ts` — useWaterLogs collection + useWaterSettings (bespoke per-user
  singleton, persisted + synced via upsert/maybeSingle). logWater, undoLastWater, WATER_DEFAULTS.
- `src/lib/water.ts` — sumByDate, totalForDate, weekSeries, monthStats, formatMl.
- Water screen: CircularProgress ring (% + total/goal), quick-add glass + custom-amount
  modal + undo, weekly BarChart w/ goal line, monthly StatTiles. `water-settings` modal
  (goal, glass size, reminder enable/interval/active hours via Switch + Stepper + time).
- New UI: CircularProgress (react-native-svg), BarChart (View-based).
- Added dep: react-native-svg.

Step 6 done. Habits & Streaks:
- `src/stores/habits.ts` — useHabits + useHabitLogs; activeHabits, logCountsFor,
  setHabitCount (revives tombstones for unique (habit_id, log_date)), toggleHabitDone,
  adjustHabitCount, add/update/archive/deleteHabit.
- `src/lib/streaks.ts` — isScheduledOn, computeHabitStats (current/longest/total/
  missed/successPct/week+month progress, today grace), buildCalendar (contribution
  grid), frequencyLabel.
- Screens: habits list (today summary + toggles), `habit-form` modal (name, kind,
  category, frequency + weekday picker, target stepper, color, start date, reminder
  time, notes), `habit/[id]` detail (today control, stat tiles, week/month bars,
  completion calendar).
- New UI: StatTile, Stepper; components habits/HabitCard, habits/HabitCalendar.
- Fixed: toggle revives soft-deleted completion/log rows so sync upserts don't violate
  DB unique constraints. `update()` may now patch `deleted_at`.

## Validated
- Full Android JS bundle exports cleanly (`expo export --platform android`): 1760
  modules, no import/runtime errors. tsc --noEmit clean after every step.

Step 5 done. Daily Planner:
- `src/stores/tasks.ts` — useTasks + useTaskCompletions collections; tasksForDate,
  isTaskDone, toggleTaskDone (boolean for one-off, completion rows for recurring),
  addTask/updateTask/deleteTask.
- `src/lib/recurrence.ts` — taskOccursOn(task, dayKey) expands none/daily/weekly/
  monthly/custom; recurrenceLabel.
- Planner screen: DateStrip (marked days), List + Timeline (agenda w/ time rail),
  day progress summary, FAB. `src/app/task-form.tsx` modal: title, description,
  category (chips), priority (segmented), date + start/end time pickers, recurrence
  (+ weekday picker / custom interval / repeat-until), edit + delete.
- New UI: Chip, Checkbox, ChipSelect, WeekdayPicker, DateTimeField, Fab, DateStrip,
  ProgressBar; component `components/tasks/TaskCard`.
- Added dep: @react-native-community/datetimepicker (config plugin auto-added to app.json).

Step 4 done. Data layer:
- `src/lib/storage.ts` — JSON AsyncStorage wrapper.
- `src/sync/collectionStore.ts` — `createCollection<T>(table)` factory: in-memory +
  persisted map of rows, `list/getById/create/update/remove` (soft delete + dirty
  marking), and `push`/`pull` (last-write-wins by updated_at). Auto-registers with engine.
- `src/sync/engine.ts` — registry, `loadAllCollections`, `syncAll`, debounced
  `scheduleSync`, `useSyncStatus`. Guests never sync; offline failures are swallowed.
- `src/providers/DataProvider.tsx` — hydrates + syncs on sign-in and app foreground;
  wired into root layout. Imports `@/stores` so all collections register up front.
- Add domain store exports to `src/stores/index.ts` as each lands.

Step 3 done. Mobile core (Expo SDK 56, RN 0.85, React 19, expo-router typed routes):
- Theme: `src/theme` — indigo/violet tokens, light+dark, `ThemeProvider` + `useTheme`,
  mode persisted (system/light/dark).
- UI kit: `src/components/ui` — Text, Button, Card, Screen, Header, TextField,
  Segmented, Icon (Ionicons), IconButton, BrandMark, EmptyState.
- Nav: root `_layout` auth gate (redirects on auth status) + `(auth)` stack +
  `(tabs)` (Today/Planner/Habits/Water/Stats) + `settings` modal. Tab screens are
  placeholders to be filled in steps 5–10.
- Auth: `src/stores/auth.ts` (Zustand) — email+password via Supabase, plus a
  **guest/local mode** (`continueAsGuest`) so the app runs with no keys; sync stays
  off until real sign-in. `activeUserId()` returns user id or guest id.
- Supabase client: `src/lib/supabase.ts` (env-driven, `isSupabaseConfigured` guard).
- Helpers: `src/lib/id.ts` (uuid/nowIso), `src/lib/date.ts` (dayjs day keys).
- Config: app.json branded (name Aria, scheme aria, pkg com.aria.planner, indigo splash).
- Added deps: @supabase/supabase-js, @react-native-async-storage/async-storage,
  zustand, dayjs, react-native-url-polyfill, @expo/vector-icons.

## Decisions log
- 2026-06-16: ⚠️ COMMITS PAUSED. Work laptop's git identity (global + local) is the
  work email (gaurav.sharma@arintra.com). User wants personal identity before any
  commits. Until then: build only, NO git commits/amends/resets. One pre-existing
  commit (d6abbc6, work email, local-only, unpushed) to be rewritten later.
- 2026-06-16: Two app codebases (Expo/Android + SwiftUI/Mac), one Supabase backend.
- 2026-06-16: Auth = email + password. Theme = Indigo/Violet, light + dark. Name = Aria.
- 2026-06-16: No web browser target. No AI features (hard constraint).
- 2026-06-16: Sync = last-write-wins by `updated_at`; soft deletes via `deleted_at`;
  client-generated UUIDs.

## Known gaps / TODO (track things deferred mid-build here)
- Native widgets (Android + macOS) cannot be verified in the dev sandbox — written
  here, built/verified by the user on their machine.
- Full Xcode not installed in sandbox (only Command Line Tools) → Mac app authored,
  not compiled here.

## Verified in sandbox
- `npx tsc --noEmit` clean after every mobile step.
- `npx expo export --platform android` bundles the whole app with no errors (through step 6).
- Native widgets + Mac app remain build-on-your-machine (no emulator/Xcode here).
