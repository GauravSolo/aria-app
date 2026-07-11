import DateTimePicker, {
  type DateTimePickerEvent,
} from '@react-native-community/datetimepicker';
import dayjs from 'dayjs';
import { useState } from 'react';
import { Platform, Pressable, StyleSheet, View } from 'react-native';

import { useTheme } from '@/theme';
import { Icon, type IconName } from './Icon';
import { Text } from './Text';

type Props = {
  label?: string;
  mode: 'date' | 'time';
  value: Date | null;
  onChange: (d: Date | null) => void;
  placeholder?: string;
  icon?: IconName;
};

export function DateTimeField({ label, mode, value, onChange, placeholder, icon }: Props) {
  const { colors, radius } = useTheme();
  const [show, setShow] = useState(false);

  const handle = (event: DateTimePickerEvent, selected?: Date) => {
    if (Platform.OS !== 'ios') setShow(false);
    if (event.type === 'set' && selected) {
      onChange(selected);
      if (Platform.OS === 'ios') setShow(false);
    } else if (event.type === 'dismissed') {
      setShow(false);
    }
  };

  const display = value
    ? mode === 'time'
      ? dayjs(value).format('h:mm A')
      : dayjs(value).format('MMM D, YYYY')
    : (placeholder ?? (mode === 'time' ? 'Pick a time' : 'Pick a date'));

  return (
    <View style={styles.wrap}>
      {label ? (
        <Text variant="subhead" tone="secondary" style={styles.label}>
          {label}
        </Text>
      ) : null}
      <Pressable
        onPress={() => setShow(true)}
        style={[
          styles.field,
          { backgroundColor: colors.surface, borderColor: colors.border, borderRadius: radius.md },
        ]}>
        <Icon name={icon ?? (mode === 'time' ? 'time-outline' : 'calendar-outline')} size={18} color={colors.textMuted} />
        <Text variant="body" color={value ? colors.text : colors.textMuted} style={styles.value}>
          {display}
        </Text>
        {value ? (
          <Pressable hitSlop={8} onPress={() => onChange(null)}>
            <Icon name="close-circle" size={18} color={colors.textMuted} />
          </Pressable>
        ) : (
          <Icon name="chevron-forward" size={16} color={colors.textMuted} />
        )}
      </Pressable>
      {show ? (
        <DateTimePicker
          value={value ?? new Date()}
          mode={mode}
          onChange={handle}
          display={Platform.OS === 'ios' ? 'spinner' : 'default'}
        />
      ) : null}
    </View>
  );
}

const styles = StyleSheet.create({
  wrap: { gap: 6 },
  label: { marginLeft: 2 },
  field: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 10,
    borderWidth: 1,
    paddingHorizontal: 14,
    minHeight: 50,
  },
  value: { flex: 1 },
});
