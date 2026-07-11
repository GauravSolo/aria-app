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

## Get started

- **Setup & run (Supabase + phone + Mac):** [docs/SETUP.md](docs/SETUP.md)
- **Test checklist:** [docs/TESTING.md](docs/TESTING.md)
- **Future ideas:** [docs/FUTURE.md](docs/FUTURE.md)

```bash
# Backend: run backend/schema.sql in your Supabase SQL editor.
cd mobile && npm install && cp .env.example .env   # add Supabase keys, then:
npx expo start                 # UI preview (notifications/widget need a dev build)
# Mac:
cd macos && brew install xcodegen && xcodegen generate && open Aria.xcodeproj
```

## Status

MVP complete: backend, full Android/mobile app, native Android widget, Mac SwiftUI app,
and macOS widget. **Live status & what each step delivered:** [docs/PROGRESS.md](docs/PROGRESS.md).
The mobile app type-checks (`tsc`) and bundles (`expo export`) clean; native widgets and
the Mac app are built on your machine (Xcode / a device dev build) per docs/SETUP.md.

## Quick orientation for developers

- Start with [CLAUDE.md](CLAUDE.md) (project overview + conventions).
- The data contract both apps implement: [docs/DATA_MODEL.md](docs/DATA_MODEL.md).
- What's done / next: [docs/PROGRESS.md](docs/PROGRESS.md) and `git log --oneline`.
