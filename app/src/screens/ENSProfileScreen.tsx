/**
 * ENSProfileScreen — configure ENS-based payment profile.
 *
 * The user enters their ENS name (e.g. "rythmn.eth").
 * The app resolves it and reads nfcwallet.* text records from mainnet ENS.
 * Those records configure how incoming payments are processed:
 *   - max amount, auto-split, preferred token, privacy mode
 *
 * Text records are set on https://app.ens.domains — this screen shows
 * what to set and guides the user through it.
 */
import React, { useState } from 'react';
import {
  View, Text, TextInput, TouchableOpacity, StyleSheet,
  ScrollView, ActivityIndicator, Linking,
} from 'react-native';
import { useApp } from '../store/AppContext';
import { ENSService, ENSPaymentProfile } from '../services/ENSService';

const RECORD_GUIDE = [
  {
    key: 'nfcwallet.receive-as',
    label: 'Preferred Token',
    example: 'USDC',
    desc: 'Hint payers which token you prefer to receive.',
  },
  {
    key: 'nfcwallet.max-amount',
    label: 'Max Payment (ETH)',
    example: '0.5',
    desc: 'Reject any payment above this amount.',
  },
  {
    key: 'nfcwallet.auto-split',
    label: 'Auto Split',
    example: 'savings.eth:20',
    desc: '20% of every payment auto-routes to savings.eth.',
  },
  {
    key: 'nfcwallet.privacy',
    label: 'Privacy Mode',
    example: 'true',
    desc: 'Each payment lands on a fresh BitGo address — unlinkable on-chain.',
  },
  {
    key: 'nfcwallet.nfc-required',
    label: 'NFC Required',
    example: 'true',
    desc: 'Only NFC-authenticated payers can send to you.',
  },
];

export default function ENSProfileScreen() {
  const { ensName, setEnsName } = useApp();
  const [input, setInput]       = useState(ensName);
  const [loading, setLoading]   = useState(false);
  const [profile, setProfile]   = useState<ENSPaymentProfile | null>(null);
  const [error, setError]       = useState('');
  const [saved, setSaved]       = useState(false);

  const lookup = async () => {
    const name = input.trim().toLowerCase();
    if (!name.includes('.')) { setError('Enter a valid ENS name (e.g. rythmn.eth)'); return; }
    setLoading(true);
    setError('');
    setProfile(null);
    setSaved(false);
    try {
      const p = await ENSService.resolveProfile(name);
      if (!p) {
        setError('ENS name not found or not registered.');
        return;
      }
      setProfile(p);
      await setEnsName(name);
      setSaved(true);
    } catch (e: any) {
      setError(e.message);
    } finally {
      setLoading(false);
    }
  };

  const activeCount = profile
    ? [profile.receiveAs, profile.autoSplit, profile.privacy, profile.nfcRequired, profile.maxAmount]
        .filter(Boolean).length
    : 0;

  return (
    <ScrollView style={s.root} contentContainerStyle={s.content} keyboardShouldPersistTaps="handled">

      {/* Header */}
      <View style={s.header}>
        <Text style={s.title}>ENS Payment Profile</Text>
        <Text style={s.sub}>
          Your ENS name is your payment identity.{'\n'}
          Text records configure exactly how you get paid.
        </Text>
      </View>

      {/* ENS input */}
      <View style={s.inputRow}>
        <TextInput
          style={s.input}
          placeholder="yourname.eth"
          placeholderTextColor="#666"
          value={input}
          onChangeText={text => { setInput(text); setSaved(false); }}
          autoCapitalize="none"
          autoCorrect={false}
        />
        <TouchableOpacity style={s.lookupBtn} onPress={lookup} disabled={loading}>
          {loading
            ? <ActivityIndicator color="#fff" size="small" />
            : <Text style={s.lookupBtnText}>Look up</Text>
          }
        </TouchableOpacity>
      </View>

      {!!error && <Text style={s.error}>{error}</Text>}

      {saved && !loading && (
        <Text style={s.savedText}>✓ Saved — payments will respect your ENS config</Text>
      )}

      {/* Active profile */}
      {profile && (
        <View style={s.profileCard}>
          <View style={s.profileHeader}>
            <Text style={s.profileName}>{profile.ensName}</Text>
            <View style={s.activeBadge}>
              <Text style={s.activeBadgeText}>{activeCount} rule{activeCount !== 1 ? 's' : ''} active</Text>
            </View>
          </View>
          <Text style={s.profileAddr} numberOfLines={1}>{profile.address}</Text>

          <View style={s.rules}>
            {profile.maxAmount !== undefined && (
              <RuleRow icon="🚫" label="Max Payment" value={`${profile.maxAmount} ETH`} />
            )}
            {profile.autoSplit && (
              <RuleRow icon="✂️" label="Auto Split" value={profile.autoSplit} />
            )}
            {profile.receiveAs && (
              <RuleRow icon="💱" label="Receive As" value={profile.receiveAs} />
            )}
            {profile.privacy && (
              <RuleRow icon="🔒" label="Privacy Mode" value="Fresh address per payment" />
            )}
            {profile.nfcRequired && (
              <RuleRow icon="📡" label="NFC Required" value="Card tap mandatory" />
            )}
            {activeCount === 0 && (
              <Text style={s.noRules}>No nfcwallet.* text records found yet.{'\n'}Set them up below.</Text>
            )}
          </View>
        </View>
      )}

      {/* Setup guide */}
      <View style={s.guide}>
        <Text style={s.guideTitle}>Set Up Your Payment Rules</Text>
        <Text style={s.guideSub}>
          Go to app.ens.domains → your name → Text Records → Add record
        </Text>

        {RECORD_GUIDE.map(r => (
          <View key={r.key} style={s.guideRow}>
            <View style={s.guideKeyRow}>
              <Text style={s.guideKey}>{r.key}</Text>
              <Text style={s.guideVal}>{r.example}</Text>
            </View>
            <Text style={s.guideDesc}>{r.desc}</Text>
          </View>
        ))}

        <TouchableOpacity
          style={s.ensLink}
          onPress={() => Linking.openURL('https://app.ens.domains')}
        >
          <Text style={s.ensLinkText}>Open ENS App →</Text>
        </TouchableOpacity>
      </View>

    </ScrollView>
  );
}

function RuleRow({ icon, label, value }: { icon: string; label: string; value: string }) {
  return (
    <View style={s.ruleRow}>
      <Text style={s.ruleIcon}>{icon}</Text>
      <View style={{ flex: 1 }}>
        <Text style={s.ruleLabel}>{label}</Text>
        <Text style={s.ruleValue}>{value}</Text>
      </View>
    </View>
  );
}

const s = StyleSheet.create({
  root:    { flex: 1, backgroundColor: '#0F0C29' },
  content: { padding: 24, paddingTop: 60, gap: 20 },

  header: { gap: 8 },
  title:  { fontSize: 26, fontWeight: '700', color: '#fff' },
  sub:    { color: 'rgba(255,255,255,0.5)', lineHeight: 22 },

  inputRow: { flexDirection: 'row', gap: 10 },
  input: {
    flex: 1, backgroundColor: 'rgba(255,255,255,0.1)',
    borderRadius: 12, padding: 14, color: '#fff', fontSize: 15,
    borderWidth: 1, borderColor: 'rgba(255,255,255,0.15)',
  },
  lookupBtn: {
    backgroundColor: '#A855F7', borderRadius: 12,
    paddingHorizontal: 18, justifyContent: 'center', minWidth: 80, alignItems: 'center',
  },
  lookupBtnText: { color: '#fff', fontWeight: '700', fontSize: 14 },

  error:     { color: '#f87171', fontSize: 13 },
  savedText: { color: '#22c55e', fontSize: 13 },

  profileCard: {
    backgroundColor: 'rgba(168,85,247,0.1)', borderRadius: 16,
    padding: 18, borderWidth: 1, borderColor: 'rgba(168,85,247,0.3)', gap: 12,
  },
  profileHeader:   { flexDirection: 'row', alignItems: 'center', justifyContent: 'space-between' },
  profileName:     { color: '#fff', fontSize: 18, fontWeight: '700' },
  activeBadge:     { backgroundColor: '#A855F7', borderRadius: 20, paddingHorizontal: 10, paddingVertical: 3 },
  activeBadgeText: { color: '#fff', fontSize: 11, fontWeight: '700' },
  profileAddr:     { color: 'rgba(255,255,255,0.4)', fontSize: 12, fontFamily: 'monospace' },

  rules:   { gap: 10 },
  ruleRow: { flexDirection: 'row', alignItems: 'flex-start', gap: 10 },
  ruleIcon:  { fontSize: 18, marginTop: 1 },
  ruleLabel: { color: 'rgba(255,255,255,0.5)', fontSize: 11, fontWeight: '600', textTransform: 'uppercase', letterSpacing: 0.5 },
  ruleValue: { color: '#fff', fontSize: 14, fontWeight: '600', marginTop: 1 },
  noRules:   { color: 'rgba(255,255,255,0.35)', fontSize: 13, lineHeight: 20 },

  guide: {
    backgroundColor: 'rgba(255,255,255,0.04)', borderRadius: 16,
    padding: 18, gap: 14,
  },
  guideTitle: { color: '#fff', fontSize: 16, fontWeight: '700' },
  guideSub:   { color: 'rgba(255,255,255,0.4)', fontSize: 12, lineHeight: 18 },
  guideRow:   { gap: 4 },
  guideKeyRow:{ flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center' },
  guideKey:   { color: '#A855F7', fontSize: 12, fontFamily: 'monospace', flex: 1 },
  guideVal:   { color: '#22c55e', fontSize: 12, fontFamily: 'monospace', fontWeight: '700' },
  guideDesc:  { color: 'rgba(255,255,255,0.4)', fontSize: 12, lineHeight: 18 },
  ensLink: {
    borderWidth: 1, borderColor: '#A855F7', borderRadius: 10,
    padding: 12, alignItems: 'center', marginTop: 4,
  },
  ensLinkText: { color: '#A855F7', fontWeight: '700', fontSize: 14 },
});
