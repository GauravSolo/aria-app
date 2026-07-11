import { create } from 'zustand';

import { isSupabaseConfigured } from '@/lib/supabase';
import { useAuth } from '@/stores/auth';

/**
 * The sync engine coordinates all registered collections. Each collection knows
 * how to load itself from disk and push/pull against Supabase. Sync only runs for
 * real accounts (guests stay local). It's best-effort: offline failures are
 * swallowed, dirty rows persist, and we retry on next schedule / app foreground.
 */

export type SyncState = 'idle' | 'syncing' | 'synced' | 'offline' | 'error';

interface SyncStatusStore {
  state: SyncState;
  lastSyncedAt: string | null;
  set: (state: SyncState, lastSyncedAt?: string) => void;
}

export const useSyncStatus = create<SyncStatusStore>((set) => ({
  state: 'idle',
  lastSyncedAt: null,
  set: (state, lastSyncedAt) =>
    set((prev) => ({ state, lastSyncedAt: lastSyncedAt ?? prev.lastSyncedAt })),
}));

export interface SyncableCollection {
  load: () => Promise<void>;
  push: () => Promise<void>;
  pull: () => Promise<void>;
}

const registry: SyncableCollection[] = [];

export function registerCollection(collection: SyncableCollection): void {
  registry.push(collection);
}

/** Hydrate every collection from local storage (call once after sign-in). */
export async function loadAllCollections(): Promise<void> {
  await Promise.all(registry.map((c) => c.load()));
}

let running = false;

export async function syncAll(): Promise<void> {
  if (!isSupabaseConfigured) return;
  if (!useAuth.getState().user) return; // guests don't sync
  if (running) return;

  running = true;
  useSyncStatus.getState().set('syncing');
  try {
    for (const c of registry) await c.push();
    for (const c of registry) await c.pull();
    useSyncStatus.getState().set('synced', new Date().toISOString());
  } catch {
    // Most likely offline or transient — keep dirty rows, surface a soft status.
    useSyncStatus.getState().set('offline');
  } finally {
    running = false;
  }
}

let timer: ReturnType<typeof setTimeout> | undefined;

/** Debounced sync, called after local mutations. */
export function scheduleSync(delayMs = 1500): void {
  if (!isSupabaseConfigured || !useAuth.getState().user) return;
  if (timer) clearTimeout(timer);
  timer = setTimeout(() => {
    void syncAll();
  }, delayMs);
}
