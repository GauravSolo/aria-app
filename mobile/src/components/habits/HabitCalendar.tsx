import { ScrollView, StyleSheet, View } from 'react-native';

import { Text } from '@/components/ui';
import type { DayCell, DayStatus } from '@/lib/streaks';
import { useTheme } from '@/theme';

type Props = {
  weeks: DayCell[][]; // columns of 7 (Sun→Sat)
  color: string;
};

const CELL = 15;
const GAP = 4;

export function HabitCalendar({ weeks, color }: Props) {
  const { colors } = useTheme();

  const fill = (status: DayStatus): { backgroundColor: string; borderColor?: string; borderWidth?: number } => {
    switch (status) {
      case 'completed':
        return { backgroundColor: color };
      case 'missed':
        return { backgroundColor: colors.dangerSoft };
      case 'pending':
        return { backgroundColor: colors.surface, borderColor: colors.borderStrong, borderWidth: 1 };
      case 'off':
        return { backgroundColor: colors.track };
      case 'future':
      default:
        return { backgroundColor: colors.surfaceAlt };
    }
  };

  return (
    <View style={{ gap: 8 }}>
      <ScrollView horizontal showsHorizontalScrollIndicator={false}>
        <View style={styles.grid}>
          {weeks.map((col, ci) => (
            <View key={ci} style={styles.col}>
              {col.map((cell) => (
                <View
                  key={cell.date}
                  style={[styles.cell, { borderRadius: 4 }, fill(cell.status)]}
                />
              ))}
            </View>
          ))}
        </View>
      </ScrollView>
      <View style={styles.legend}>
        <LegendDot color={colors.track} label="" />
        <Text variant="caption" tone="muted">
          Less
        </Text>
        <LegendDot color={color + '66'} label="" />
        <LegendDot color={color} label="" />
        <Text variant="caption" tone="muted">
          More
        </Text>
      </View>
    </View>
  );
}

function LegendDot({ color }: { color: string; label: string }) {
  return <View style={{ width: 12, height: 12, borderRadius: 3, backgroundColor: color }} />;
}

const styles = StyleSheet.create({
  grid: { flexDirection: 'row', gap: GAP },
  col: { gap: GAP },
  cell: { width: CELL, height: CELL },
  legend: { flexDirection: 'row', alignItems: 'center', gap: 5 },
});
