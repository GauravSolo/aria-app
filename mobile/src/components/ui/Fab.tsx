import { Pressable, StyleSheet } from 'react-native';

import { useTheme } from '@/theme';
import { Icon, type IconName } from './Icon';

type Props = {
  icon?: IconName;
  onPress?: () => void;
};

/** Floating action button, bottom-right. */
export function Fab({ icon = 'add', onPress }: Props) {
  const { colors } = useTheme();
  return (
    <Pressable
      onPress={onPress}
      style={({ pressed }) => [
        styles.fab,
        {
          backgroundColor: colors.primary,
          opacity: pressed ? 0.9 : 1,
          shadowColor: colors.primary,
        },
      ]}>
      <Icon name={icon} size={28} color={colors.onPrimary} />
    </Pressable>
  );
}

const styles = StyleSheet.create({
  fab: {
    position: 'absolute',
    right: 18,
    bottom: 24,
    width: 58,
    height: 58,
    borderRadius: 29,
    alignItems: 'center',
    justifyContent: 'center',
    shadowOpacity: 0.35,
    shadowRadius: 10,
    shadowOffset: { width: 0, height: 4 },
    elevation: 6,
  },
});
