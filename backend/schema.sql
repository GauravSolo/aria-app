-- ============================================================================
-- Aria — Supabase schema
-- ----------------------------------------------------------------------------
-- Run this in the Supabase SQL editor (or `supabase db push`). It is re-runnable:
-- enums use DO-guards, tables use IF NOT EXISTS, policies are dropped & recreated.
--
-- Mirrors docs/DATA_MODEL.md (authoritative). Sync model: every table carries
-- user_id, created_at, updated_at, deleted_at (soft delete). Conflict resolution
-- is last-write-wins by updated_at. The CLIENT sets updated_at (so we deliberately
-- do NOT force it via trigger — that would break last-write-wins on pull).
-- ============================================================================

create extension if not exists pgcrypto;  -- gen_random_uuid()

-- ── Enums ───────────────────────────────────────────────────────────────────
do $$ begin
  create type category as enum ('work','study','health','personal','other');
exception when duplicate_object then null; end $$;

do $$ begin
  create type priority as enum ('low','medium','high');
exception when duplicate_object then null; end $$;

do $$ begin
  create type task_recurrence as enum ('none','daily','weekly','monthly','custom');
exception when duplicate_object then null; end $$;

do $$ begin
  create type habit_kind as enum ('build','quit');
exception when duplicate_object then null; end $$;

do $$ begin
  create type frequency as enum ('daily','weekly','custom');
exception when duplicate_object then null; end $$;

do $$ begin
  create type reminder_kind as enum ('task','habit','water','custom');
exception when duplicate_object then null; end $$;

do $$ begin
  create type reminder_repeat as enum ('once','daily','weekly','interval');
exception when duplicate_object then null; end $$;

do $$ begin
  create type notif_status as enum ('delivered','done','snoozed','dismissed');
exception when duplicate_object then null; end $$;

-- ── profiles ─────────────────────────────────────────────────────────────────
create table if not exists profiles (
  id           uuid primary key references auth.users(id) on delete cascade,
  display_name text,
  theme        text not null default 'system',   -- system | light | dark
  created_at   timestamptz not null default now(),
  updated_at   timestamptz not null default now()
);

-- ── tasks ────────────────────────────────────────────────────────────────────
create table if not exists tasks (
  id                  uuid primary key default gen_random_uuid(),
  user_id             uuid not null references auth.users(id) on delete cascade,
  title               text not null,
  description         text,
  category            category not null default 'other',
  priority            priority not null default 'medium',
  start_time          timestamptz,
  end_time            timestamptz,
  due_date            date,
  recurrence          task_recurrence not null default 'none',
  recurrence_interval int  not null default 1,
  recurrence_days     int[] not null default '{}',
  recurrence_end_date date,
  is_completed        boolean not null default false,
  completed_at        timestamptz,
  sort_order          int not null default 0,
  created_at          timestamptz not null default now(),
  updated_at          timestamptz not null default now(),
  deleted_at          timestamptz
);

-- ── task_completions (per-occurrence completion for recurring tasks) ──────────
create table if not exists task_completions (
  id              uuid primary key default gen_random_uuid(),
  user_id         uuid not null references auth.users(id) on delete cascade,
  task_id         uuid not null references tasks(id) on delete cascade,
  occurrence_date date not null,
  completed_at    timestamptz not null default now(),
  created_at      timestamptz not null default now(),
  updated_at      timestamptz not null default now(),
  deleted_at      timestamptz,
  unique (task_id, occurrence_date)
);

-- ── habits ───────────────────────────────────────────────────────────────────
create table if not exists habits (
  id            uuid primary key default gen_random_uuid(),
  user_id       uuid not null references auth.users(id) on delete cascade,
  name          text not null,
  kind          habit_kind not null default 'build',
  category      category not null default 'health',
  frequency     frequency not null default 'daily',
  target_count  int not null default 1,
  custom_days   int[] not null default '{}',
  reminder_time time,
  start_date    date not null default current_date,
  notes         text,
  color         text,
  icon          text,
  is_archived   boolean not null default false,
  sort_order    int not null default 0,
  created_at    timestamptz not null default now(),
  updated_at    timestamptz not null default now(),
  deleted_at    timestamptz
);

-- ── habit_logs ───────────────────────────────────────────────────────────────
create table if not exists habit_logs (
  id         uuid primary key default gen_random_uuid(),
  user_id    uuid not null references auth.users(id) on delete cascade,
  habit_id   uuid not null references habits(id) on delete cascade,
  log_date   date not null,
  count      int not null default 1,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(),
  deleted_at timestamptz,
  unique (habit_id, log_date)
);

-- ── water_settings (one row per user) ─────────────────────────────────────────
create table if not exists water_settings (
  user_id               uuid primary key references auth.users(id) on delete cascade,
  daily_goal_ml         int  not null default 4000,
  glass_size_ml         int  not null default 250,
  reminder_interval_min int  not null default 45,
  reminder_enabled      boolean not null default true,
  active_start          time not null default '08:00',
  active_end            time not null default '22:00',
  updated_at            timestamptz not null default now()
);

-- ── water_logs ───────────────────────────────────────────────────────────────
create table if not exists water_logs (
  id         uuid primary key default gen_random_uuid(),
  user_id    uuid not null references auth.users(id) on delete cascade,
  log_date   date not null,
  amount_ml  int not null,
  logged_at  timestamptz not null default now(),
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(),
  deleted_at timestamptz
);

-- ── reminders ────────────────────────────────────────────────────────────────
create table if not exists reminders (
  id                    uuid primary key default gen_random_uuid(),
  user_id               uuid not null references auth.users(id) on delete cascade,
  title                 text not null,
  body                  text,
  kind                  reminder_kind not null default 'custom',
  ref_id                uuid,
  repeat                reminder_repeat not null default 'once',
  repeat_days           int[] not null default '{}',
  interval_min          int,
  time_of_day           time,
  next_trigger_at       timestamptz,
  is_enabled            boolean not null default true,
  snooze_until          timestamptz,
  local_notification_id text,
  created_at            timestamptz not null default now(),
  updated_at            timestamptz not null default now(),
  deleted_at            timestamptz
);

-- ── notification_history ──────────────────────────────────────────────────────
create table if not exists notification_history (
  id          uuid primary key default gen_random_uuid(),
  user_id     uuid not null references auth.users(id) on delete cascade,
  reminder_id uuid references reminders(id) on delete set null,
  title       text not null,
  body        text,
  kind        reminder_kind not null default 'custom',
  fired_at    timestamptz not null default now(),
  status      notif_status not null default 'delivered',
  created_at  timestamptz not null default now(),
  updated_at  timestamptz not null default now(),
  deleted_at  timestamptz
);

-- ── Indexes (sync pulls filter by user_id + updated_at; UI by date) ───────────
create index if not exists idx_tasks_user_updated         on tasks(user_id, updated_at);
create index if not exists idx_tasks_user_due             on tasks(user_id, due_date);
create index if not exists idx_taskcompl_user_updated     on task_completions(user_id, updated_at);
create index if not exists idx_habits_user_updated        on habits(user_id, updated_at);
create index if not exists idx_habitlogs_user_updated     on habit_logs(user_id, updated_at);
create index if not exists idx_habitlogs_habit_date       on habit_logs(habit_id, log_date);
create index if not exists idx_waterlogs_user_updated     on water_logs(user_id, updated_at);
create index if not exists idx_waterlogs_user_date        on water_logs(user_id, log_date);
create index if not exists idx_reminders_user_updated     on reminders(user_id, updated_at);
create index if not exists idx_notifhist_user_updated     on notification_history(user_id, updated_at);

-- ── Row Level Security: each user sees only their own rows ────────────────────
alter table profiles             enable row level security;
alter table tasks                enable row level security;
alter table task_completions     enable row level security;
alter table habits               enable row level security;
alter table habit_logs           enable row level security;
alter table water_settings       enable row level security;
alter table water_logs           enable row level security;
alter table reminders            enable row level security;
alter table notification_history enable row level security;

-- profiles keyed by id (= auth.uid())
drop policy if exists "profiles_owner" on profiles;
create policy "profiles_owner" on profiles
  for all using (auth.uid() = id) with check (auth.uid() = id);

-- generic owner policy macro, applied per table on user_id
do $$
declare t text;
begin
  foreach t in array array[
    'tasks','task_completions','habits','habit_logs',
    'water_settings','water_logs','reminders','notification_history'
  ] loop
    execute format('drop policy if exists "%1$s_owner" on %1$s;', t);
    execute format(
      'create policy "%1$s_owner" on %1$s for all
         using (auth.uid() = user_id) with check (auth.uid() = user_id);', t);
  end loop;
end $$;

-- ── New-user bootstrap: create profile + default water settings on signup ─────
create or replace function handle_new_user()
returns trigger
language plpgsql
security definer set search_path = public
as $$
begin
  insert into public.profiles (id, display_name)
  values (new.id, coalesce(new.raw_user_meta_data->>'display_name', split_part(new.email,'@',1)))
  on conflict (id) do nothing;

  insert into public.water_settings (user_id)
  values (new.id)
  on conflict (user_id) do nothing;

  return new;
end $$;

drop trigger if exists on_auth_user_created on auth.users;
create trigger on_auth_user_created
  after insert on auth.users
  for each row execute function handle_new_user();

-- ── Realtime (optional): publish tables so clients can subscribe to changes ───
do $$
declare t text;
begin
  foreach t in array array[
    'tasks','task_completions','habits','habit_logs',
    'water_settings','water_logs','reminders','notification_history','profiles'
  ] loop
    begin
      execute format('alter publication supabase_realtime add table %s;', t);
    exception when duplicate_object then null;  -- already in publication
    end;
  end loop;
end $$;
