import { StyleSheet, View } from 'react-native';

import { useTheme } from '@/theme';
import { Icon, type IconName } from './Icon';
import { Text } from './Text';

type Props = {
  icon: IconName;
  title: string;
  message?: string;
  children?: React.ReactNode;
};

export function EmptyState({ icon, title, message, children }: Props) {
  const { colors, radius } = useTheme();
  return (
    <View style={styles.wrap}>
      <View
        style={[
          styles.iconWrap,
          { backgroundColor: colors.primarySoft, borderRadius: radius.pill },
        ]}>
        <Icon name={icon} size={30} color={colors.primary} />
      </View>
      <Text variant="title2" center>
        {title}
      </Text>
      {message ? (
        <Text variant="callout" tone="secondary" center style={styles.message}>
          {message}
        </Text>
      ) : null}
      {children ? <View style={styles.action}>{children}</View> : null}
    </View>
  );
}

const styles = StyleSheet.create({
  wrap: { alignItems: 'center', justifyContent: 'center', paddingVertical: 48, gap: 10 },
  iconWrap: { width: 64, height: 64, alignItems: 'center', justifyContent: 'center' },
  message: { maxWidth: 300 },
  action: { marginTop: 12, alignSelf: 'stretch', paddingHorizontal: 24 },
});
