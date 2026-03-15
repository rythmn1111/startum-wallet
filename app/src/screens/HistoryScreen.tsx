/**
 * HistoryScreen — encrypted payment receipts via Fileverse.
 *
 * Every payment you collect creates an E2E encrypted Fileverse document.
 * No bank, no Stripe, no middlemen can read these. They're anchored on-chain
 * and accessible at ddocs.new — owned only by you.
 */
import React, { useState, useCallback } from 'react';
import {
  View, Text, StyleSheet, FlatList, TouchableOpacity,
  ActivityIndicator, Linking, RefreshControl,
} from 'react-native';
import { NetworkService } from '../services/NetworkService';
import { useFocusEffect } from '@react-navigation/native';

interface Receipt {
  ddocId: string;
  title: string;
  docUrl: string;
  createdAt: string;
}

export default function HistoryScreen() {
  const [receipts, setReceipts] = useState<Receipt[]>([]);
  const [loading, setLoading]   = useState(false);
  const [error, setError]       = useState('');

  const load = useCallback(async () => {
    setLoading(true);
    setError('');
    try {
      const data = await NetworkService.listReceipts();
      setReceipts(data.reverse()); // newest first
    } catch (e: any) {
      setError(e.message);
    } finally {
      setLoading(false);
    }
  }, []);

  // Reload every time the tab is focused
  useFocusEffect(load);

  const openDoc = (url: string) => Linking.openURL(url);

  const renderItem = ({ item }: { item: Receipt }) => (
    <TouchableOpacity style={s.card} onPress={() => openDoc(item.docUrl)} activeOpacity={0.7}>
      <View style={s.cardTop}>
        <Text style={s.cardTitle} numberOfLines={1}>{item.title}</Text>
        <Text style={s.cardArrow}>→</Text>
      </View>
      <Text style={s.cardDate}>{item.createdAt ? new Date(item.createdAt).toLocaleString() : '—'}</Text>
      <View style={s.cardBadgeRow}>
        <View style={s.badge}>
          <Text style={s.badgeText}>🔒 E2E Encrypted</Text>
        </View>
        <View style={[s.badge, s.badgeGreen]}>
          <Text style={s.badgeText}>⛓ On-chain</Text>
        </View>
      </View>
      <Text style={s.docUrl} numberOfLines={1}>{item.docUrl}</Text>
    </TouchableOpacity>
  );

  return (
    <View style={s.root}>
      {/* Header */}
      <View style={s.header}>
        <Text style={s.title}>Receipts</Text>
        <Text style={s.sub}>
          Every payment creates an encrypted Fileverse document.{'\n'}
          Tap a receipt to open it at ddocs.new.
        </Text>
      </View>

      {/* Privacy banner */}
      <View style={s.banner}>
        <Text style={s.bannerIcon}>🔐</Text>
        <View style={{ flex: 1 }}>
          <Text style={s.bannerTitle}>End-to-end encrypted receipts</Text>
          <Text style={s.bannerSub}>No bank. No Stripe. No middlemen. Just you.</Text>
        </View>
      </View>

      {!!error && (
        <View style={s.errorBox}>
          <Text style={s.errorText}>{error}</Text>
          <TouchableOpacity onPress={load} style={s.retryBtn}>
            <Text style={s.retryText}>Retry</Text>
          </TouchableOpacity>
        </View>
      )}

      {loading && receipts.length === 0 ? (
        <View style={s.center}>
          <ActivityIndicator size="large" color="#A855F7" />
          <Text style={s.loadingText}>Loading receipts…</Text>
        </View>
      ) : (
        <FlatList
          data={receipts}
          keyExtractor={r => r.ddocId}
          renderItem={renderItem}
          contentContainerStyle={s.list}
          refreshControl={<RefreshControl refreshing={loading} onRefresh={load} tintColor="#A855F7" />}
          ListEmptyComponent={
            !loading ? (
              <View style={s.empty}>
                <Text style={s.emptyIcon}>🧾</Text>
                <Text style={s.emptyText}>No receipts yet</Text>
                <Text style={s.emptySub}>Every payment you collect will appear here as an encrypted Fileverse document.</Text>
              </View>
            ) : null
          }
        />
      )}
    </View>
  );
}

const s = StyleSheet.create({
  root:   { flex: 1, backgroundColor: '#0F0C29' },
  header: { paddingHorizontal: 24, paddingTop: 60, paddingBottom: 16, gap: 6 },
  title:  { fontSize: 26, fontWeight: '700', color: '#fff' },
  sub:    { color: 'rgba(255,255,255,0.45)', fontSize: 13, lineHeight: 20 },

  banner: {
    flexDirection: 'row', alignItems: 'center', gap: 12,
    marginHorizontal: 24, marginBottom: 16,
    backgroundColor: 'rgba(168,85,247,0.12)', borderRadius: 14,
    padding: 14, borderWidth: 1, borderColor: 'rgba(168,85,247,0.25)',
  },
  bannerIcon:  { fontSize: 24 },
  bannerTitle: { color: '#A855F7', fontWeight: '700', fontSize: 13 },
  bannerSub:   { color: 'rgba(255,255,255,0.45)', fontSize: 12, marginTop: 2 },

  list: { paddingHorizontal: 24, paddingBottom: 40, gap: 12 },

  card: {
    backgroundColor: 'rgba(255,255,255,0.06)', borderRadius: 16,
    padding: 16, gap: 8, borderWidth: 1, borderColor: 'rgba(255,255,255,0.1)',
  },
  cardTop:   { flexDirection: 'row', alignItems: 'center', justifyContent: 'space-between' },
  cardTitle: { color: '#fff', fontWeight: '600', fontSize: 15, flex: 1 },
  cardArrow: { color: '#A855F7', fontWeight: '700', fontSize: 16 },
  cardDate:  { color: 'rgba(255,255,255,0.35)', fontSize: 12 },
  cardBadgeRow: { flexDirection: 'row', gap: 8 },
  badge: {
    backgroundColor: 'rgba(168,85,247,0.15)', borderRadius: 20,
    paddingHorizontal: 10, paddingVertical: 3,
  },
  badgeGreen: { backgroundColor: 'rgba(34,197,94,0.15)' },
  badgeText: { color: 'rgba(255,255,255,0.6)', fontSize: 11, fontWeight: '600' },
  docUrl:    { color: 'rgba(168,85,247,0.6)', fontSize: 11, fontFamily: 'monospace' },

  center:      { flex: 1, alignItems: 'center', justifyContent: 'center', gap: 12 },
  loadingText: { color: 'rgba(255,255,255,0.4)', fontSize: 14 },

  empty:     { alignItems: 'center', paddingTop: 60, gap: 12, paddingHorizontal: 32 },
  emptyIcon: { fontSize: 48 },
  emptyText: { color: '#fff', fontSize: 18, fontWeight: '600' },
  emptySub:  { color: 'rgba(255,255,255,0.4)', textAlign: 'center', lineHeight: 20, fontSize: 13 },

  errorBox:  { marginHorizontal: 24, backgroundColor: 'rgba(248,113,113,0.1)', borderRadius: 12, padding: 14, gap: 8 },
  errorText: { color: '#f87171', fontSize: 13 },
  retryBtn:  { alignSelf: 'flex-start' },
  retryText: { color: '#A855F7', fontWeight: '600', fontSize: 13 },
});
