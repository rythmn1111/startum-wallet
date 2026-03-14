import React, { useState } from 'react';
import {
  View, Text, TextInput, TouchableOpacity, StyleSheet,
  KeyboardAvoidingView, Platform, ActivityIndicator, Alert,
} from 'react-native';
import { NetworkService } from '../services/NetworkService';
import { useApp } from '../store/AppContext';

export default function OnboardingScreen() {
  const { login } = useApp();
  const [isLogin, setIsLogin]     = useState(false);
  const [username, setUsername]   = useState('');
  const [password, setPassword]   = useState('');
  const [loading, setLoading]     = useState(false);

  const submit = async () => {
    if (!username || !password) return;
    if (!isLogin && username.length < 3) {
      Alert.alert('Username must be at least 3 characters');
      return;
    }
    if (!isLogin && password.length < 8) {
      Alert.alert('Login password must be at least 8 characters');
      return;
    }
    setLoading(true);
    try {
      const res = isLogin
        ? await NetworkService.login(username.trim(), password)
        : await NetworkService.register(username.trim(), password);
      await login(res.token);
    } catch (e: any) {
      Alert.alert('Error', e.message);
    } finally {
      setLoading(false);
    }
  };

  return (
    <KeyboardAvoidingView style={s.root} behavior={Platform.OS === 'ios' ? 'padding' : undefined}>
      <View style={s.logo}>
        <Text style={s.logoIcon}>📡</Text>
        <Text style={s.title}>NFC Wallet</Text>
        <Text style={s.subtitle}>Your keys. Split in two.</Text>
      </View>

      <View style={s.form}>
        <TextInput
          style={s.input}
          placeholder="Username"
          placeholderTextColor="#888"
          value={username}
          onChangeText={setUsername}
          autoCapitalize="none"
          autoCorrect={false}
        />
        <TextInput
          style={s.input}
          placeholder="Login Password"
          placeholderTextColor="#888"
          value={password}
          onChangeText={setPassword}
          secureTextEntry
        />

        <TouchableOpacity style={s.btn} onPress={submit} disabled={loading}>
          {loading
            ? <ActivityIndicator color="#000" />
            : <Text style={s.btnText}>{isLogin ? 'Sign In' : 'Create Account'}</Text>
          }
        </TouchableOpacity>

        <TouchableOpacity onPress={() => setIsLogin(!isLogin)}>
          <Text style={s.toggle}>
            {isLogin ? "Don't have an account? Register" : 'Already have an account? Sign In'}
          </Text>
        </TouchableOpacity>
      </View>
    </KeyboardAvoidingView>
  );
}

const s = StyleSheet.create({
  root:     { flex: 1, backgroundColor: '#0F0C29', justifyContent: 'center' },
  logo:     { alignItems: 'center', marginBottom: 48 },
  logoIcon: { fontSize: 64, marginBottom: 8 },
  title:    { fontSize: 36, fontWeight: '700', color: '#fff' },
  subtitle: { color: '#aaa', marginTop: 4 },
  form:     { paddingHorizontal: 32, gap: 12 },
  input: {
    backgroundColor: 'rgba(255,255,255,0.1)', borderRadius: 12, padding: 16,
    color: '#fff', fontSize: 16, borderWidth: 1, borderColor: 'rgba(255,255,255,0.15)',
  },
  btn: {
    backgroundColor: '#fff', borderRadius: 14, padding: 16,
    alignItems: 'center', marginTop: 8,
  },
  btnText: { fontWeight: '600', fontSize: 16, color: '#000' },
  toggle:  { color: 'rgba(255,255,255,0.6)', textAlign: 'center', marginTop: 8 },
});
