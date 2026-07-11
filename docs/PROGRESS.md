# PROGRESS.md — Aria build ledger

> Live status of the build. Update this **after every commit**. Legend:
> ✅ done · 🚧 in progress · ⬜ todo. Newest notes at the bottom of each section.

## Step checklist

| # | Step | Status |
|---|------|--------|
| 1 | Repo + scaffold (CLAUDE.md, docs, .gitignore, monorepo) | ✅ |
| 2 | Backend: Supabase schema + RLS + indexes | 🚧 |
| 3 | Mobile core: Expo scaffold, theme, nav, Supabase client, auth | ⬜ |
| 4 | Mobile data layer: types, Zustand stores, persistence, sync engine | ⬜ |
| 5 | Daily Planner: tasks CRUD, timeline, priority/category/recurring | ⬜ |
| 6 | Habits & Streaks: form, streak logic, calendar, stats | ⬜ |
| 7 | Water tracking: goal, intervals, logging, progress, summaries | ⬜ |
| 8 | Reminders + notifications: schedules, snooze, history | ⬜ |
| 9 | Dashboard: aggregated home + quick-add | ⬜ |
| 10 | Analytics: rates, trends, charts | ⬜ |
| 11 | Android widget: config plugin + Kotlin/Glance | ⬜ |
| 12 | Mac app: SwiftUI scaffold + auth + feature screens | ⬜ |
| 13 | Mac WidgetKit widget | ⬜ |
| 14 | Docs: setup, run, testing checklist, future ideas | ⬜ |

## Current focus
Step 2 — `backend/` Supabase schema (tables, enums, RLS, indexes, triggers).

## Decisions log
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
- (filled in as steps are type-checked / built)
