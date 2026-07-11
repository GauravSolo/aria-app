# SETUP.md — build & run Aria

Aria is a monorepo: a **Supabase** backend, an **Expo** Android app (`mobile/`), and a
native **SwiftUI** Mac app (`macos/`). Both apps share one Supabase project and sync.

---

## 0. Prerequisites

| For | Install |
|-----|---------|
| Everything | [Node.js 18+](https://nodejs.org), npm, git |
| Backend | A free [Supabase](https://supabase.com) account |
| Android app | Android Studio (SDK + an emulator) **or** a physical Android phone, plus the Expo CLI (bundled via `npx`) |
| Mac app | macOS 14+, **full Xcode** (not just Command Line Tools), [Homebrew](https://brew.sh) |

---

## 1. Backend (Supabase)

1. Create a new project at https://supabase.com.
2. Open **SQL Editor**, paste the contents of [`backend/schema.sql`](../backend/schema.sql), and **Run**.
   (It's re-runnable — safe to run again after edits.)
3. Go to **Project Settings → API** and copy:
   - **Project URL** (e.g. `https://abcd.supabase.co`)
   - **anon public** key
4. (Optional) **Authentication → Providers → Email**: turn *Confirm email* off for the
   fastest testing, or leave on and confirm via the email link.

These two values go into both apps below.

---

## 2. Android app (`mobile/`)

```bash
cd mobile
npm install
cp .env.example .env
# edit .env → paste your EXPO_PUBLIC_SUPABASE_URL and EXPO_PUBLIC_SUPABASE_ANON_KEY
```

### Run it
- **Quick preview (Expo Go):**
  ```bash
  npx expo start      # press "a" for Android, or scan the QR with Expo Go
  ```
  Good for the UI. ⚠️ **Notifications and the home-screen widget do NOT work in Expo Go** —
  they need a dev build (next).

- **Full dev build (notifications + widget):**
  ```bash
  npx expo run:android        # builds & installs a dev client (needs Android Studio/SDK)
  ```
  or use EAS: `npm i -g eas-cli && eas build --profile development --platform android`.

  After it installs: long-press the home screen → **Widgets** → **Aria — Today** to place it.

### Notes
- **No account?** Tap **“Continue without an account”** — the app works fully offline; sync
  turns on when you later sign in.
- Sign in with the **same account** on phone and Mac to sync.

---

## 3. Mac app (`macos/`)

```bash
brew install xcodegen          # one-time
cd macos
xcodegen generate              # creates Aria.xcodeproj from project.yml
open Aria.xcodeproj
```

In Xcode:
1. Open **`Aria/Config.swift`** and paste your Supabase **URL** + **anon key**.
2. Select the **Aria** target → **Signing & Capabilities** → choose your Team. Do the same
   for the **AriaWidgetExtension** target. (Both already declare the
   `group.com.aria.planner` App Group — keep them identical.)
3. Pick the **Aria** scheme → **Run** (⌘R).
4. The widget shows up in **Notification Center / desktop widgets** → edit widgets → **Aria**.

> The Mac app is **online-first** (the phone is the offline-first primary). It reads/writes
> Supabase directly and republishes a snapshot the widget reads.

---

## Project layout

```
backend/   schema.sql (+ README)            — Supabase
mobile/    Expo app — src/{app,components,lib,stores,sync,theme,widgets,types}
macos/     SwiftUI — project.yml, Aria/, AriaShared/, AriaWidget/
docs/      ARCHITECTURE · DATA_MODEL · PROGRESS · SETUP · TESTING · FUTURE
```

## Troubleshooting
- **“Supabase not connected” banner** → `.env` (mobile) or `Config.swift` (Mac) keys missing/typo'd.
- **RLS errors / empty data** → make sure `schema.sql` ran fully (RLS policies + trigger).
- **Widget shows nothing** → open the app once while signed in so it can publish a snapshot.
- **Android build fails** → confirm Android Studio SDK + an emulator/device; retry `npx expo run:android`.
- **Mac signing error** → set a Team on *both* targets; App Group must match on both.
