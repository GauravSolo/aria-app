# Aria — your personal day planner

A clean, offline-first personal-assistant planner for **Android** and **macOS**, with
cross-device sync, home-screen widgets, and notifications. **No AI** — everything is
forms, buttons, checkboxes, and dropdowns.

Features: daily planner & tasks · reminders · water tracking · habits & streaks ·
analytics dashboard · home-screen widgets · light + dark mode.

## Repository layout

```
backend/   Supabase database: SQL schema, RLS policies, migrations
mobile/    Expo + React Native + TypeScript  → Android app + home-screen widget
macos/     Native SwiftUI app + WidgetKit widget
docs/      ARCHITECTURE.md · DATA_MODEL.md · PROGRESS.md (+ SETUP/testing later)
```

Two native UIs (phone in TypeScript, Mac in Swift) over **one Supabase backend**.
See [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md) for the why and how.

## Status

This is built incrementally, one feature per commit. **Current status lives in
[docs/PROGRESS.md](docs/PROGRESS.md).** Setup & run instructions for phone and Mac are
added in the final docs step (and summarized in each app folder's README as it lands).

## Quick orientation for developers

- Start with [CLAUDE.md](CLAUDE.md) (project overview + conventions).
- The data contract both apps implement: [docs/DATA_MODEL.md](docs/DATA_MODEL.md).
- What's done / next: [docs/PROGRESS.md](docs/PROGRESS.md) and `git log --oneline`.
