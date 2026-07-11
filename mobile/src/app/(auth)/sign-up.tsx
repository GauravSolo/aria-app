import { router } from 'expo-router';
import { useState } from 'react';
import { Alert, KeyboardAvoidingView, Platform, Pressable, StyleSheet, View } from 'react-native';

import { BrandMark, Button, Card, Screen, Text, TextField } from '@/components/ui';
import { useAuth } from '@/stores/auth';
import { useTheme } from '@/theme';

export default function SignUpScreen() {
  const { spacing } = useTheme();
  const { signUp, busy, error, clearError } = useAuth();
  const [name, setName] = useState('');
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [confirm, setConfirm] = useState('');
  const [localError, setLocalError] = useState<string | null>(null);

  const onSubmit = async () => {
    clearError();
    setLocalError(null);
    if (password.length < 6) {
      setLocalError('Password must be at least 6 characters.');
      return;
    }
    if (password !== confirm) {
      setLocalError('Passwords do not match.');
      return;
    }
    const ok = await signUp(email, password, name);
    if (ok) {
      // If email confirmation is on, no session is created yet.
      if (!useAuth.getState().session) {
        Alert.alert(
          'Almost there',
          'Check your email to confirm your account, then log in.',
          [{ text: 'OK', onPress: () => router.replace('/sign-in') }],
        );
      }
      // Otherwise the auth gate redirects to the app automatically.
    }
  };

  return (
    <Screen scroll padded>
      <KeyboardAvoidingView behavior={Platform.OS === 'ios' ? 'padding' : undefined}>
        <View style={[styles.brand, { marginTop: spacing.xxxl }]}>
          <BrandMark size={56} />
          <View style={{ alignItems: 'center', gap: 4 }}>
            <Text variant="title">Create your account</Text>
            <Text variant="callout" tone="secondary">
              One account, synced on every device
            </Text>
          </View>
        </View>

        <Card style={{ gap: 16, marginTop: spacing.xl }}>
          <TextField
            label="Name"
            icon="person-outline"
            value={name}
            onChangeText={setName}
            placeholder="Your name"
            autoCapitalize="words"
          />
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
            placeholder="At least 6 characters"
            secureTextEntry
            autoCapitalize="none"
          />
          <TextField
            label="Confirm password"
            icon="lock-closed-outline"
            value={confirm}
            onChangeText={setConfirm}
            placeholder="Re-enter password"
            secureTextEntry
            autoCapitalize="none"
          />
          {localError || error ? (
            <Text variant="footnote" tone="danger">
              {localError ?? error}
            </Text>
          ) : null}
          <Button title="Sign up" onPress={onSubmit} loading={busy} fullWidth />
        </Card>

        <View style={styles.footerRow}>
          <Text variant="subhead" tone="secondary">
            Already have an account?
          </Text>
          <Pressable hitSlop={8} onPress={() => router.replace('/sign-in')}>
            <Text variant="subhead" tone="primary">
              Log in
            </Text>
          </Pressable>
        </View>
      </KeyboardAvoidingView>
    </Screen>
  );
}

const styles = StyleSheet.create({
  brand: { alignItems: 'center', gap: 16, marginBottom: 8 },
  footerRow: { flexDirection: 'row', justifyContent: 'center', gap: 6, marginTop: 18 },
});
