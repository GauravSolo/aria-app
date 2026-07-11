# macos/ — Aria for Mac (SwiftUI + WidgetKit)

The native macOS app and its WidgetKit desktop widget. Built incrementally — see
[../docs/PROGRESS.md](../docs/PROGRESS.md).

> Authored in this repo as Swift sources + project config. Build & run requires
> **full Xcode** (Command Line Tools alone won't build a SwiftUI app + widget).

## Run (summary — full steps in docs/SETUP.md later)
1. Open `Aria.xcodeproj` (or the SwiftPM package) in Xcode.
2. Set your Supabase URL + anon key in the app config.
3. Select the **Aria** scheme → Run. The widget appears in the macOS widget gallery.

## Layout
- `Aria/` — SwiftUI app (views, stores, models, sync, notifications).
- `AriaWidget/` — WidgetKit extension (small / medium / large).
- Shared data models mirror [../docs/DATA_MODEL.md](../docs/DATA_MODEL.md).
