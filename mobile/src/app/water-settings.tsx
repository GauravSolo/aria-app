import dayjs from 'dayjs';
import { router } from 'expo-router';
import { StyleSheet, Switch, View } from 'react-native';

import { Button, Card, DateTimeField, Screen, Stepper, Text } from '@/components/ui';
import { formatMl } from '@/lib/water';
import { useWaterSettings } from '@/stores/water';
import { useTheme } from '@/theme';

function toDate(hhmm: string): Date {
  return dayjs(`2000-01-01T${hhmm}`).toDate();
}

export default function WaterSettingsScreen() {
  const { colors } = useTheme();
  useWaterSettings((s) => s.byUser); // re-render on change
  const s = useWaterSettings.getState().current();
  const update = useWaterSettings((st) => st.update);

  return (
    <Screen scroll edges={['top', 'left', 'right', 'bottom']}>
      <View style={styles.bar}>
        <Text variant="title">Water settings</Text>
        <Button title="Done" size="sm" onPress={() => router.back()} />
      </View>

      <Card style={styles.row}>
        <View style={styles.label}>
          <Text variant="bodyStrong">Daily goal</Text>
          <Text variant="footnote" tone="secondary">
            {formatMl(s.daily_goal_ml)}
          </Text>
        </View>
        <Stepper
          value={s.daily_goal_ml}
          onChange={(v) => update({ daily_goal_ml: v })}
          min={500}
          max={8000}
          step={250}
        />
      </Card>

      <Card style={styles.row}>
        <View style={styles.label}>
          <Text variant="bodyStrong">Glass size</Text>
          <Text variant="footnote" tone="secondary">
            One-tap add amount
          </Text>
        </View>
        <Stepper
          value={s.glass_size_ml}
          onChange={(v) => update({ glass_size_ml: v })}
          min={50}
          max={1000}
          step={50}
        />
      </Card>

      <Text variant="caption" tone="muted" style={styles.sectionLabel}>
        REMINDERS
      </Text>

      <Card style={styles.row}>
        <View style={styles.label}>
          <Text variant="bodyStrong">Remind me to drink</Text>
          <Text variant="footnote" tone="secondary">
            Schedules repeating notifications
          </Text>
        </View>
        <Switch
          value={s.reminder_enabled}
          onValueChange={(v) => update({ reminder_enabled: v })}
          trackColor={{ false: colors.track, true: colors.primary }}
          thumbColor="#fff"
        />
      </Card>

      {s.reminder_enabled ? (
        <>
          <Card style={styles.row}>
            <View style={styles.label}>
              <Text variant="bodyStrong">Every</Text>
              <Text variant="footnote" tone="secondary">
                Reminder interval
              </Text>
            </View>
            <Stepper
              value={s.reminder_interval_min}
              onChange={(v) => update({ reminder_interval_min: v })}
              min={15}
              max={240}
              step={15}
              suffix="min"
            />
          </Card>

          <Card style={{ gap: 12 }}>
            <Text variant="bodyStrong">Active hours</Text>
            <View style={styles.times}>
              <View style={styles.flex}>
                <DateTimeField
                  label="From"
                  mode="time"
                  value={toDate(s.active_start)}
                  onChange={(d) => d && update({ active_start: dayjs(d).format('HH:mm') })}
                />
              </View>
              <View style={styles.flex}>
                <DateTimeField
                  label="Until"
                  mode="time"
                  value={toDate(s.active_end)}
                  onChange={(d) => d && update({ active_end: dayjs(d).format('HH:mm') })}
                />
              </View>
            </View>
          </Card>
        </>
      ) : null}

      <Text variant="footnote" tone="muted" center style={{ marginTop: 8 }}>
        Reminders fire on your device even offline. Manage all reminders in the
        Reminders area.
      </Text>
    </Screen>
  );
}

const styles = StyleSheet.create({
  bar: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    paddingTop: 8,
  },
  row: { flexDirection: 'row', alignItems: 'center', justifyContent: 'space-between', gap: 12 },
  label: { flex: 1, gap: 2 },
  sectionLabel: { marginLeft: 4, letterSpacing: 1, marginTop: 4 },
  times: { flexDirection: 'row', gap: 12 },
  flex: { flex: 1 },
});
