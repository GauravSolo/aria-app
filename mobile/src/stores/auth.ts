import AsyncStorage from '@react-native-async-storage/async-storage';
import type { Session, User } from '@supabase/supabase-js';
import { create } from 'zustand';

import { uuid } from '@/lib/id';
import { isSupabaseConfigured, supabase } from '@/lib/supabase';

type AuthStatus = 'loading' | 'signedIn' | 'signedOut';

const GUEST_FLAG_KEY = 'aria.guest';
const GUEST_ID_KEY = 'aria.guestId';

interface AuthState {
  status: AuthStatus;
  session: Session | null;
  user: User | null;
  /** Local-only mode: app works fully offline, sync disabled until a real sign-in. */
  isGuest: boolean;
  guestId: string | null;
  busy: boolean;
  error: string | null;

  init: () => Promise<void>;
  signIn: (email: string, password: string) => Promise<boolean>;
  signUp: (email: string, password: string, displayName?: string) => Promise<boolean>;
  continueAsGuest: () => Promise<void>;
  signOut: () => Promise<void>;
  clearError: () => void;
}

let subscribed = false;

export const useAuth = create<AuthState>((set) => ({
  status: 'loading',
  session: null,
  user: null,
  isGuest: false,
  guestId: null,
  busy: false,
  error: null,

  init: async () => {
    // 1) A real Supabase session always wins.
    if (isSupabaseConfigured) {
      const { data } = await supabase.auth.getSession();
      if (data.session) {
        set({
          session: data.session,
          user: data.session.user,
          isGuest: false,
          status: 'signedIn',
        });
      }
      if (!subscribed) {
        subscribed = true;
        supabase.auth.onAuthStateChange((_event, session) => {
          if (session) {
            set({ session, user: session.user, isGuest: false, status: 'signedIn' });
          } else {
            set({ session: null, user: null, status: 'signedOut' });
          }
        });
      }
      if (data.session) return;
    }

    // 2) Otherwise restore a previously chosen guest (local-only) session.
    const guestFlag = await AsyncStorage.getItem(GUEST_FLAG_KEY);
    if (guestFlag === '1') {
      let gid = await AsyncStorage.getItem(GUEST_ID_KEY);
      if (!gid) {
        gid = uuid();
        await AsyncStorage.setItem(GUEST_ID_KEY, gid);
      }
      set({ isGuest: true, guestId: gid, status: 'signedIn' });
      return;
    }

    set({ status: 'signedOut' });
  },

  signIn: async (email, password) => {
    if (!isSupabaseConfigured) {
      set({ error: 'Add your Supabase keys to enable accounts (see mobile/.env.example).' });
      return false;
    }
    set({ busy: true, error: null });
    const { error } = await supabase.auth.signInWithPassword({ email: email.trim(), password });
    set({ busy: false, error: error?.message ?? null });
    return !error;
  },

  signUp: async (email, password, displayName) => {
    if (!isSupabaseConfigured) {
      set({ error: 'Add your Supabase keys to enable accounts (see mobile/.env.example).' });
      return false;
    }
    set({ busy: true, error: null });
    const { error } = await supabase.auth.signUp({
      email: email.trim(),
      password,
      options: { data: displayName ? { display_name: displayName.trim() } : undefined },
    });
    set({ busy: false, error: error?.message ?? null });
    return !error;
  },

  continueAsGuest: async () => {
    let gid = await AsyncStorage.getItem(GUEST_ID_KEY);
    if (!gid) {
      gid = uuid();
      await AsyncStorage.setItem(GUEST_ID_KEY, gid);
    }
    await AsyncStorage.setItem(GUEST_FLAG_KEY, '1');
    set({ isGuest: true, guestId: gid, status: 'signedIn', error: null });
  },

  signOut: async () => {
    set({ busy: true });
    if (isSupabaseConfigured) await supabase.auth.signOut();
    await AsyncStorage.removeItem(GUEST_FLAG_KEY);
    set({
      busy: false,
      session: null,
      user: null,
      isGuest: false,
      status: 'signedOut',
    });
  },

  clearError: () => set({ error: null }),
}));

/**
 * The id that owns all local rows: the Supabase user id when signed in, otherwise
 * the stable guest id. `null` only while signed out. Use in stores/sync.
 */
export function activeUserId(): string | null {
  const s = useAuth.getState();
  if (s.user) return s.user.id;
  if (s.isGuest && s.guestId) return s.guestId;
  return null;
}
