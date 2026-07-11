import { StyleSheet, View } from 'react-native';

import { useTheme } from '@/theme';
import { Icon } from './Icon';
import { Text } from './Text';

type Props = { size?: number; showWordmark?: boolean };

/** Aria logo: rounded indigo tile with a spark, optional wordmark. */
export function BrandMark({ size = 56, showWordmark = false }: Props) {
  const { colors, radius } = useTheme();
  return (
    <View style={styles.wrap}>
      <View
        style={{
          width: size,
          height: size,
          borderRadius: radius.lg,
          backgroundColor: colors.primary,
          alignItems: 'center',
          justifyContent: 'center',
        }}>
        <Icon name="sparkles" size={size * 0.5} color={colors.onPrimary} />
      </View>
      {showWordmark ? <Text variant="display">Aria</Text> : null}
    </View>
  );
}

const styles = StyleSheet.create({
  wrap: { alignItems: 'center', gap: 14 },
});
