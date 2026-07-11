# android-native/ — Aria for Android (native Kotlin)

A **native** Android app: Kotlin · Jetpack Compose (Material 3) · **Jetpack Glance**
home-screen widget · Supabase (auth + Postgrest). Built specifically so the
**home-screen widget works** (the Expo/React-Native widget can't run on SDK 56 / the
New Architecture). Hits the **same Supabase project** as the Expo & Mac apps, so your
data syncs across all of them.

## Why this exists
`react-native-android-widget` doesn't support RN 0.85's New Architecture yet, and RN
0.85 forbids disabling it — so the Expo widget renders blank. Jetpack Glance is Google's
native widget toolkit; the app and widget share storage **in-process** (DataStore), so
there are no cross-process / App-Group issues. The widget just works.

## Build & run
Your Mac is already set up from the Expo work (Android Studio, JDK 21, SDK, `ANDROID_HOME`).

1. In **Android Studio**: **File → Open** → select the **`android-native`** folder
   (open this folder itself, not the repo root).
2. Let it **Gradle sync** (it downloads Gradle 8.13 + dependencies; first sync takes a
   few minutes and will generate the Gradle wrapper if needed).
3. Plug in your **OnePlus 11R** (USB debugging) *or* pick the emulator, then press **▶ Run**.
4. Sign in with the **same account** you use on the Mac app → your data appears.
5. Add the widget: long-press home screen → **Widgets** → **Aria — Today** → drag it out.
   Open the app once so it writes the first snapshot.

`applicationId` is `com.aria.app` (distinct from the Expo build `com.aria.planner`), so
both can be installed side by side.

## Layout
```
app/src/main/java/com/aria/app/
  Config.kt              Supabase URL + anon key
  AriaApp.kt             Application → inits Supabase
  MainActivity.kt        Compose entry (auth gate)
  data/                  Models, Supa client, PrefsSessionManager, Repository,
                         AppViewModel, Logic (recurrence/streaks/water), WidgetStore
  ui/                    Theme, AuthScreen, Screens (dashboard/planner/habits/water)
  widget/                AriaWidget (Glance) + AriaWidgetReceiver
res/xml/aria_widget_info.xml   widget metadata
```

## Status / caveats
- Authored on your machine's SDK; **not compiled in the build sandbox**. On first sync
  you *may* need to nudge a dependency version or an import (Supabase-Kotlin / Glance /
  Compose APIs evolve). These are quick fixes — tell the assistant the exact error.
- Feature scope: auth · dashboard · planner (tasks) · habits + streaks · water · **Glance
  widget**. Reminders/analytics can be ported next.
