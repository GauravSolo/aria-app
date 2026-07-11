import dayjs from 'dayjs';
import { router, useLocalSearchParams } from 'expo-router';
import { useMemo, useState } from 'react';
import { Alert, StyleSheet, View } from 'react-native';

import {
  Button,
  ChipSelect,
  DateTimeField,
  Screen,
  Segmented,
  Text,
  TextField,
  WeekdayPicker,
  type ChipOption,
} from '@/components/ui';
import { dateKey } from '@/lib/date';
import { addTask, deleteTask, updateTask, useTasks, type TaskInput } from '@/stores/tasks';
import { categoryColors, useTheme } from '@/theme';
import {
  CATEGORIES,
  CATEGORY_LABEL,
  PRIORITIES,
  PRIORITY_LABEL,
  type Category,
  type Priority,
  type TaskRecurrence,
} from '@/types/db';

const CATEGORY_OPTIONS: ChipOption<Category>[] = CATEGORIES.map((c) => ({
  label: CATEGORY_LABEL[c],
  value: c,
  color: categoryColors[c],
}));

const RECURRENCE_OPTIONS: ChipOption<TaskRecurrence>[] = [
  { label: 'One-time', value: 'none' },
  { label: 'Daily', value: 'daily' },
  { label: 'Weekly', value: 'weekly' },
  { label: 'Monthly', value: 'monthly' },
  { label: 'Custom', value: 'custom' },
];

function combine(anchorKey: string, time: Date): string {
  return dayjs(anchorKey)
    .hour(time.getHours())
    .minute(time.getMinutes())
    .second(0)
    .millisecond(0)
    .toISOString();
}

export default function TaskFormScreen() {
  const { colors, spacing } = useTheme();
  const params = useLocalSearchParams<{ id?: string; date?: string }>();
  const existing = params.id ? useTasks.getState().getById(params.id) : undefined;
  const anchorDefault = existing?.due_date ?? params.date ?? dateKey(new Date());

  const [title, setTitle] = useState(existing?.title ?? '');
  const [description, setDescription] = useState(existing?.description ?? '');
  const [category, setCategory] = useState<Category>(existing?.category ?? 'other');
  const [priority, setPriority] = useState<Priority>(existing?.priority ?? 'medium');
  const [start, setStart] = useState<Date | null>(
    existing?.start_time ? new Date(existing.start_time) : null,
  );
  const [end, setEnd] = useState<Date | null>(
    existing?.end_time ? new Date(existing.end_time) : null,
  );
  const [due, setDue] = useState<Date>(dayjs(anchorDefault).toDate());
  const [recurrence, setRecurrence] = useState<TaskRecurrence>(existing?.recurrence ?? 'none');
  const [interval, setIntervalStr] = useState(String(existing?.recurrence_interval ?? 2));
  const [days, setDays] = useState<number[]>(existing?.recurrence_days ?? []);
  const [until, setUntil] = useState<Date | null>(
    existing?.recurrence_end_date ? dayjs(existing.recurrence_end_date).toDate() : null,
  );
  const [error, setError] = useState<string | null>(null);

  const priorityOptions = useMemo(
    () => PRIORITIES.map((p) => ({ label: PRIORITY_LABEL[p], value: p })),
    [],
  );

  const onSave = () => {
    if (!title.trim()) {
      setError('Please enter a title.');
      return;
    }
    const anchorKey = dateKey(due);
    const input: TaskInput = {
      title,
      description,
      category,
      priority,
      start_time: start ? combine(anchorKey, start) : null,
      end_time: end ? combine(anchorKey, end) : null,
      due_date: anchorKey,
      recurrence,
      recurrence_interval: recurrence === 'custom' ? Math.max(1, parseInt(interval, 10) || 1) : 1,
      recurrence_days: recurrence === 'weekly' ? days : [],
      recurrence_end_date: recurrence !== 'none' && until ? dateKey(until) : null,
    };
    if (existing) updateTask(existing.id, input);
    else addTask(input);
    router.back();
  };

  const onDelete = () => {
    if (!existing) return;
    Alert.alert('Delete task?', 'This can’t be undone.', [
      { text: 'Cancel', style: 'cancel' },
      {
        text: 'Delete',
        style: 'destructive',
        onPress: () => {
          deleteTask(existing.id);
          router.back();
        },
      },
    ]);
  };

  return (
    <Screen scroll edges={['top', 'left', 'right', 'bottom']}>
      <View style={styles.bar}>
        <Button title="Cancel" variant="ghost" size="sm" onPress={() => router.back()} />
        <Text variant="headline">{existing ? 'Edit task' : 'New task'}</Text>
        <Button title="Save" size="sm" onPress={onSave} />
      </View>

      <TextField
        label="Title"
        value={title}
        onChangeText={setTitle}
        placeholder="What do you need to do?"
        error={error}
      />
      <TextField
        label="Description"
        value={description}
        onChangeText={setDescription}
        placeholder="Optional details"
        multiline
        style={{ minHeight: 70, textAlignVertical: 'top' }}
      />

      <Field label="CATEGORY">
        <ChipSelect options={CATEGORY_OPTIONS} value={category} onChange={setCategory} />
      </Field>

      <Field label="PRIORITY">
        <Segmented options={priorityOptions} value={priority} onChange={setPriority} />
      </Field>

      <Field label="SCHEDULE">
        <View style={{ gap: 12 }}>
          <DateTimeField label="Date" mode="date" value={due} onChange={(d) => d && setDue(d)} />
          <View style={styles.row}>
            <View style={{ flex: 1 }}>
              <DateTimeField label="Start" mode="time" value={start} onChange={setStart} />
            </View>
            <View style={{ flex: 1 }}>
              <DateTimeField label="End" mode="time" value={end} onChange={setEnd} />
            </View>
          </View>
        </View>
      </Field>

      <Field label="REPEAT">
        <View style={{ gap: 12 }}>
          <ChipSelect options={RECURRENCE_OPTIONS} value={recurrence} onChange={setRecurrence} />
          {recurrence === 'weekly' ? <WeekdayPicker value={days} onChange={setDays} /> : null}
          {recurrence === 'custom' ? (
            <View style={styles.row}>
              <Text variant="body" tone="secondary">
                Every
              </Text>
              <TextField
                value={interval}
                onChangeText={setIntervalStr}
                keyboardType="number-pad"
                style={{ width: 64, textAlign: 'center' }}
              />
              <Text variant="body" tone="secondary">
                days
              </Text>
            </View>
          ) : null}
          {recurrence !== 'none' ? (
            <DateTimeField
              label="Repeat until (optional)"
              mode="date"
              value={until}
              onChange={setUntil}
            />
          ) : null}
        </View>
      </Field>

      {existing ? (
        <Button
          title="Delete task"
          variant="ghost"
          icon="trash-outline"
          onPress={onDelete}
          style={{ marginTop: spacing.sm }}
        />
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
  bar: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    marginBottom: 4,
  },
  row: { flexDirection: 'row', alignItems: 'center', gap: 12 },
});
