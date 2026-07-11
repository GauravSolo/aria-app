import { Pressable, type StyleProp, type ViewStyle } from 'react-native';

import { useTheme } from '@/theme';
import { Icon, type IconName } from './Icon';

type Props = {
  name: IconName;
  onPress?: () => void;
  size?: number;
  color?: string;
  filled?: boolean;
  style?: StyleProp<ViewStyle>;
};

/** Circular tappable icon used for header actions and inline controls. */
export function IconButton({ name, onPress, size = 22, color, filled, style }: Props) {
  const { colors, radius } = useTheme();
  return (
    <Pressable
      onPress={onPress}
      hitSlop={8}
      style={({ pressed }) => [
        {
          width: 40,
          height: 40,
          alignItems: 'center',
          justifyContent: 'center',
          borderRadius: radius.pill,
          backgroundColor: filled ? colors.surfaceAlt : 'transparent',
          opacity: pressed ? 0.6 : 1,
        },
        style,
      ]}>
      <Icon name={name} size={size} color={color ?? colors.textSecondary} />
    </Pressable>
  );
}
