import dayjs from 'dayjs';
import { router } from 'expo-router';
import { useMemo, useState } from 'react';
import { StyleSheet, Switch, View } from 'react-native';

import {
  Card,
  Chip,
  EmptyState,
  Fab,
  Icon,
  IconButton,
  Screen,
  Segmented,
  Text,
} from '@/components/ui';
import { reminderSummary } from '@/lib/reminders';
import { useAuth } from '@/stores/auth';
import {
  markReminderDone,
  snoozeReminder,
  toggleReminder,
  useNotificationHistory,
  useReminders,
} from '@/stores/reminders';
import { useTheme } from '@/theme';
import type { NotifStatus } from '@/types/db';

export default function RemindersScreen() {
  const { colors } = useTheme();
  const uid = useAuth((s) => s.user?.id ?? s.guestId ?? '');
  const reminderItems = useReminders((s) => s.items);
  const historyItems = useNotificationHistory((s) => s.items);
  const [tab, setTab] = useState<'reminders' | 'history'>('reminders');

  const reminders = useMemo(
    () =>
      Object.values(reminderItems)
        .filter((r) => !r.deleted_at && r.user_id === uid)
        .sort((a, b) => a.created_at.localeCompare(b.created_at)),
    [reminderItems, uid],
  );

  const history = useMemo(
    () =>
      Object.values(historyItems)
        .filter((h) => !h.deleted_at && h.user_id === uid)
        .sort((a, b) => b.fired_at.localeCompare(a.fired_at))
        .slice(0, 100),
    [historyItems, uid],
  );

  const statusColor: Record<NotifStatus, string> = {
    delivered: colors.info,
    done: colors.success,
    snoozed: colors.warning,
    dismissed: colors.textMuted,
  };

  return (
    <Screen padded={false}>
      <View style={styles.bar}>
        <IconButton name="chevron-back" onPress={() => router.back()} filled />
        <Text variant="title">Reminders</Text>
        <View style={{ width: 40 }} />
      </View>

      <View style={styles.pad}>
        <Segmented
          options={[
            { label: 'Reminders', value: 'reminders' },
            { label: 'History', value: 'history' },
          ]}
          value={tab}
          onChange={(v) => setTab(v as typeof tab)}
        />
      </View>

      {tab === 'reminders' ? (
        reminders.length === 0 ? (
          <EmptyState
            icon="notifications-outline"
            title="No reminders yet"
            message="Add one-time, daily, weekly or interval reminders. They fire on your device, even offline."
          />
        ) : (
          <Screen scroll padded contentStyle={{ paddingTop: 12 }}>
            {reminders.map((r) => (
              <Card key={r.id} style={styles.reminderCard}>
                <View style={{ flex: 1, gap: 4 }}>
                  <Text variant="bodyStrong" numberOfLines={1} style={!r.is_enabled ? { color: colors.textMuted } : undefined}>
                    {r.title}
                  </Text>
                  <View style={styles.metaRow}>
                    <Icon name="repeat-outline" size={13} color={colors.textMuted} />
                    <Text variant="footnote" tone="secondary">
                      {reminderSummary(r)}
                    </Text>
                  </View>
                  <View style={styles.actions}>
                    <IconButton name="create-outline" size={18} onPress={() => router.push({ pathname: '/reminder-form', params: { id: r.id } })} />
                    <IconButton name="time-outline" size={18} onPress={() => snoozeReminder(r.id, 10)} />
                    <IconButton name="checkmark-circle-outline" size={18} color={colors.success} onPress={() => markReminderDone(r.id)} />
                  </View>
                </View>
                <Switch
                  value={r.is_enabled}
                  onValueChange={() => toggleReminder(r.id)}
                  trackColor={{ false: colors.track, true: colors.primary }}
                  thumbColor="#fff"
                />
              </Card>
            ))}
          </Screen>
        )
      ) : history.length === 0 ? (
        <EmptyState
          icon="time-outline"
          title="No notification history"
          message="Reminders you receive will be listed here."
        />
      ) : (
        <Screen scroll padded contentStyle={{ paddingTop: 12 }}>
          {history.map((h) => (
            <Card key={h.id} style={styles.historyCard}>
              <View style={{ flex: 1, gap: 2 }}>
                <Text variant="subhead">{h.title}</Text>
                <Text variant="caption" tone="muted">
                  {dayjs(h.fired_at).format('MMM D · h:mm A')}
                </Text>
              </View>
              <Chip label={h.status} color={statusColor[h.status]} />
            </Card>
          ))}
        </Screen>
      )}

      {tab === 'reminders' ? <Fab onPress={() => router.push('/reminder-form')} /> : null}
    </Screen>
  );
}

const styles = StyleSheet.create({
  bar: { flexDirection: 'row', alignItems: 'center', justifyContent: 'space-between', paddingHorizontal: 16, paddingTop: 8 },
  pad: { paddingHorizontal: 16, paddingTop: 8 },
  reminderCard: { flexDirection: 'row', alignItems: 'center', gap: 12 },
  metaRow: { flexDirection: 'row', alignItems: 'center', gap: 5 },
  actions: { flexDirection: 'row', gap: 4, marginTop: 4, marginLeft: -8 },
  historyCard: { flexDirection: 'row', alignItems: 'center', justifyContent: 'space-between', gap: 12 },
});
