import React, { useState } from 'react';
import {
  View, Text, TextInput, TouchableOpacity, StyleSheet,
  ScrollView, ActivityIndicator,
} from 'react-native';
import { NFCService } from '../services/NFCService';
import { NetworkService } from '../services/NetworkService';
import { reconstructKeys } from '../services/WalletService';
import { hexToBytes } from '../services/CryptoService';

type Step = 'select' | 'details' | 'nfc' | 'password' | 'sending' | 'done';

const ETH_RPC = 'https://ethereum-sepolia-rpc.publicnode.com';
const SOL_RPC = 'https://api.devnet.solana.com';

async function sendEth(ethPrivKeyHex: string, to: string, amountEth: string): Promise<string> {
  const { ethers } = require('ethers');
  const provider = new ethers.JsonRpcProvider(ETH_RPC);
  const wallet = new ethers.Wallet(ethPrivKeyHex, provider);
  const tx = await wallet.sendTransaction({ to, value: ethers.parseEther(amountEth) });
  return tx.hash;
}

async function sendSol(solPrivKeyHex: string, to: string, amountSol: string): Promise<string> {
  const nacl = require('tweetnacl');
  const { Connection, PublicKey, SystemProgram, Transaction, Keypair } = require('@solana/web3.js');
  const privKeyBytes = hexToBytes(solPrivKeyHex);
  const keypair = nacl.sign.keyPair.fromSeed(privKeyBytes);
  const solKeypair = Keypair.fromSecretKey(
    Buffer.concat([Buffer.from(privKeyBytes), Buffer.from(keypair.publicKey)])
  );
  const connection = new Connection(SOL_RPC, 'confirmed');
  const lamports = Math.round(parseFloat(amountSol) * 1e9);
  const { blockhash } = await connection.getLatestBlockhash('confirmed');
  const txn = new Transaction().add(
    SystemProgram.transfer({
      fromPubkey: new PublicKey(keypair.publicKey),
      toPubkey: new PublicKey(to),
      lamports,
    })
  );
  txn.recentBlockhash = blockhash;
  txn.feePayer = new PublicKey(keypair.publicKey);
  txn.sign(solKeypair);
  return connection.sendRawTransaction(txn.serialize());
}

export default function SendScreen() {
  const [step, setStep]         = useState<Step>('select');
  const [chain, setChain]       = useState<'ETH' | 'SOL'>('ETH');
  const [toAddress, setTo]      = useState('');
  const [amount, setAmount]     = useState('');
  const [password, setPassword] = useState('');
  const [walletId, setWalletId] = useState('');
  const [nfcHalf, setNfcHalf]   = useState('');
  const [txHash, setTxHash]     = useState('');
  const [loading, setLoading]   = useState(false);
  const [error, setError]       = useState('');

  const selectChain = (c: 'ETH' | 'SOL') => {
    setChain(c);
    setStep('details');
  };

  const goToNfc = () => {
    if (!toAddress.trim() || !amount.trim()) { setError('Fill in recipient and amount'); return; }
    setError('');
    setStep('nfc');
    // start NFC scan immediately
    NFCService.readCard()
      .then(card => {
        setWalletId(card.walletId);
        setNfcHalf(card.nfcHalfHex);
        setStep('password');
      })
      .catch(e => {
        setError(e.message);
        setStep('details');
      });
  };

  const doSend = async () => {
    setLoading(true);
    setError('');
    try {
      const server = await NetworkService.fetchWallet(walletId);
      const { ethPrivKey, solPrivKey } = await reconstructKeys(nfcHalf, server, password);
      const hash = chain === 'ETH'
        ? await sendEth(ethPrivKey, toAddress.trim(), amount.trim())
        : await sendSol(solPrivKey, toAddress.trim(), amount.trim());
      setTxHash(hash);
      setStep('done');
    } catch (e: any) {
      setError(e.message);
    } finally {
      setLoading(false);
    }
  };

  const reset = () => {
    setStep('select');
    setTo(''); setAmount(''); setPassword('');
    setWalletId(''); setNfcHalf(''); setTxHash(''); setError('');
  };

  // ── STEP: Select chain ──────────────────────────────────────────────────
  if (step === 'select') return (
    <View style={s.root}>
      <View style={s.center}>
        <Text style={s.title}>Pay with</Text>
        <Text style={s.sub}>Choose which currency to send</Text>
        <TouchableOpacity style={s.bigChainBtn} onPress={() => selectChain('ETH')}>
          <Text style={s.bigChainIcon}>⬡</Text>
          <Text style={s.bigChainLabel}>Ethereum</Text>
          <Text style={s.bigChainSub}>Sepolia testnet</Text>
        </TouchableOpacity>
        <TouchableOpacity style={[s.bigChainBtn, { borderColor: '#9945FF' }]} onPress={() => selectChain('SOL')}>
          <Text style={s.bigChainIcon}>◎</Text>
          <Text style={s.bigChainLabel}>Solana</Text>
          <Text style={s.bigChainSub}>Devnet</Text>
        </TouchableOpacity>
      </View>
    </View>
  );

  // ── STEP: Enter details ─────────────────────────────────────────────────
  if (step === 'details') return (
    <ScrollView style={s.root} contentContainerStyle={s.scrollContent} keyboardShouldPersistTaps="handled">
      <TouchableOpacity onPress={() => setStep('select')} style={s.back}>
        <Text style={s.backText}>← Back</Text>
      </TouchableOpacity>
      <Text style={s.title}>Send {chain}</Text>
      <Text style={s.label}>Recipient Address</Text>
      <TextInput
        style={s.input}
        placeholder={chain === 'ETH' ? '0x...' : 'Solana address'}
        placeholderTextColor="#888"
        value={toAddress} onChangeText={setTo}
        autoCapitalize="none" autoCorrect={false}
      />
      <Text style={s.label}>Amount ({chain})</Text>
      <TextInput
        style={s.input}
        placeholder="0.00"
        placeholderTextColor="#888"
        value={amount} onChangeText={setAmount}
        keyboardType="decimal-pad"
      />
      {!!error && <Text style={s.error}>{error}</Text>}
      <TouchableOpacity
        style={[s.btn, (!toAddress || !amount) && s.btnDisabled]}
        onPress={goToNfc}
        disabled={!toAddress || !amount}
      >
        <Text style={s.btnText}>Tap Card to Pay</Text>
      </TouchableOpacity>
    </ScrollView>
  );

  // ── STEP: NFC scanning ──────────────────────────────────────────────────
  if (step === 'nfc') return (
    <View style={s.root}>
      <View style={s.center}>
        <Text style={{ fontSize: 90 }}>📡</Text>
        <Text style={s.title}>Tap Your Card</Text>
        <Text style={s.sub}>Hold the NFC wallet card{'\n'}to the back of the phone</Text>
        <ActivityIndicator size="large" color="#A855F7" style={{ marginTop: 8 }} />
        {!!error && <Text style={s.error}>{error}</Text>}
        <TouchableOpacity style={s.cancelBtn} onPress={() => { NFCService.cancelScan?.(); setStep('details'); }}>
          <Text style={s.cancelText}>Cancel</Text>
        </TouchableOpacity>
      </View>
    </View>
  );

  // ── STEP: Password ──────────────────────────────────────────────────────
  if (step === 'password') return (
    <ScrollView style={s.root} contentContainerStyle={s.scrollContent} keyboardShouldPersistTaps="handled">
      <Text style={{ fontSize: 56, textAlign: 'center' }}>🔒</Text>
      <Text style={s.title}>Enter Password</Text>
      <Text style={s.sub}>
        Sending <Text style={{ color: '#A855F7', fontWeight: '700' }}>{amount} {chain}</Text>{'\n'}
        to {toAddress.slice(0, 8)}…{toAddress.slice(-6)}
      </Text>
      <TextInput
        style={s.input}
        placeholder="Encryption password"
        placeholderTextColor="#888"
        value={password} onChangeText={setPassword}
        secureTextEntry autoFocus
      />
      {!!error && <Text style={s.error}>{error}</Text>}
      <TouchableOpacity
        style={[s.btn, (!password || loading) && s.btnDisabled]}
        onPress={doSend} disabled={!password || loading}
      >
        {loading ? <ActivityIndicator color="#fff" /> : <Text style={s.btnText}>Confirm & Send</Text>}
      </TouchableOpacity>
      <TouchableOpacity style={s.cancelBtn} onPress={reset}>
        <Text style={s.cancelText}>Cancel</Text>
      </TouchableOpacity>
    </ScrollView>
  );

  // ── STEP: Done ──────────────────────────────────────────────────────────
  return (
    <View style={s.root}>
      <View style={s.center}>
        <Text style={{ fontSize: 80 }}>✅</Text>
        <Text style={s.title}>Sent!</Text>
        <View style={s.hashBox}>
          <Text style={s.hashLabel}>Transaction Hash</Text>
          <Text style={s.hash} selectable>{txHash}</Text>
        </View>
        <TouchableOpacity style={s.btn} onPress={reset}>
          <Text style={s.btnText}>New Payment</Text>
        </TouchableOpacity>
      </View>
    </View>
  );
}

const s = StyleSheet.create({
  root:         { flex: 1, backgroundColor: '#0F0C29' },
  scrollContent:{ padding: 24, paddingTop: 60, gap: 12, flexGrow: 1 },
  center:       { flex: 1, alignItems: 'center', justifyContent: 'center', gap: 20, paddingHorizontal: 32 },
  title:        { fontSize: 28, fontWeight: '700', color: '#fff', textAlign: 'center' },
  sub:          { color: 'rgba(255,255,255,0.6)', textAlign: 'center', lineHeight: 22 },
  label:        { color: 'rgba(255,255,255,0.7)', fontSize: 13, fontWeight: '600' },
  error:        { color: '#f87171', textAlign: 'center' },
  back:         { marginBottom: 8 },
  backText:     { color: '#A855F7', fontSize: 15 },
  bigChainBtn: {
    width: '100%', borderRadius: 20, borderWidth: 1.5,
    borderColor: '#627EEA', padding: 24, alignItems: 'center', gap: 4,
    backgroundColor: 'rgba(255,255,255,0.05)',
  },
  bigChainIcon:  { fontSize: 40 },
  bigChainLabel: { fontSize: 22, fontWeight: '700', color: '#fff' },
  bigChainSub:   { fontSize: 13, color: 'rgba(255,255,255,0.4)' },
  input: {
    backgroundColor: 'rgba(255,255,255,0.1)', borderRadius: 12,
    padding: 16, color: '#fff', fontSize: 16,
    borderWidth: 1, borderColor: 'rgba(255,255,255,0.15)',
  },
  btn: {
    backgroundColor: '#A855F7', borderRadius: 14,
    padding: 16, alignItems: 'center', width: '100%',
  },
  btnDisabled: { opacity: 0.4 },
  btnText:     { color: '#fff', fontWeight: '700', fontSize: 17 },
  cancelBtn:   { alignItems: 'center', paddingVertical: 8 },
  cancelText:  { color: 'rgba(255,255,255,0.35)', fontSize: 14 },
  hashBox: {
    backgroundColor: 'rgba(255,255,255,0.08)', borderRadius: 12,
    padding: 16, width: '100%', gap: 6,
  },
  hashLabel: { color: '#A855F7', fontSize: 11, fontWeight: '700', textTransform: 'uppercase', letterSpacing: 1 },
  hash:      { color: '#fff', fontSize: 11, fontFamily: 'monospace', lineHeight: 18 },
});
