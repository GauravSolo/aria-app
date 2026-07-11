import Constants from 'expo-constants';
import { router } from 'expo-router';
import { Alert, StyleSheet, View } from 'react-native';

import {
  Button,
  Card,
  Icon,
  IconButton,
  Screen,
  Segmented,
  Text,
  type SegmentOption,
} from '@/components/ui';
import { useAuth } from '@/stores/auth';
import { useTheme, type ThemeMode } from '@/theme';

const THEME_OPTIONS: SegmentOption<ThemeMode>[] = [
  { label: 'System', value: 'system', icon: 'phone-portrait-outline' },
  { label: 'Light', value: 'light', icon: 'sunny-outline' },
  { label: 'Dark', value: 'dark', icon: 'moon-outline' },
];

export default function SettingsScreen() {
  const { colors, mode, setMode } = useTheme();
  const { user, isGuest, signOut } = useAuth();

  const confirmSignOut = () => {
    Alert.alert('Sign out?', 'You can sign back in anytime.', [
      { text: 'Cancel', style: 'cancel' },
      { text: 'Sign out', style: 'destructive', onPress: () => signOut() },
    ]);
  };

  return (
    <Screen scroll edges={['top', 'left', 'right', 'bottom']}>
      <View style={styles.titleRow}>
        <Text variant="title">Settings</Text>
        <IconButton name="close" onPress={() => router.back()} filled />
      </View>

      {/* Appearance */}
      <View style={styles.section}>
        <Text variant="caption" tone="muted" style={styles.sectionLabel}>
          APPEARANCE
        </Text>
        <Card>
          <Text variant="bodyStrong">Theme</Text>
          <Text variant="footnote" tone="secondary" style={{ marginBottom: 12 }}>
            Choose how Aria looks. “System” follows your device.
          </Text>
          <Segmented options={THEME_OPTIONS} value={mode} onChange={setMode} />
        </Card>
      </View>

      {/* Reminders */}
      <View style={styles.section}>
        <Text variant="caption" tone="muted" style={styles.sectionLabel}>
          NOTIFICATIONS
        </Text>
        <Card
          onPress={() => {
            router.back();
            router.push('/reminders');
          }}>
          <View style={styles.linkRow}>
            <View style={styles.linkLeft}>
              <View style={[styles.avatar, { backgroundColor: colors.primarySoft }]}>
                <Icon name="notifications-outline" color={colors.primary} />
              </View>
              <View>
                <Text variant="bodyStrong">Reminders</Text>
                <Text variant="footnote" tone="secondary">
                  Manage reminders &amp; notification history
                </Text>
              </View>
            </View>
            <Icon name="chevron-forward" color={colors.textMuted} />
          </View>
        </Card>
      </View>

      {/* Account */}
      <View style={styles.section}>
        <Text variant="caption" tone="muted" style={styles.sectionLabel}>
          ACCOUNT
        </Text>
        <Card>
          <View style={styles.accountRow}>
            <View
              style={[styles.avatar, { backgroundColor: colors.primarySoft }]}>
              <Icon name={isGuest ? 'cloud-offline-outline' : 'person'} color={colors.primary} />
            </View>
            <View style={{ flex: 1 }}>
              <Text variant="bodyStrong">{isGuest ? 'Local mode' : user?.email ?? 'Signed in'}</Text>
              <Text variant="footnote" tone="secondary">
                {isGuest ? 'Data stays on this device' : 'Synced across your devices'}
              </Text>
            </View>
          </View>

          {isGuest ? (
            <Button
              title="Sign in to sync"
              icon="cloud-upload-outline"
              variant="secondary"
              fullWidth
              style={{ marginTop: 14 }}
              onPress={signOut /* returns to auth screen where they can sign in */}
            />
          ) : (
            <Button
              title="Sign out"
              icon="log-out-outline"
              variant="ghost"
              fullWidth
              style={{ marginTop: 14 }}
              onPress={confirmSignOut}
            />
          )}
        </Card>
      </View>

      {/* About */}
      <View style={styles.section}>
        <Text variant="caption" tone="muted" style={styles.sectionLabel}>
          ABOUT
        </Text>
        <Card>
          <View style={styles.aboutRow}>
            <Text variant="body">Aria</Text>
            <Text variant="body" tone="secondary">
              v{Constants.expoConfig?.version ?? '1.0.0'}
            </Text>
          </View>
        </Card>
      </View>
    </Screen>
  );
}

const styles = StyleSheet.create({
  titleRow: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    paddingTop: 8,
  },
  section: { gap: 8 },
  sectionLabel: { marginLeft: 4, letterSpacing: 1 },
  accountRow: { flexDirection: 'row', alignItems: 'center', gap: 14 },
  linkRow: { flexDirection: 'row', alignItems: 'center', justifyContent: 'space-between' },
  linkLeft: { flexDirection: 'row', alignItems: 'center', gap: 14 },
  avatar: { width: 44, height: 44, borderRadius: 22, alignItems: 'center', justifyContent: 'center' },
  aboutRow: { flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center' },
});
