# ARCHITECTURE.md — Aria

## Why this shape

Aria targets exactly two devices: an **Android phone** and a **macOS laptop**, with
**cross-device sync**, **native home-screen widgets**, and **notifications** on both.

Real OS widgets are inherently native — an Android widget must be Kotlin/Jetpack
Glance, a macOS widget must be Swift/WidgetKit. No cross-platform framework produces
both from one shared UI. So Aria is **two native UIs over one shared backend**:

```
        ┌─────────────────────────────────────────────┐
        │                 Supabase                     │
        │   Postgres + Row Level Security + Auth        │
        │   + Realtime  (the sync hub & source of truth)│
        └───────────────▲───────────────▲──────────────┘
                        │               │
              sync (REST/Realtime)  sync (REST/Realtime)
                        │               │
   ┌────────────────────┴───┐   ┌───────┴────────────────────┐
   │  mobile/  (Expo / RN)   │   │  macos/  (SwiftUI)          │
   │  • screens (expo-router)│   │  • SwiftUI views            │
   │  • Zustand stores       │   │  • ObservableObject stores  │
   │  • local persistence    │   │  • local cache (File/SwiftData)
   │  • sync engine          │   │  • sync engine              │
   │  • expo-notifications    │   │  • UserNotifications        │
   │  • Android widget (Kotlin/Glance)│ • WidgetKit widget      │
   └─────────────────────────┘   └─────────────────────────────┘
```

## Offline-first sync

1. All reads/writes hit the **local store** first → UI is instant and works offline.
2. Writes mark rows dirty (bump `updated_at`); soft-deletes set `deleted_at`.
3. A **sync engine** runs on app start, on reconnect, and periodically:
   - **push:** upsert local dirty rows to Supabase.
   - **pull:** fetch rows where `updated_at > last_pulled_at`, merge by last-write-wins.
4. UUIDs are client-generated so offline creates never collide on sync.

## Notifications

- **Scheduling is local** on each device (expo-notifications / UserNotifications) so
  reminders fire **offline**. The `reminders` table is the synced schedule; each device
  reconciles its local OS notifications from that table.
- Snooze = push `snooze_until` and reschedule. Firing writes a `notification_history` row.

## Widgets

- Widgets are **read-only projections** of the dashboard data (next task, pending count,
  water progress, next water reminder, active streaks, today's habit progress).
- They read from a small shared store the app writes to:
  - Android: `SharedPreferences`/DataStore written by the app, read by Glance.
  - macOS: App Group `UserDefaults` written by the app, read by the widget; refreshed
    via `WidgetCenter.reloadAllTimelines()`.

## Module map (filled in as code lands)

- `mobile/src/lib/` — pure logic: `streaks`, `water`, `recurrence`, `analytics`, `date`.
- `mobile/src/stores/` — Zustand stores per domain (tasks, habits, water, reminders…).
- `mobile/src/sync/` — sync engine + Supabase client wrapper.
- `macos/Aria/` — SwiftUI app; `macos/AriaWidget/` — widget extension.
