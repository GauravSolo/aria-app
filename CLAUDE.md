# CLAUDE.md — Aria control tower

> This file is auto-loaded into context every session. It is the single source of
> truth for **what Aria is, how it's organized, and how to continue building it.**
> If you (a future Claude session or a human) are picking this up cold, read this
> file, then `docs/PROGRESS.md` (what's done / next), then `git log --oneline`.

---

## What Aria is

**Aria** is a personal-assistant **day-planner app** for one user (the repo owner).
It manages: daily planner/tasks, reminders, water intake, habits + streaks,
analytics, a dashboard, and home-screen widgets.

**Hard constraints (from `requirements.md`):**
- ❌ **No AI / LLM features of any kind.** No natural-language input. Everything is
  buttons, forms, checkboxes, dropdowns, selections. (The *name* "Aria" is just a
  personal-assistant vibe — it implies no AI.)
- ✅ Offline-first. Works with no network; syncs when online.
- ✅ Light + dark mode. Beautiful, modern, minimal UI (Todoist / TickTick / Notion vibe).

## Target devices (only these two — no web browser target)

| Device | App | Widget | Notifications |
|--------|-----|--------|---------------|
| OnePlus Android (latest) | Expo / React Native (`mobile/`) | Native Kotlin / Jetpack Glance | expo-notifications |
| MacBook M5 Pro (macOS) | Native SwiftUI (`macos/`) | WidgetKit extension | UserNotifications |

Both apps sync through **one Supabase project** (`backend/`).

## Architecture (two UIs, one backend)

```
repo root (this is the monorepo)
├─ backend/   Supabase: SQL schema, RLS policies, migrations           [SQL]
├─ mobile/    Expo + React Native + TypeScript  → Android app + widget  [TS]
├─ macos/     Native SwiftUI app + WidgetKit widget                     [Swift]
└─ docs/      PROGRESS.md (ledger) · DATA_MODEL.md (contract) · ARCHITECTURE.md
```

The phone (TypeScript) and Mac (Swift) **cannot share UI code**. They share only the
**data contract** in `docs/DATA_MODEL.md`, implemented identically against Supabase.
When you change the data model, update **all three**: `docs/DATA_MODEL.md`, the
Supabase SQL in `backend/`, and both app implementations.

## Stack

- **Backend / sync:** Supabase (Postgres + Auth + Realtime). Auth = **email + password**.
- **Mobile:** Expo SDK · TypeScript · expo-router · Zustand (persisted, offline-first) ·
  expo-notifications · `@supabase/supabase-js` · dayjs · custom lightweight charts.
- **Mac:** SwiftUI · WidgetKit · `supabase-swift` · UserNotifications.
- **Theme:** Indigo/Violet accent (`#6366F1` → `#8B5CF6`), full light + dark.

## Conventions (keep these consistent across the codebase)

- **Sync model:** every table has `user_id`, `updated_at`, `deleted_at` (soft delete).
  Conflict resolution = **last-write-wins by `updated_at`**. Local store is the
  source of truth offline; the sync engine pushes dirty rows and pulls newer rows.
- **IDs:** UUID v4 generated **client-side** (so offline creates work + sync cleanly).
- **Dates:** store timestamps as UTC ISO strings / `timestamptz`. "Per-day" things
  (`log_date`, `due_date`, `occurrence_date`) are plain `date` in the user's local day.
- **Mobile folders:** `src/app/` = expo-router screens only (route groups
  `(auth)` + `(tabs)`); logic lives in `src/{stores,lib,sync,components,theme,types}`.
  Screens stay thin. Path alias `@/*` → `mobile/src/*`. UI kit in `src/components/ui`.
- **No business logic in components.** Streak/water/analytics math lives in
  `src/lib/*` pure functions so it's testable and mirrored 1:1 in Swift.
- **Enums** (category, priority, frequency, …) are defined once in `docs/DATA_MODEL.md`
  and must match in SQL, TS, and Swift.

## How to run (filled in as each part lands — see docs for full steps)

- **Mobile:** `cd mobile && npm install && npx expo start` (press `a` for Android).
- **Backend:** create a free Supabase project, run `backend/schema.sql`, put URL + anon
  key in `mobile/.env` and the Mac app config. See `docs/SETUP.md` (added in step 14).
- **Mac:** open `macos/Aria.xcodeproj` in Xcode and run. (Needs full Xcode.)

## Build status

👉 **Always check `docs/PROGRESS.md` for the live status and what to do next.**
Commit one feature at a time. **Do not add a Claude co-author trailer to commits.**
