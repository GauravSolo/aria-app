import dayjs from 'dayjs';
import { router, useLocalSearchParams } from 'expo-router';
import { useState } from 'react';
import { Alert, Pressable, StyleSheet, View } from 'react-native';

import {
  Button,
  ChipSelect,
  DateTimeField,
  Screen,
  Segmented,
  Stepper,
  Text,
  TextField,
  WeekdayPicker,
  type ChipOption,
} from '@/components/ui';
import { dateKey } from '@/lib/date';
import { addHabit, deleteHabit, updateHabit, useHabits, type HabitInput } from '@/stores/habits';
import { categoryColors, useTheme } from '@/theme';
import {
  CATEGORIES,
  CATEGORY_LABEL,
  type Category,
  type Frequency,
  type HabitKind,
} from '@/types/db';

const CATEGORY_OPTIONS: ChipOption<Category>[] = CATEGORIES.map((c) => ({
  label: CATEGORY_LABEL[c],
  value: c,
  color: categoryColors[c],
}));

const COLOR_PRESETS = ['#6366F1', '#8B5CF6', '#10B981', '#F59E0B', '#EF4444', '#3B82F6', '#EC4899'];

export default function HabitFormScreen() {
  const { colors, spacing } = useTheme();
  const params = useLocalSearchParams<{ id?: string }>();
  const existing = params.id ? useHabits.getState().getById(params.id) : undefined;

  const [name, setName] = useState(existing?.name ?? '');
  const [kind, setKind] = useState<HabitKind>(existing?.kind ?? 'build');
  const [category, setCategory] = useState<Category>(existing?.category ?? 'health');
  const [frequency, setFrequency] = useState<Frequency>(existing?.frequency ?? 'daily');
  const [days, setDays] = useState<number[]>(existing?.custom_days ?? []);
  const [target, setTarget] = useState(existing?.target_count ?? 1);
  const [color, setColor] = useState<string | null>(existing?.color ?? null);
  const [startDate, setStartDate] = useState<Date>(
    existing?.start_date ? dayjs(existing.start_date).toDate() : new Date(),
  );
  const [reminder, setReminder] = useState<Date | null>(
    existing?.reminder_time ? dayjs(`2000-01-01T${existing.reminder_time}`).toDate() : null,
  );
  const [notes, setNotes] = useState(existing?.notes ?? '');
  const [error, setError] = useState<string | null>(null);

  const onSave = () => {
    if (!name.trim()) {
      setError('Please name your habit.');
      return;
    }
    const input: HabitInput = {
      name,
      kind,
      category,
      frequency,
      target_count: target,
      custom_days: frequency === 'daily' ? [] : days,
      reminder_time: reminder ? dayjs(reminder).format('HH:mm') : null,
      start_date: dateKey(startDate),
      notes,
      color,
    };
    if (existing) updateHabit(existing.id, input);
    else addHabit(input);
    router.back();
  };

  const onDelete = () => {
    if (!existing) return;
    Alert.alert('Delete habit?', 'Its history will be removed too.', [
      { text: 'Cancel', style: 'cancel' },
      {
        text: 'Delete',
        style: 'destructive',
        onPress: () => {
          deleteHabit(existing.id);
          router.back();
        },
      },
    ]);
  };

  return (
    <Screen scroll edges={['top', 'left', 'right', 'bottom']}>
      <View style={styles.bar}>
        <Button title="Cancel" variant="ghost" size="sm" onPress={() => router.back()} />
        <Text variant="headline">{existing ? 'Edit habit' : 'New habit'}</Text>
        <Button title="Save" size="sm" onPress={onSave} />
      </View>

      <TextField
        label="Habit name"
        value={name}
        onChangeText={setName}
        placeholder="e.g. Read 20 pages"
        error={error}
      />

      <Field label="TYPE">
        <Segmented
          options={[
            { label: 'Build', value: 'build', icon: 'add-circle-outline' },
            { label: 'Quit', value: 'quit', icon: 'remove-circle-outline' },
          ]}
          value={kind}
          onChange={setKind}
        />
      </Field>

      <Field label="CATEGORY">
        <ChipSelect options={CATEGORY_OPTIONS} value={category} onChange={setCategory} />
      </Field>

      <Field label="FREQUENCY">
        <View style={{ gap: 12 }}>
          <Segmented
            options={[
              { label: 'Daily', value: 'daily' },
              { label: 'Weekly', value: 'weekly' },
              { label: 'Custom', value: 'custom' },
            ]}
            value={frequency}
            onChange={setFrequency}
          />
          {frequency !== 'daily' ? <WeekdayPicker value={days} onChange={setDays} /> : null}
        </View>
      </Field>

      <Field label="TARGET PER DAY">
        <Stepper value={target} onChange={setTarget} min={1} max={50} suffix={target > 1 ? 'times' : 'time'} />
      </Field>

      <Field label="COLOR">
        <View style={styles.swatches}>
          {COLOR_PRESETS.map((c) => (
            <Pressable
              key={c}
              onPress={() => setColor(c)}
              style={[
                styles.swatch,
                { backgroundColor: c, borderColor: color === c ? colors.text : 'transparent' },
              ]}
            />
          ))}
          <Pressable
            onPress={() => setColor(null)}
            style={[
              styles.swatch,
              styles.swatchNone,
              { borderColor: color === null ? colors.text : colors.border },
            ]}>
            <Text variant="caption" tone="muted">
              Auto
            </Text>
          </Pressable>
        </View>
      </Field>

      <Field label="SCHEDULE">
        <View style={{ gap: 12 }}>
          <DateTimeField
            label="Start date"
            mode="date"
            value={startDate}
            onChange={(d) => d && setStartDate(d)}
          />
          <DateTimeField
            label="Reminder time (optional)"
            mode="time"
            value={reminder}
            onChange={setReminder}
            icon="notifications-outline"
          />
        </View>
      </Field>

      <TextField
        label="Notes"
        value={notes}
        onChangeText={setNotes}
        placeholder="Optional"
        multiline
        style={{ minHeight: 64, textAlignVertical: 'top' }}
      />

      {existing ? (
        <Button
          title="Delete habit"
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
  bar: { flexDirection: 'row', alignItems: 'center', justifyContent: 'space-between', marginBottom: 4 },
  swatches: { flexDirection: 'row', flexWrap: 'wrap', gap: 10 },
  swatch: { width: 36, height: 36, borderRadius: 18, borderWidth: 2 },
  swatchNone: { alignItems: 'center', justifyContent: 'center' },
});
