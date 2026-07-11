# DATA_MODEL.md — Aria data contract (authoritative)

> The **single source of truth** for Aria's data. The Supabase SQL (`backend/`),
> the TypeScript types (`mobile/src/types`), and the Swift models (`macos/`) must
> all match this file. Change here first, then propagate to all three.

## Conventions

- **PK:** `id uuid` (client-generated UUID v4) on every table except singletons keyed by `user_id`.
- **Ownership:** every table has `user_id uuid` → `auth.users.id`. RLS restricts rows to the owner.
- **Sync columns (every table):**
  - `created_at timestamptz default now()`
  - `updated_at timestamptz default now()` — bumped on every write (client sets it).
  - `deleted_at timestamptz null` — **soft delete**; non-null = tombstone, hidden in UI, kept for sync.
- **Conflict rule:** last-write-wins by `updated_at`.
- **Timestamps** are UTC. **`date` columns** (`*_date`) are the user's *local* calendar day (no tz).

## Enums (must be identical in SQL / TS / Swift)

| Enum | Values |
|------|--------|
| `category` | `work` · `study` · `health` · `personal` · `other` |
| `priority` | `low` · `medium` · `high` |
| `task_recurrence` | `none` · `daily` · `weekly` · `monthly` · `custom` |
| `habit_kind` | `build` · `quit` |
| `frequency` | `daily` · `weekly` · `custom` |
| `reminder_kind` | `task` · `habit` · `water` · `custom` |
| `reminder_repeat` | `once` · `daily` · `weekly` · `interval` |
| `notif_status` | `delivered` · `done` · `snoozed` · `dismissed` |

`days_of_week`: integer array, `0=Sunday … 6=Saturday`.

---

## Tables

### `profiles` (1 row per user)
| col | type | notes |
|-----|------|-------|
| id | uuid PK | = `auth.users.id` |
| display_name | text | |
| theme | text | `system` \| `light` \| `dark` (default `system`) |
| created_at / updated_at | timestamptz | |

### `tasks`
| col | type | notes |
|-----|------|-------|
| id | uuid PK | |
| user_id | uuid | |
| title | text | required |
| description | text | nullable |
| category | category | default `other` |
| priority | priority | default `medium` |
| start_time | timestamptz | nullable — scheduled start (drives hourly timeline) |
| end_time | timestamptz | nullable |
| due_date | date | nullable |
| recurrence | task_recurrence | default `none` |
| recurrence_interval | int | default 1 — every N (days/weeks/months) for `custom`/repeats |
| recurrence_days | int[] | for `weekly` — which `days_of_week` |
| recurrence_end_date | date | nullable — stop recurring after |
| is_completed | bool | default false — used by **non-recurring** tasks |
| completed_at | timestamptz | nullable |
| sort_order | int | default 0 — manual ordering in a day |
| created_at / updated_at / deleted_at | | sync cols |

### `task_completions` (per-occurrence completion for **recurring** tasks)
| col | type | notes |
|-----|------|-------|
| id | uuid PK | |
| user_id | uuid | |
| task_id | uuid | → tasks.id |
| occurrence_date | date | the day this occurrence belongs to |
| completed_at | timestamptz | |
| created_at / updated_at / deleted_at | | |
| **unique** | (task_id, occurrence_date) | |

> Non-recurring tasks: use `tasks.is_completed`. Recurring tasks: a row here marks a
> specific day done. Occurrences are *computed* from the recurrence rule, not stored.

### `habits`
| col | type | notes |
|-----|------|-------|
| id | uuid PK | |
| user_id | uuid | |
| name | text | required |
| kind | habit_kind | default `build` |
| category | category | default `health` |
| frequency | frequency | default `daily` |
| target_count | int | default 1 — completions per period (e.g. read 20 pages → 1; pushups → N) |
| custom_days | int[] | for `weekly`/`custom` — which `days_of_week` are active |
| reminder_time | time | nullable — local time of day |
| start_date | date | when tracking begins |
| notes | text | nullable |
| color | text | nullable — hex accent for UI |
| icon | text | nullable — icon name |
| is_archived | bool | default false |
| sort_order | int | default 0 |
| created_at / updated_at / deleted_at | | |

### `habit_logs` (one per habit per day it was acted on)
| col | type | notes |
|-----|------|-------|
| id | uuid PK | |
| user_id | uuid | |
| habit_id | uuid | → habits.id |
| log_date | date | |
| count | int | default 1 — progress toward `target_count` |
| created_at / updated_at / deleted_at | | |
| **unique** | (habit_id, log_date) | |

> "Completed today" = `count >= habit.target_count`. Streaks are **computed** from
> `habit_logs` + the habit schedule (see `lib/streaks`), never stored.

### `water_settings` (1 row per user)
| col | type | notes |
|-----|------|-------|
| user_id | uuid PK | |
| daily_goal_ml | int | default 4000 |
| glass_size_ml | int | default 250 |
| reminder_interval_min | int | default 45 |
| reminder_enabled | bool | default true |
| active_start | time | default 08:00 — only remind within window |
| active_end | time | default 22:00 |
| updated_at | timestamptz | |

### `water_logs`
| col | type | notes |
|-----|------|-------|
| id | uuid PK | |
| user_id | uuid | |
| log_date | date | |
| amount_ml | int | |
| logged_at | timestamptz | |
| created_at / updated_at / deleted_at | | |

### `reminders`
| col | type | notes |
|-----|------|-------|
| id | uuid PK | |
| user_id | uuid | |
| title | text | required |
| body | text | nullable |
| kind | reminder_kind | default `custom` |
| ref_id | uuid | nullable — task/habit it belongs to |
| repeat | reminder_repeat | default `once` |
| repeat_days | int[] | for `weekly` |
| interval_min | int | nullable — for `interval` (e.g. water every 45m) |
| time_of_day | time | nullable — for daily/weekly |
| next_trigger_at | timestamptz | next fire time (recomputed on fire/snooze) |
| is_enabled | bool | default true |
| snooze_until | timestamptz | nullable |
| local_notification_id | text | nullable — OS notification handle (not synced cross-device) |
| created_at / updated_at / deleted_at | | |

### `notification_history`
| col | type | notes |
|-----|------|-------|
| id | uuid PK | |
| user_id | uuid | |
| reminder_id | uuid | nullable |
| title | text | |
| body | text | nullable |
| kind | reminder_kind | |
| fired_at | timestamptz | |
| status | notif_status | default `delivered` |
| created_at / updated_at / deleted_at | | |

---

## Derived (computed, never stored)

- **Task occurrences** — expanded from `tasks.recurrence*` for a given date range.
- **Habit streaks** — current / longest / total / missed, from `habit_logs` + schedule.
- **Water totals** — daily/weekly/monthly sums from `water_logs`, vs `daily_goal_ml`.
- **Analytics** — completion rates, best day, trends, all from the above.

These live as **pure functions** in `mobile/src/lib/*` and are mirrored in Swift so
both platforms compute identical numbers.
