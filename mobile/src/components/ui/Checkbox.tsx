import { Pressable } from 'react-native';

import { useTheme } from '@/theme';
import { Icon } from './Icon';

type Props = {
  checked: boolean;
  onPress?: () => void;
  color?: string;
  size?: number;
};

export function Checkbox({ checked, onPress, color, size = 26 }: Props) {
  const { colors } = useTheme();
  const tint = color ?? colors.primary;
  return (
    <Pressable
      onPress={onPress}
      hitSlop={10}
      style={{
        width: size,
        height: size,
        borderRadius: size / 2,
        borderWidth: 2,
        borderColor: checked ? tint : colors.borderStrong,
        backgroundColor: checked ? tint : 'transparent',
        alignItems: 'center',
        justifyContent: 'center',
      }}>
      {checked ? <Icon name="checkmark" size={size * 0.6} color={colors.onPrimary} /> : null}
    </Pressable>
  );
}
