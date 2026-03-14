/**
 * WalletService — BIP39 mnemonic, BIP44 HD key derivation, combined key splitting.
 *
 * Flow:
 *   mnemonic → eth_priv (32 bytes) + sol_priv (32 bytes)
 *   combined = eth_priv || sol_priv  (64 bytes)
 *   encrypted = AES-256-GCM(combined, password)  →  ciphertext (64 bytes) + iv + tag + salt
 *   [nfcHalf, serverHalf] = XOR-split(ciphertext)
 *   NFC card stores: walletId + nfcHalf
 *   Server stores:   walletId + serverHalf + iv + tag + salt + ethAddress + solAddress
 *
 * ETH: secp256k1 via @scure/bip32, path m/44'/60'/0'/0/0
 * SOL: ed25519 via SLIP-0010, path m/44'/501'/0'/0'
 */
import * as bip39 from '@scure/bip39';
import { wordlist } from '@scure/bip39/wordlists/english';
import { HDKey } from '@scure/bip32';
import { ethers } from 'ethers';
import { hmac } from '@noble/hashes/hmac';
import { sha512 } from '@noble/hashes/sha512';
import QuickCrypto from 'react-native-quick-crypto';
import { encrypt, decrypt, xorSplit, xorCombine, hexToBytes, bytesToHex } from './CryptoService';
import type { CombinedKeySplit, ServerWalletResponse } from '../types';

function slip10DeriveEd25519(seed: Uint8Array, path: string): Uint8Array {
  let I = hmac(sha512, new TextEncoder().encode('ed25519 seed'), seed);
  let key = I.slice(0, 32);
  let chainCode = I.slice(32);

  const segments = path.split('/').slice(1);
  for (const seg of segments) {
    const hardened = seg.endsWith("'");
    const index = parseInt(hardened ? seg.slice(0, -1) : seg) + (hardened ? 0x80000000 : 0);
    const data = new Uint8Array(37);
    data[0] = 0x00;
    data.set(key, 1);
    new DataView(data.buffer).setUint32(33, index, false);
    I = hmac(sha512, chainCode, data);
    key = I.slice(0, 32);
    chainCode = I.slice(32);
  }
  return key;
}

function randomWalletId(): string {
  // 8 random bytes as a 16-char hex string — used as the NFC card lookup key
  const bytes = QuickCrypto.getRandomValues(new Uint8Array(8));
  return bytesToHex(bytes);
}

export async function generateWallets() {
  const mnemonic = bip39.generateMnemonic(wordlist, 128); // 12 words
  const seed = await bip39.mnemonicToSeed(mnemonic);

  // ETH — BIP44: m/44'/60'/0'/0/0
  const hdRoot  = HDKey.fromMasterSeed(seed);
  const ethNode = hdRoot.derive("m/44'/60'/0'/0/0");
  if (!ethNode.privateKey) throw new Error('Failed to derive ETH private key');
  const ethWallet  = new ethers.Wallet(bytesToHex(ethNode.privateKey));
  const ethAddress = ethWallet.address;

  // SOL — SLIP-0010 ed25519: m/44'/501'/0'/0'
  const solPrivKey = slip10DeriveEd25519(seed, "m/44'/501'/0'/0'");
  const nacl = require('tweetnacl');
  const solKeypair = nacl.sign.keyPair.fromSeed(solPrivKey);
  const solAddress = bs58Encode(solKeypair.publicKey);

  return {
    mnemonic,
    ethAddress,
    ethPrivateKey: bytesToHex(ethNode.privateKey),
    solAddress,
    solPrivateKey: bytesToHex(solPrivKey),
  };
}

/**
 * Encrypt both private keys together and XOR-split the ciphertext.
 * Returns everything needed for NFC write + server storage.
 */
export async function splitCombinedKeys(
  ethPrivKeyHex: string,
  solPrivKeyHex: string,
  ethAddress: string,
  solAddress: string,
  password: string,
): Promise<CombinedKeySplit> {
  // Concatenate both 32-byte private keys → 64-byte plaintext
  const ethBytes = hexToBytes(ethPrivKeyHex);
  const solBytes = hexToBytes(solPrivKeyHex);
  const combined = new Uint8Array(64);
  combined.set(ethBytes, 0);
  combined.set(solBytes, 32);

  // Encrypt combined blob
  const bundle = await encrypt(combined, password);

  // XOR-split only the ciphertext (iv/tag/salt stay with server)
  const { nfcHalf, serverHalf } = xorSplit(hexToBytes(bundle.ciphertext));

  return {
    walletId: randomWalletId(),
    nfcHalf,
    serverHalf,
    bundle,
    ethAddress,
    solAddress,
  };
}

/**
 * Reconstruct both private keys from NFC half + server response + password.
 * Returns { ethPrivKey, solPrivKey } as hex strings.
 * Caller is responsible for zeroing out keys after use.
 */
export async function reconstructKeys(
  nfcHalfHex: string,
  server: ServerWalletResponse,
  password: string,
): Promise<{ ethPrivKey: string; solPrivKey: string }> {
  const ciphertextBytes = xorCombine(nfcHalfHex, server.serverHalf);
  const plaintext = await decrypt(
    {
      ciphertext: bytesToHex(ciphertextBytes),
      iv:   server.iv,
      tag:  server.tag,
      salt: server.salt,
    },
    password,
  );

  if (plaintext.length !== 64) throw new Error('Decrypted key blob has unexpected length');

  return {
    ethPrivKey: bytesToHex(plaintext.slice(0, 32)),
    solPrivKey: bytesToHex(plaintext.slice(32, 64)),
  };
}

// Minimal Base58 encoder for Solana addresses
const BASE58_ALPHABET = '123456789ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz';
function bs58Encode(bytes: Uint8Array): string {
  const digits: number[] = [];
  for (const byte of bytes) {
    let carry = byte;
    for (let i = 0; i < digits.length; i++) {
      carry += digits[i] << 8;
      digits[i] = carry % 58;
      carry = Math.floor(carry / 58);
    }
    while (carry > 0) { digits.push(carry % 58); carry = Math.floor(carry / 58); }
  }
  let result = '';
  for (let i = 0; i < bytes.length && bytes[i] === 0; i++) result += '1';
  for (let i = digits.length - 1; i >= 0; i--) result += BASE58_ALPHABET[digits[i]];
  return result;
}
