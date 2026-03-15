import React, { useRef } from 'react';
import {
  StyleSheet,
  ViewStyle,
  Animated,
  Pressable,
} from 'react-native';
import { LinearGradient } from 'expo-linear-gradient';
import { Colors, Radius, Shadows } from '../../theme/colors';

interface GradientCardProps {
  children:   React.ReactNode;
  style?:     ViewStyle | ViewStyle[];
  glowColor?: string;
  onPress?:   () => void;
  noPadding?: boolean;
  disabled?:  boolean;
}

export default function GradientCard({
  children,
  style,
  glowColor,
  onPress,
  noPadding = false,
  disabled  = false,
}: GradientCardProps) {
  const scaleAnim = useRef(new Animated.Value(1)).current;

  const pressIn = () => {
    if (!onPress) return;
    Animated.timing(scaleAnim, {
      toValue: 0.97,
      duration: 100,
      useNativeDriver: true,
    }).start();
  };

  const pressOut = () => {
    if (!onPress) return;
    Animated.spring(scaleAnim, {
      toValue: 1,
      useNativeDriver: true,
      speed: 30,
      bounciness: 4,
    }).start();
  };

  const glowShadow = glowColor
    ? {
        shadowColor:  glowColor,
        shadowOffset: { width: 0, height: 0 },
        shadowOpacity: 0.35,
        shadowRadius:  20,
        elevation:     12,
      }
    : Shadows.card;

  const inner = (
    <Animated.View style={{ transform: [{ scale: scaleAnim }] }}>
      <LinearGradient
        colors={['#393E46', '#2E3238']}
        start={{ x: 0, y: 0 }}
        end={{ x: 1, y: 1 }}
        style={[
          styles.card,
          glowShadow,
          !noPadding && styles.defaultPadding,
          style,
        ]}
      >
        {children}
      </LinearGradient>
    </Animated.View>
  );

  if (!onPress) return inner;

  return (
    <Pressable
      onPress={onPress}
      onPressIn={pressIn}
      onPressOut={pressOut}
      disabled={disabled}
    >
      {inner}
    </Pressable>
  );
}

const styles = StyleSheet.create({
  card: {
    borderRadius: Radius.lg,
    borderWidth: 1,
    borderColor: Colors.surfaceBorder,
    overflow: 'hidden',
  },
  defaultPadding: {
    padding: 16,
  },
});
