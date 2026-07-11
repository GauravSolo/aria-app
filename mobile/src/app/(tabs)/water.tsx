import { router } from 'expo-router';
import { useMemo, useState } from 'react';
import { Modal, StyleSheet, View } from 'react-native';

import {
  BarChart,
  Button,
  Card,
  CircularProgress,
  Header,
  IconButton,
  Screen,
  StatTile,
  Stepper,
  Text,
} from '@/components/ui';
import { todayKey } from '@/lib/date';
import { formatMl, monthStats, totalForDate, weekSeries } from '@/lib/water';
import { useAuth } from '@/stores/auth';
import { logWater, undoLastWater, useWaterLogs, useWaterSettings } from '@/stores/water';
import { useTheme } from '@/theme';

export default function WaterScreen() {
  const { colors } = useTheme();
  const uid = useAuth((s) => s.user?.id ?? s.guestId ?? '');
  const logItems = useWaterLogs((s) => s.items);
  useWaterSettings((s) => s.byUser); // subscribe to settings changes
  const settings = useWaterSettings.getState().current();
  const today = todayKey();

  const logs = useMemo(
    () => Object.values(logItems).filter((l) => !l.deleted_at && l.user_id === uid),
    [logItems, uid],
  );

  const total = totalForDate(logs, today);
  const goal = settings.daily_goal_ml;
  const progress = goal > 0 ? total / goal : 0;
  const week = useMemo(() => weekSeries(logs, today), [logs, today]);
  const month = useMemo(() => monthStats(logs, goal, today), [logs, goal, today]);

  const [customOpen, setCustomOpen] = useState(false);
  const [customMl, setCustomMl] = useState(settings.glass_size_ml);

  const weekBars = week.map((d) => ({ label: d.label, value: d.ml, highlight: d.date === today }));

  return (
    <Screen scroll>
      <Header
        title="Water"
        subtitle="Hydration"
        right={<IconButton name="options-outline" onPress={() => router.push('/water-settings')} />}
      />

      {/* Ring + today total */}
      <Card style={styles.ringCard}>
        <CircularProgress progress={progress} size={190} color={colors.info}>
          <View style={{ alignItems: 'center' }}>
            <Text variant="display" color={colors.info}>
              {Math.round(progress * 100)}%
            </Text>
            <Text variant="callout" tone="secondary">
              {formatMl(total)} / {formatMl(goal)}
            </Text>
          </View>
        </CircularProgress>
        <Text variant="footnote" tone="muted">
          {total >= goal ? 'Goal reached — nice work! 💧' : `${formatMl(Math.max(0, goal - total))} to go`}
        </Text>
      </Card>

      {/* Quick add */}
      <View style={styles.addRow}>
        <Button
          title={`Glass · ${settings.glass_size_ml}ml`}
          icon="water"
          onPress={() => logWater(settings.glass_size_ml)}
          style={styles.flex}
        />
        <Button title="Custom" variant="secondary" icon="add" onPress={() => setCustomOpen(true)} />
      </View>
      <Button
        title="Undo last glass"
        variant="ghost"
        icon="arrow-undo-outline"
        size="sm"
        onPress={() => undoLastWater()}
      />

      {/* Weekly summary */}
      <Card style={{ gap: 14 }}>
        <View style={styles.cardHead}>
          <Text variant="bodyStrong">This week</Text>
          <Text variant="footnote" tone="secondary">
            goal {formatMl(goal)}/day
          </Text>
        </View>
        <BarChart data={weekBars} color={colors.info} goal={goal} />
      </Card>

      {/* Monthly summary */}
      <Text variant="caption" tone="muted" style={styles.sectionLabel}>
        THIS MONTH
      </Text>
      <View style={styles.grid}>
        <View style={styles.col}>
          <StatTile label="Total" value={formatMl(month.total)} icon="water-outline" color={colors.info} />
        </View>
        <View style={styles.col}>
          <StatTile label="Daily average" value={formatMl(month.average)} icon="stats-chart" color={colors.primary} />
        </View>
        <View style={styles.col}>
          <StatTile label="Days goal met" value={month.daysMetGoal} icon="trophy" color={colors.success} />
        </View>
        <View style={styles.col}>
          <StatTile label="Days tracked" value={month.daysTracked} icon="calendar" color={colors.accent} />
        </View>
      </View>
      <View style={{ height: 8 }} />

      {/* Custom amount modal */}
      <Modal visible={customOpen} transparent animationType="fade" onRequestClose={() => setCustomOpen(false)}>
        <View style={[styles.modalOverlay, { backgroundColor: colors.overlay }]}>
          <Card style={styles.modalCard}>
            <Text variant="title2">Add water</Text>
            <Text variant="footnote" tone="secondary">
              Choose an amount in millilitres.
            </Text>
            <View style={{ alignItems: 'center', paddingVertical: 8 }}>
              <Stepper value={customMl} onChange={setCustomMl} min={50} max={2000} step={50} suffix="ml" />
            </View>
            <View style={styles.modalActions}>
              <Button title="Cancel" variant="ghost" onPress={() => setCustomOpen(false)} style={styles.flex} />
              <Button
                title={`Add ${customMl}ml`}
                onPress={() => {
                  logWater(customMl);
                  setCustomOpen(false);
                }}
                style={styles.flex}
              />
            </View>
          </Card>
        </View>
      </Modal>
    </Screen>
  );
}

const styles = StyleSheet.create({
  ringCard: { alignItems: 'center', gap: 12 },
  addRow: { flexDirection: 'row', gap: 10 },
  flex: { flex: 1 },
  cardHead: { flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center' },
  sectionLabel: { marginLeft: 4, letterSpacing: 1 },
  grid: { flexDirection: 'row', flexWrap: 'wrap', gap: 12 },
  col: { flexGrow: 1, flexBasis: '46%' },
  modalOverlay: { flex: 1, alignItems: 'center', justifyContent: 'center', padding: 24 },
  modalCard: { width: '100%', maxWidth: 360, gap: 8 },
  modalActions: { flexDirection: 'row', gap: 10, marginTop: 8 },
});
