# FUTURE.md — improvement ideas

Aria's MVP is complete. Natural next steps, roughly by value:

## Sync & data
- **Realtime sync**: subscribe to Supabase Realtime so phone↔Mac update instantly (the
  schema already publishes tables to `supabase_realtime`).
- **Guest → account migration**: when a guest signs in, reassign local rows' `user_id`.
- **Conflict UI**: surface/merge edits beyond last-write-wins; per-field merge for notes.
- **Mac offline cache**: bring the Mac app to full offline-first parity (SwiftData/file cache
  + the same dirty/push/pull engine the phone uses).

## Features
- **Mac parity**: recurrence editor, habit detail + calendar, analytics, reminders UI on Mac.
- **Sub-tasks / checklists** within a task; **tags**; attachments/notes.
- **Drag-to-reschedule** on the planner timeline; week/month calendar views.
- **Habit insights**: best time of day, weekday heatmap, “at-risk streak” nudges.
- **Water**: smart reminders that pause after you log; HealthKit / Google Fit sync.
- **Reminders**: rich notification actions (Done / Snooze buttons), per-reminder sounds.
- **Search & filters** across tasks/habits; quick command palette.

## Polish
- **Onboarding** with sample data + a goal-setting flow.
- **Haptics & micro-animations** (reanimated) for completing tasks/habits.
- **Custom themes / accent picker**; per-category icons; app icon variants.
- **Localization** + accessibility audit (VoiceOver/TalkBack, dynamic type).

## Platform
- **iOS app + iOS/iPadOS widgets** — the Expo app already targets iOS; add WidgetKit there too.
- **Android widget**: interactive actions (tick a task from the widget), themed light/dark.
- **Watch**: complications for streak / water.

## Engineering
- **Tests**: unit-test the pure libs (`recurrence`, `streaks`, `water`, `analytics`) in TS and
  the Swift `Logic.swift`; component tests; a Detox/Maestro e2e smoke flow.
- **CI**: typecheck + lint + bundle export on PRs; EAS build pipeline.
- **Error/analytics tooling** (Sentry) and a crash-free release process.
- **Backups/export**: let the user export their data as JSON/CSV.
