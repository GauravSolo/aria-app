import dayjs from 'dayjs';
import { StyleSheet, View } from 'react-native';

import { Card, Checkbox, Chip, Text } from '@/components/ui';
import { recurrenceLabel } from '@/lib/recurrence';
import { categoryColors, priorityColors, useTheme } from '@/theme';
import { CATEGORY_LABEL, type Task } from '@/types/db';

type Props = {
  task: Task;
  done: boolean;
  onToggle: () => void;
  onPress: () => void;
};

function timeRange(task: Task): string | null {
  if (!task.start_time) return null;
  const start = dayjs(task.start_time).format('h:mm A');
  if (task.end_time) return `${start} – ${dayjs(task.end_time).format('h:mm A')}`;
  return start;
}

export function TaskCard({ task, done, onToggle, onPress }: Props) {
  const { colors } = useTheme();
  const time = timeRange(task);
  const catColor = categoryColors[task.category];

  return (
    <Card onPress={onPress} style={styles.card}>
      <View style={[styles.priorityBar, { backgroundColor: priorityColors[task.priority] }]} />
      <Checkbox checked={done} onPress={onToggle} color={catColor} />
      <View style={styles.body}>
        <Text
          variant="bodyStrong"
          numberOfLines={2}
          style={done ? { textDecorationLine: 'line-through', color: colors.textMuted } : undefined}>
          {task.title}
        </Text>
        {task.description ? (
          <Text variant="footnote" tone="secondary" numberOfLines={1}>
            {task.description}
          </Text>
        ) : null}
        <View style={styles.meta}>
          {time ? <Chip label={time} icon="time-outline" /> : null}
          <Chip label={CATEGORY_LABEL[task.category]} color={catColor} />
          {task.recurrence !== 'none' ? (
            <Chip label={recurrenceLabel(task)} icon="repeat-outline" />
          ) : null}
        </View>
      </View>
    </Card>
  );
}

const styles = StyleSheet.create({
  card: { flexDirection: 'row', alignItems: 'center', gap: 14, overflow: 'hidden' },
  priorityBar: { position: 'absolute', left: 0, top: 0, bottom: 0, width: 4 },
  body: { flex: 1, gap: 4 },
  meta: { flexDirection: 'row', flexWrap: 'wrap', gap: 6, marginTop: 2 },
});
