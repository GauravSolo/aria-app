# mobile/ — Aria for Android (Expo + React Native + TypeScript)

The Android app and its native home-screen widget. Also runnable via Expo on a
device/emulator. Built incrementally — see [../docs/PROGRESS.md](../docs/PROGRESS.md).

## Run (summary — full steps in docs/SETUP.md later)
```bash
cd mobile
npm install
npx expo start      # press 'a' to open Android
```
Requires a `.env` with `EXPO_PUBLIC_SUPABASE_URL` and `EXPO_PUBLIC_SUPABASE_ANON_KEY`
(see `.env.example`, added with the core scaffold in step 3).

## Layout
- `app/` — expo-router screens (thin).
- `src/lib/` — pure logic (streaks, water, recurrence, analytics, dates).
- `src/stores/` — Zustand stores per domain.
- `src/sync/` — Supabase client + sync engine.
- `src/components/`, `src/theme/`, `src/types/`.
