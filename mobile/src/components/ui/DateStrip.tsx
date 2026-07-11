import dayjs from 'dayjs';
import { useMemo } from 'react';
import { FlatList, Pressable, StyleSheet, View } from 'react-native';

import { dateKey, todayKey } from '@/lib/date';
import { useTheme } from '@/theme';
import { Text } from './Text';

const BACK = 7;
const FORWARD = 30;
const ITEM_WIDTH = 56;

type Props = {
  selected: string; // YYYY-MM-DD
  onSelect: (key: string) => void;
  marked?: Set<string>;
};

export function DateStrip({ selected, onSelect, marked }: Props) {
  const { colors, radius } = useTheme();
  const today = todayKey();

  const days = useMemo(() => {
    const start = dayjs().subtract(BACK, 'day');
    return Array.from({ length: BACK + FORWARD + 1 }, (_, i) => start.add(i, 'day'));
  }, []);

  return (
    <FlatList
      horizontal
      data={days}
      keyExtractor={(d) => d.format('YYYY-MM-DD')}
      showsHorizontalScrollIndicator={false}
      contentContainerStyle={styles.content}
      initialScrollIndex={BACK}
      getItemLayout={(_, index) => ({ length: ITEM_WIDTH, offset: ITEM_WIDTH * index, index })}
      renderItem={({ item }) => {
        const key = dateKey(item);
        const active = key === selected;
        const isToday = key === today;
        const hasMark = marked?.has(key);
        return (
          <Pressable
            onPress={() => onSelect(key)}
            style={[
              styles.day,
              {
                borderRadius: radius.lg,
                backgroundColor: active ? colors.primary : colors.surface,
                borderColor: active ? colors.primary : colors.border,
              },
            ]}>
            <Text variant="caption" color={active ? colors.onPrimary : colors.textMuted}>
              {item.format('ddd').toUpperCase()}
            </Text>
            <Text
              variant="title2"
              color={active ? colors.onPrimary : isToday ? colors.primary : colors.text}>
              {item.format('D')}
            </Text>
            <View
              style={[
                styles.dot,
                {
                  backgroundColor: hasMark
                    ? active
                      ? colors.onPrimary
                      : colors.primary
                    : 'transparent',
                },
              ]}
            />
          </Pressable>
        );
      }}
    />
  );
}

const styles = StyleSheet.create({
  content: { gap: 8, paddingVertical: 4, paddingHorizontal: 2 },
  day: {
    width: ITEM_WIDTH - 8,
    paddingVertical: 10,
    alignItems: 'center',
    gap: 3,
    borderWidth: 1,
  },
  dot: { width: 5, height: 5, borderRadius: 3, marginTop: 1 },
});
