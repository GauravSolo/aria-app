import { StyleSheet, View } from 'react-native';

import { Text } from './Text';

type Props = {
  title: string;
  subtitle?: string;
  right?: React.ReactNode;
};

/** Large screen header used at the top of each tab. */
export function Header({ title, subtitle, right }: Props) {
  return (
    <View style={styles.row}>
      <View style={styles.flex}>
        {subtitle ? (
          <Text variant="caption" tone="muted" style={styles.subtitle}>
            {subtitle.toUpperCase()}
          </Text>
        ) : null}
        <Text variant="title">{title}</Text>
      </View>
      {right ? <View style={styles.right}>{right}</View> : null}
    </View>
  );
}

const styles = StyleSheet.create({
  row: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    paddingTop: 8,
    paddingBottom: 4,
  },
  flex: { flex: 1, gap: 2 },
  subtitle: { letterSpacing: 1 },
  right: { marginLeft: 12 },
});
