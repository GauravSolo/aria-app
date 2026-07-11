import { Link } from 'expo-router';
import { useState } from 'react';
import {
  KeyboardAvoidingView,
  Platform,
  Pressable,
  StyleSheet,
  View,
} from 'react-native';

import { BrandMark, Button, Card, Screen, Text, TextField } from '@/components/ui';
import { isSupabaseConfigured } from '@/lib/supabase';
import { useAuth } from '@/stores/auth';
import { useTheme } from '@/theme';

export default function SignInScreen() {
  const { colors, spacing } = useTheme();
  const { signIn, continueAsGuest, busy, error, clearError } = useAuth();
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');

  const onSubmit = () => {
    clearError();
    signIn(email, password);
  };

  return (
    <Screen scroll padded>
      <KeyboardAvoidingView behavior={Platform.OS === 'ios' ? 'padding' : undefined}>
        <View style={[styles.brand, { marginTop: spacing.huge }]}>
          <BrandMark size={64} />
          <View style={{ alignItems: 'center', gap: 4 }}>
            <Text variant="display">Welcome back</Text>
            <Text variant="callout" tone="secondary">
              Sign in to sync your day across devices
            </Text>
          </View>
        </View>

        {!isSupabaseConfigured ? (
          <Card style={{ backgroundColor: colors.warningSoft, borderColor: colors.warning }}>
            <Text variant="subhead" tone="warning">
              Supabase not connected
            </Text>
            <Text variant="footnote" tone="secondary" style={{ marginTop: 4 }}>
              Add your keys to mobile/.env to enable accounts &amp; sync. You can still
              use Aria fully offline with “Continue without an account” below.
            </Text>
          </Card>
        ) : null}

        <Card style={{ gap: 16, marginTop: spacing.xl }}>
          <TextField
            label="Email"
            icon="mail-outline"
            value={email}
            onChangeText={setEmail}
            placeholder="you@example.com"
            keyboardType="email-address"
            autoCapitalize="none"
            autoComplete="email"
          />
          <TextField
            label="Password"
            icon="lock-closed-outline"
            value={password}
            onChangeText={setPassword}
            placeholder="••••••••"
            secureTextEntry
            autoCapitalize="none"
          />
          {error ? (
            <Text variant="footnote" tone="danger">
              {error}
            </Text>
          ) : null}
          <Button title="Log in" onPress={onSubmit} loading={busy} fullWidth />
        </Card>

        <View style={styles.footerRow}>
          <Text variant="subhead" tone="secondary">
            New to Aria?
          </Text>
          <Link href="/sign-up" asChild>
            <Pressable hitSlop={8}>
              <Text variant="subhead" tone="primary">
                Create an account
              </Text>
            </Pressable>
          </Link>
        </View>

        <View style={styles.dividerRow}>
          <View style={[styles.line, { backgroundColor: colors.border }]} />
          <Text variant="caption" tone="muted">
            OR
          </Text>
          <View style={[styles.line, { backgroundColor: colors.border }]} />
        </View>

        <Button
          title="Continue without an account"
          variant="ghost"
          icon="cloud-offline-outline"
          onPress={continueAsGuest}
          fullWidth
        />
        <Text variant="footnote" tone="muted" center style={{ marginTop: 6 }}>
          Local-only mode — your data stays on this device until you sign in.
        </Text>
      </KeyboardAvoidingView>
    </Screen>
  );
}

const styles = StyleSheet.create({
  brand: { alignItems: 'center', gap: 18, marginBottom: 8 },
  footerRow: { flexDirection: 'row', justifyContent: 'center', gap: 6, marginTop: 18 },
  dividerRow: { flexDirection: 'row', alignItems: 'center', gap: 12, marginVertical: 22 },
  line: { flex: 1, height: 1 },
});
