import { StyleSheet, View } from 'react-native';

import { useTheme } from '@/theme';
import { Text } from './Text';

export type Bar = {
  label: string;
  value: number;
  highlight?: boolean;
};

type Props = {
  data: Bar[];
  height?: number;
  color?: string;
  /** Optional reference line (e.g. daily goal). */
  goal?: number;
};

/** Lightweight View-based bar chart (no native chart dep). */
export function BarChart({ data, height = 130, color, goal }: Props) {
  const { colors } = useTheme();
  const tint = color ?? colors.primary;
  const max = Math.max(goal ?? 0, ...data.map((d) => d.value), 1);

  return (
    <View>
      <View style={[styles.plot, { height }]}>
        {goal && goal > 0 ? (
          <View
            style={[
              styles.goalLine,
              { bottom: `${(goal / max) * 100}%`, borderColor: colors.borderStrong },
            ]}
          />
        ) : null}
        {data.map((d, i) => {
          const pct = max > 0 ? (d.value / max) * 100 : 0;
          return (
            <View key={i} style={styles.barCol}>
              <View style={styles.barTrack}>
                <View
                  style={{
                    height: `${pct}%`,
                    minHeight: d.value > 0 ? 4 : 0,
                    width: '64%',
                    alignSelf: 'center',
                    borderRadius: 6,
                    backgroundColor: d.highlight ? tint : tint + '55',
                  }}
                />
              </View>
            </View>
          );
        })}
      </View>
      <View style={styles.labels}>
        {data.map((d, i) => (
          <Text key={i} variant="caption" tone="muted" style={styles.label}>
            {d.label}
          </Text>
        ))}
      </View>
    </View>
  );
}

const styles = StyleSheet.create({
  plot: { flexDirection: 'row', alignItems: 'flex-end', gap: 6 },
  goalLine: {
    position: 'absolute',
    left: 0,
    right: 0,
    borderTopWidth: 1,
    borderStyle: 'dashed',
  },
  barCol: { flex: 1, height: '100%', justifyContent: 'flex-end' },
  barTrack: { height: '100%', justifyContent: 'flex-end' },
  labels: { flexDirection: 'row', gap: 6, marginTop: 6 },
  label: { flex: 1, textAlign: 'center' },
});
