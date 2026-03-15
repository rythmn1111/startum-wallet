import 'react-native-get-random-values';
import React from 'react';
import { NavigationContainer } from '@react-navigation/native';
import { StatusBar, StyleSheet, Text, View } from 'react-native';
import { SafeAreaProvider } from 'react-native-safe-area-context';
import { AppProvider } from './src/store/AppContext';
import AppNavigator from './src/navigation/AppNavigator';
import { Colors } from './src/theme/colors';
import { FontFamily, FontSize, useLoadFonts } from './src/theme/typography';

export default function App() {
  const fontsLoaded = useLoadFonts();

  if (!fontsLoaded) {
    return (
      <SafeAreaProvider>
        <View style={styles.splash}>
          <StatusBar barStyle="light-content" />
          <View style={styles.logoWrap}>
            <Text allowFontScaling={false} style={styles.logo}>N</Text>
          </View>
          <Text allowFontScaling={false} style={styles.wordmark}>NFC Wallet</Text>
          <Text allowFontScaling={false} style={styles.subtitle}>Loading secure wallet experience…</Text>
        </View>
      </SafeAreaProvider>
    );
  }

  return (
    <SafeAreaProvider>
      <AppProvider>
        <NavigationContainer>
          <AppNavigator />
        </NavigationContainer>
      </AppProvider>
    </SafeAreaProvider>
  );
}

const styles = StyleSheet.create({
  splash: {
    flex: 1,
    alignItems: 'center',
    justifyContent: 'center',
    backgroundColor: Colors.deepDark,
    gap: 14,
    paddingHorizontal: 24,
  },
  logoWrap: {
    width: 72,
    height: 72,
    borderRadius: 36,
    alignItems: 'center',
    justifyContent: 'center',
    backgroundColor: Colors.orangeDim,
    borderWidth: 1,
    borderColor: Colors.orangeMid,
  },
  logo: {
    fontFamily: FontFamily.syneXBold,
    fontSize: FontSize['2xl'],
    color: Colors.brandOrange,
  },
  wordmark: {
    fontFamily: FontFamily.syneXBold,
    fontSize: FontSize['2xl'],
    color: Colors.offWhite,
  },
  subtitle: {
    fontFamily: FontFamily.inter,
    fontSize: FontSize.base,
    color: Colors.textMuted,
    textAlign: 'center',
  },
});
