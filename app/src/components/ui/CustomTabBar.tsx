/**
 * CustomTabBar — premium bottom navigation with floating orange Pay FAB.
 * 4 regular tabs + center floating action button.
 */
import React, { useEffect, useRef } from 'react';
import {
  Animated,
  Pressable,
  StyleSheet,
  Text,
  View,
} from 'react-native';
import { BottomTabBarProps } from '@react-navigation/bottom-tabs';
import { Feather } from '@expo/vector-icons';

import { Colors, Radius, Shadows } from '../../theme/colors';
import { FontFamily, FontSize } from '../../theme/typography';

// ── Tab icon map ──────────────────────────────────────────────────────────────
const TAB_ICON: Record<string, string> = {
  Wallet:   'home',
  Receive:  'download',
  Pay:      'send',
  Activity: 'clock',
  Settings: 'settings',
};

// ── Regular tab item ──────────────────────────────────────────────────────────
function TabItem({
  label,
  icon,
  focused,
  onPress,
}: {
  label: string;
  icon: string;
  focused: boolean;
  onPress: () => void;
}) {
  const scale      = useRef(new Animated.Value(1)).current;
  const dotOpacity = useRef(new Animated.Value(focused ? 1 : 0)).current;

  useEffect(() => {
    Animated.parallel([
      Animated.spring(scale, {
        toValue: focused ? 1.08 : 1,
        useNativeDriver: true,
        speed: 30,
        bounciness: 6,
      }),
      Animated.timing(dotOpacity, {
        toValue: focused ? 1 : 0,
        duration: 200,
        useNativeDriver: true,
      }),
    ]).start();
  }, [focused]);

  return (
    <Pressable onPress={onPress} style={styles.tabItem}>
      <Animated.View style={{ transform: [{ scale }], alignItems: 'center', gap: 4 }}>
        <Feather
          name={icon as any}
          size={22}
          color={focused ? Colors.brandOrange : Colors.textFaint}
        />
        <Animated.View
          style={[styles.activeIndicator, { opacity: dotOpacity }]}
        />
        <Text
          allowFontScaling={false}
          style={[styles.tabLabel, focused ? styles.tabLabelActive : styles.tabLabelInactive]}
        >
          {label}
        </Text>
      </Animated.View>
    </Pressable>
  );
}

// ── FAB (floating Pay button) ─────────────────────────────────────────────────
function FabButton({ onPress }: { onPress: () => void }) {
  const scale = useRef(new Animated.Value(1)).current;

  function handlePress() {
    Animated.sequence([
      Animated.spring(scale, { toValue: 0.9, useNativeDriver: true, speed: 50 }),
      Animated.spring(scale, { toValue: 1,   useNativeDriver: true, speed: 30, bounciness: 10 }),
    ]).start();
    onPress();
  }

  return (
    <View style={styles.fabWrap}>
      <Pressable onPress={handlePress} hitSlop={10}>
        <Animated.View style={[styles.fab, { transform: [{ scale }] }, Shadows.orangeGlowStrong as object]}>
          <Feather name="send" size={24} color={Colors.offWhite} />
        </Animated.View>
      </Pressable>
      <Text allowFontScaling={false} style={styles.fabLabel}>Pay</Text>
    </View>
  );
}

// ── MAIN COMPONENT ────────────────────────────────────────────────────────────
export default function CustomTabBar({ state, descriptors, navigation }: BottomTabBarProps) {
  const routes = state.routes;

  return (
    <View style={styles.barShadow}>
      <View style={styles.bar}>
        {routes.map((route, index) => {
          const { options } = descriptors[route.key];
          const focused = state.index === index;
          const label   = (options.tabBarLabel as string) ?? route.name;
          const icon    = TAB_ICON[route.name] ?? 'circle';

          const isFAB = route.name === 'Pay';

          if (isFAB) {
            return (
              <FabButton
                key={route.key}
                onPress={() => {
                  const event = navigation.emit({ type: 'tabPress', target: route.key, canPreventDefault: true });
                  if (!focused && !event.defaultPrevented) navigation.navigate(route.name);
                }}
              />
            );
          }

          return (
            <TabItem
              key={route.key}
              label={label}
              icon={icon}
              focused={focused}
              onPress={() => {
                const event = navigation.emit({ type: 'tabPress', target: route.key, canPreventDefault: true });
                if (!focused && !event.defaultPrevented) navigation.navigate(route.name);
              }}
            />
          );
        })}
      </View>
    </View>
  );
}

// ── Styles ────────────────────────────────────────────────────────────────────
const styles = StyleSheet.create({
  barShadow: {
    shadowColor: Colors.deepDark,
    shadowOffset: { width: 0, height: -4 },
    shadowOpacity: 0.6,
    shadowRadius: 12,
    elevation: 16,
  },
  bar: {
    flexDirection: 'row',
    alignItems: 'flex-end',
    backgroundColor: Colors.surface,
    borderTopWidth: 1,
    borderTopColor: Colors.divider,
    paddingBottom: 28,
    paddingTop: 10,
    paddingHorizontal: 8,
  },

  tabItem: {
    flex: 1,
    alignItems: 'center',
    paddingVertical: 4,
  },
  activeIndicator: {
    width: 4,
    height: 4,
    borderRadius: 2,
    backgroundColor: Colors.brandOrange,
    marginTop: -2,
  },
  tabLabel: {
    fontFamily: FontFamily.interMedium,
    fontSize: 10,
    marginTop: 2,
  },
  tabLabelActive:   { color: Colors.brandOrange },
  tabLabelInactive: { color: Colors.textFaint },

  fabWrap: {
    flex: 1,
    alignItems: 'center',
    marginBottom: 8,
  },
  fab: {
    width: 60,
    height: 60,
    borderRadius: 30,
    backgroundColor: Colors.brandOrange,
    alignItems: 'center',
    justifyContent: 'center',
    marginBottom: 4,
    marginTop: -24,
  },
  fabLabel: {
    fontFamily: FontFamily.interSemiBold,
    fontSize: 10,
    color: Colors.brandOrange,
    marginTop: 2,
  },
});
