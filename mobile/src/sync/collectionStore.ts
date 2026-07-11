import { create, type StoreApi, type UseBoundStore } from 'zustand';

import { nowIso, uuid } from '@/lib/id';
import { getJSON, setJSON } from '@/lib/storage';
import { supabase } from '@/lib/supabase';
import { activeUserId, useAuth } from '@/stores/auth';
import { registerCollection, scheduleSync } from './engine';

/** Every synced row carries these. */
export interface BaseRow {
  id: string;
  user_id: string;
  created_at: string;
  updated_at: string;
  deleted_at: string | null;
}

interface PersistShape<T> {
  items: Record<string, T>;
  dirty: string[];
  lastPulledAt: string | null;
}

export interface CollectionApi<T extends BaseRow> {
  items: Record<string, T>;
  dirty: Record<string, true>;
  lastPulledAt: string | null;
  loaded: boolean;

  load: () => Promise<void>;
  /** Live rows for the current user (excludes soft-deleted). */
  list: () => T[];
  getById: (id: string) => T | undefined;
  create: (data: Omit<T, keyof BaseRow>) => T;
  /** Patch domain fields (and `deleted_at` to revive a tombstone). */
  update: (id: string, patch: Partial<Omit<T, 'id' | 'user_id' | 'created_at'>>) => T | undefined;
  remove: (id: string) => void;

  push: () => Promise<void>;
  pull: () => Promise<void>;
}

/**
 * Creates an offline-first Zustand store for one Supabase table. Writes go to
 * memory + AsyncStorage immediately and mark the row dirty; the sync engine later
 * pushes dirty rows and pulls newer ones (last-write-wins by `updated_at`).
 */
export function createCollection<T extends BaseRow>(
  table: string,
): UseBoundStore<StoreApi<CollectionApi<T>>> {
  const KEY = `aria.col.${table}`;

  const store = create<CollectionApi<T>>((set, get) => {
    const persist = () => {
      const s = get();
      void setJSON(KEY, {
        items: s.items,
        dirty: Object.keys(s.dirty),
        lastPulledAt: s.lastPulledAt,
      } satisfies PersistShape<T>);
    };

    return {
      items: {},
      dirty: {},
      lastPulledAt: null,
      loaded: false,

      load: async () => {
        const data = await getJSON<PersistShape<T>>(KEY, {
          items: {},
          dirty: [],
          lastPulledAt: null,
        });
        const dirty: Record<string, true> = {};
        data.dirty.forEach((id) => (dirty[id] = true));
        set({ items: data.items, dirty, lastPulledAt: data.lastPulledAt, loaded: true });
      },

      list: () => {
        const uid = activeUserId();
        return Object.values(get().items).filter((r) => !r.deleted_at && r.user_id === uid);
      },

      getById: (id) => get().items[id],

      create: (data) => {
        const now = nowIso();
        const row = {
          id: uuid(),
          user_id: activeUserId() ?? '',
          created_at: now,
          updated_at: now,
          deleted_at: null,
          ...data,
        } as T;
        set((s) => ({
          items: { ...s.items, [row.id]: row },
          dirty: { ...s.dirty, [row.id]: true },
        }));
        persist();
        scheduleSync();
        return row;
      },

      update: (id, patch) => {
        const cur = get().items[id];
        if (!cur) return undefined;
        const row = { ...cur, ...patch, updated_at: nowIso() } as T;
        set((s) => ({ items: { ...s.items, [id]: row }, dirty: { ...s.dirty, [id]: true } }));
        persist();
        scheduleSync();
        return row;
      },

      remove: (id) => {
        const cur = get().items[id];
        if (!cur) return;
        const now = nowIso();
        const row = { ...cur, deleted_at: now, updated_at: now } as T;
        set((s) => ({ items: { ...s.items, [id]: row }, dirty: { ...s.dirty, [id]: true } }));
        persist();
        scheduleSync();
      },

      push: async () => {
        const user = useAuth.getState().user;
        if (!user) return;
        const s = get();
        const rows = Object.keys(s.dirty)
          .map((id) => s.items[id])
          .filter((r): r is T => Boolean(r) && r.user_id === user.id);
        if (rows.length === 0) return;

        const { error } = await supabase.from(table).upsert(rows);
        if (error) throw error;

        set((st) => {
          const d = { ...st.dirty };
          rows.forEach((r) => delete d[r.id]);
          return { dirty: d };
        });
        persist();
      },

      pull: async () => {
        const user = useAuth.getState().user;
        if (!user) return;
        const s = get();
        let query = supabase.from(table).select('*').eq('user_id', user.id);
        if (s.lastPulledAt) query = query.gt('updated_at', s.lastPulledAt);
        const { data, error } = await query;
        if (error) throw error;
        if (!data || data.length === 0) return;

        set((st) => {
          const items = { ...st.items };
          let maxUpdated = st.lastPulledAt;
          for (const remote of data as T[]) {
            const local = items[remote.id];
            const localDirty = Boolean(st.dirty[remote.id]);
            const remoteWins = !local
              ? true
              : localDirty
                ? remote.updated_at > local.updated_at
                : remote.updated_at >= local.updated_at;
            if (remoteWins) items[remote.id] = remote;
            if (!maxUpdated || remote.updated_at > maxUpdated) maxUpdated = remote.updated_at;
          }
          return { items, lastPulledAt: maxUpdated };
        });
        persist();
      },
    };
  });

  // Register raw functions with the engine (zustand actions are stable references).
  const api = store.getState();
  registerCollection({ load: api.load, push: api.push, pull: api.pull });

  return store;
}
