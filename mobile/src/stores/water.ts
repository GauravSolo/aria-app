import { create } from 'zustand';

import { todayKey } from '@/lib/date';
import { nowIso } from '@/lib/id';
import { getJSON, setJSON } from '@/lib/storage';
import { supabase } from '@/lib/supabase';
import { createCollection, registerCollection, scheduleSync } from '@/sync';
import { activeUserId, useAuth } from '@/stores/auth';
import type { WaterLog, WaterSettings } from '@/types/db';

export const useWaterLogs = createCollection<WaterLog>('water_logs');

// ── Water settings (per-user singleton; bespoke store, not the collection factory) ──
const SETTINGS_KEY = 'aria.water_settings';

export const WATER_DEFAULTS = {
  daily_goal_ml: 4000,
  glass_size_ml: 250,
  reminder_interval_min: 45,
  reminder_enabled: true,
  active_start: '08:00',
  active_end: '22:00',
};

function defaultsFor(uid: string): WaterSettings {
  return { user_id: uid, ...WATER_DEFAULTS, updated_at: nowIso() };
}

interface WaterSettingsState {
  byUser: Record<string, WaterSettings>;
  loaded: boolean;
  load: () => Promise<void>;
  current: () => WaterSettings;
  update: (patch: Partial<Omit<WaterSettings, 'user_id' | 'updated_at'>>) => void;
  push: () => Promise<void>;
  pull: () => Promise<void>;
}

export const useWaterSettings = create<WaterSettingsState>((set, get) => ({
  byUser: {},
  loaded: false,

  load: async () => {
    const byUser = await getJSON<Record<string, WaterSettings>>(SETTINGS_KEY, {});
    set({ byUser, loaded: true });
  },

  current: () => {
    const uid = activeUserId() ?? '';
    return get().byUser[uid] ?? defaultsFor(uid);
  },

  update: (patch) => {
    const uid = activeUserId() ?? '';
    const cur = get().byUser[uid] ?? defaultsFor(uid);
    const next: WaterSettings = { ...cur, ...patch, user_id: uid, updated_at: nowIso() };
    const byUser = { ...get().byUser, [uid]: next };
    set({ byUser });
    void setJSON(SETTINGS_KEY, byUser);
    scheduleSync();
  },

  push: async () => {
    const user = useAuth.getState().user;
    if (!user) return;
    const s = get().byUser[user.id];
    if (!s) return;
    const { error } = await supabase.from('water_settings').upsert(s);
    if (error) throw error;
  },

  pull: async () => {
    const user = useAuth.getState().user;
    if (!user) return;
    const { data, error } = await supabase
      .from('water_settings')
      .select('*')
      .eq('user_id', user.id)
      .maybeSingle();
    if (error) throw error;
    if (!data) return;
    const remote = data as WaterSettings;
    const local = get().byUser[user.id];
    if (!local || remote.updated_at >= local.updated_at) {
      const byUser = { ...get().byUser, [user.id]: remote };
      set({ byUser });
      void setJSON(SETTINGS_KEY, byUser);
    }
  },
}));

registerCollection({
  load: useWaterSettings.getState().load,
  push: useWaterSettings.getState().push,
  pull: useWaterSettings.getState().pull,
});

// ── Logging helpers ──────────────────────────────────────────────────────────
export function logWater(amountMl: number, key: string = todayKey()): WaterLog {
  return useWaterLogs.getState().create({
    log_date: key,
    amount_ml: amountMl,
    logged_at: nowIso(),
  });
}

export function undoLastWater(key: string = todayKey()): void {
  const todays = useWaterLogs
    .getState()
    .list()
    .filter((l) => l.log_date === key)
    .sort((a, b) => a.logged_at.localeCompare(b.logged_at));
  const last = todays[todays.length - 1];
  if (last) useWaterLogs.getState().remove(last.id);
}
