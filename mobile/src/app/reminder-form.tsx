import dayjs from 'dayjs';
import { router, useLocalSearchParams } from 'expo-router';
import { useState } from 'react';
import { Alert, StyleSheet, View } from 'react-native';

import {
  Button,
  DateTimeField,
  Screen,
  Segmented,
  Stepper,
  Text,
  TextField,
  WeekdayPicker,
} from '@/components/ui';
import { addReminder, deleteReminder, updateReminder, useReminders, type ReminderInput } from '@/stores/reminders';
import type { ReminderRepeat } from '@/types/db';

export default function ReminderFormScreen() {
  const params = useLocalSearchParams<{ id?: string }>();
  const existing = params.id ? useReminders.getState().getById(params.id) : undefined;

  const [title, setTitle] = useState(existing?.title ?? '');
  const [body, setBody] = useState(existing?.body ?? '');
  const [repeat, setRepeat] = useState<ReminderRepeat>(existing?.repeat ?? 'once');
  const [date, setDate] = useState<Date>(
    existing?.next_trigger_at ? new Date(existing.next_trigger_at) : dayjs().add(1, 'hour').toDate(),
  );
  const [time, setTime] = useState<Date>(
    existing?.time_of_day
      ? dayjs(`2000-01-01T${existing.time_of_day}`).toDate()
      : existing?.next_trigger_at
        ? new Date(existing.next_trigger_at)
        : dayjs().add(1, 'hour').toDate(),
  );
  const [days, setDays] = useState<number[]>(existing?.repeat_days ?? []);
  const [interval, setIntervalMin] = useState(existing?.interval_min ?? 60);
  const [error, setError] = useState<string | null>(null);

  const onSave = () => {
    if (!title.trim()) {
      setError('Please enter a title.');
      return;
    }
    const hm = dayjs(time).format('HH:mm');
    const nextOnce = dayjs(date)
      .hour(time.getHours())
      .minute(time.getMinutes())
      .second(0)
      .toISOString();
    const input: ReminderInput = {
      title,
      body,
      kind: 'custom',
      repeat,
      time_of_day: repeat === 'daily' || repeat === 'weekly' ? hm : null,
      repeat_days: repeat === 'weekly' ? days : [],
      interval_min: repeat === 'interval' ? Math.max(15, interval) : null,
      next_trigger_at: repeat === 'once' ? nextOnce : null,
    };
    if (existing) updateReminder(existing.id, input);
    else addReminder(input);
    router.back();
  };

  const onDelete = () => {
    if (!existing) return;
    Alert.alert('Delete reminder?', undefined, [
      { text: 'Cancel', style: 'cancel' },
      {
        text: 'Delete',
        style: 'destructive',
        onPress: () => {
          deleteReminder(existing.id);
          router.back();
        },
      },
    ]);
  };

  return (
    <Screen scroll edges={['top', 'left', 'right', 'bottom']}>
      <View style={styles.bar}>
        <Button title="Cancel" variant="ghost" size="sm" onPress={() => router.back()} />
        <Text variant="headline">{existing ? 'Edit reminder' : 'New reminder'}</Text>
        <Button title="Save" size="sm" onPress={onSave} />
      </View>

      <TextField
        label="Title"
        value={title}
        onChangeText={setTitle}
        placeholder="What should I remind you about?"
        error={error}
      />
      <TextField
        label="Note (optional)"
        value={body}
        onChangeText={setBody}
        placeholder="Extra details"
      />

      <Field label="REPEAT">
        <Segmented
          options={[
            { label: 'Once', value: 'once' },
            { label: 'Daily', value: 'daily' },
            { label: 'Weekly', value: 'weekly' },
            { label: 'Interval', value: 'interval' },
          ]}
          value={repeat}
          onChange={setRepeat}
        />
      </Field>

      {repeat === 'once' ? (
        <Field label="WHEN">
          <View style={styles.row}>
            <View style={styles.flex}>
              <DateTimeField label="Date" mode="date" value={date} onChange={(d) => d && setDate(d)} />
            </View>
            <View style={styles.flex}>
              <DateTimeField label="Time" mode="time" value={time} onChange={(d) => d && setTime(d)} />
            </View>
          </View>
        </Field>
      ) : null}

      {repeat === 'daily' ? (
        <Field label="TIME">
          <DateTimeField label="Time" mode="time" value={time} onChange={(d) => d && setTime(d)} />
        </Field>
      ) : null}

      {repeat === 'weekly' ? (
        <Field label="DAYS & TIME">
          <View style={{ gap: 12 }}>
            <WeekdayPicker value={days} onChange={setDays} />
            <DateTimeField label="Time" mode="time" value={time} onChange={(d) => d && setTime(d)} />
          </View>
        </Field>
      ) : null}

      {repeat === 'interval' ? (
        <Field label="EVERY">
          <Stepper value={interval} onChange={setIntervalMin} min={15} max={480} step={15} suffix="min" />
        </Field>
      ) : null}

      {existing ? (
        <Button title="Delete reminder" variant="ghost" icon="trash-outline" onPress={onDelete} style={{ marginTop: 8 }} />
      ) : null}
      <View style={{ height: 12 }} />
    </Screen>
  );
}

function Field({ label, children }: { label: string; children: React.ReactNode }) {
  return (
    <View style={{ gap: 8 }}>
      <Text variant="caption" tone="muted" style={{ marginLeft: 2, letterSpacing: 1 }}>
        {label}
      </Text>
      {children}
    </View>
  );
}

const styles = StyleSheet.create({
  bar: { flexDirection: 'row', alignItems: 'center', justifyContent: 'space-between', marginBottom: 4 },
  row: { flexDirection: 'row', gap: 12 },
  flex: { flex: 1 },
});
