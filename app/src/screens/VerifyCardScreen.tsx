import React, { useState } from 'react';
import {
  View, Text, TouchableOpacity, StyleSheet, ActivityIndicator,
} from 'react-native';
import { NFCService } from '../services/NFCService';
import { useApp } from '../store/AppContext';

export default function VerifyCardScreen() {
  const { walletId, setCardVerified, logout } = useApp();
  const [loading, setLoading] = useState(false);
  const [error, setError]     = useState('');

  const scan = async () => {
    setLoading(true);
    setError('');
    try {
      const card = await NFCService.readCard();
      if (card.walletId !== walletId) {
        setError('Wrong NFC card. This card does not match your wallet.');
        return;
      }
      setCardVerified();
    } catch (e: any) {
      setError(e.message);
    } finally {
      setLoading(false);
    }
  };

  return (
    <View style={s.root}>
      <View style={s.content}>
        <Text style={s.icon}>📡</Text>
        <Text style={s.title}>Tap Your Card</Text>
        <Text style={s.sub}>
          Hold your NFC wallet card to the back of the phone to verify ownership and unlock your wallet.
        </Text>

        {!!error && <Text style={s.error}>{error}</Text>}

        <TouchableOpacity style={s.btn} onPress={scan} disabled={loading}>
          {loading
            ? <ActivityIndicator color="#fff" />
            : <Text style={s.btnText}>Tap NFC Card</Text>
          }
        </TouchableOpacity>

        <TouchableOpacity style={s.logoutBtn} onPress={logout}>
          <Text style={s.logoutText}>Sign out</Text>
        </TouchableOpacity>

        {/* Dev bypass — subtle, bottom of screen */}
        <TouchableOpacity style={s.skipBtn} onPress={setCardVerified}>
          <Text style={s.skipText}>skip</Text>
        </TouchableOpacity>
      </View>
    </View>
  );
}

const s = StyleSheet.create({
  root:      { flex: 1, backgroundColor: '#0F0C29', justifyContent: 'center' },
  content:   { paddingHorizontal: 32, alignItems: 'center', gap: 20 },
  icon:      { fontSize: 80 },
  title:     { fontSize: 28, fontWeight: '700', color: '#fff' },
  sub:       { color: 'rgba(255,255,255,0.6)', textAlign: 'center', lineHeight: 22 },
  error:     { color: '#f87171', textAlign: 'center' },
  btn: {
    backgroundColor: '#A855F7', borderRadius: 14,
    padding: 18, alignItems: 'center', width: '100%',
  },
  btnText:   { color: '#fff', fontWeight: '700', fontSize: 17 },
  logoutBtn: { marginTop: 8 },
  logoutText:{ color: 'rgba(255,255,255,0.35)', fontSize: 14 },
  skipBtn:   { marginTop: 16 },
  skipText:  { color: 'rgba(255,255,255,0.12)', fontSize: 12 },
});
