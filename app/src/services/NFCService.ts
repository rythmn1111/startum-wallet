/**
 * NFCService — reads and writes the NFC card payload.
 *
 * NFC card stores a compact JSON text record:
 *   {"w":"<16-char-walletId>","h":"<base64-nfcHalf>"}
 *
 * Storage budget on a 137-byte card:
 *   JSON payload: ~119 bytes
 *   NDEF text record overhead: ~7 bytes
 *   Total: ~126 bytes  ✓
 */
import NfcManager, { Ndef, NfcTech } from 'react-native-nfc-manager';
import { bytesToBase64, base64ToBytes, bytesToHex, hexToBytes } from './CryptoService';
import type { NFCCardPayload } from '../types';

let initialized = false;

async function ensureInit() {
  if (!initialized) {
    await NfcManager.start();
    initialized = true;
  }
}

export const NFCService = {
  isSupported: async (): Promise<boolean> => {
    try {
      await ensureInit();
      return await NfcManager.isSupported();
    } catch {
      return false;
    }
  },

  /** Write the combined NFC payload (single tap, single write). */
  writeCard: async (walletId: string, nfcHalfHex: string): Promise<void> => {
    await ensureInit();
    try {
      await NfcManager.requestTechnology(NfcTech.Ndef);

      // Encode nfcHalf as base64 to save space vs hex
      const nfcHalfBase64 = bytesToBase64(hexToBytes(nfcHalfHex));

      const payload: NFCCardPayload = { w: walletId, h: nfcHalfBase64 };
      const jsonStr = JSON.stringify(payload);
      const bytes = Ndef.encodeMessage([Ndef.textRecord(jsonStr)]);
      await NfcManager.ndefHandler.writeNdefMessage(bytes);
    } finally {
      NfcManager.cancelTechnologyRequest();
    }
  },

  /** Read the NFC card and return walletId + nfcHalf (as hex). */
  readCard: async (): Promise<{ walletId: string; nfcHalfHex: string }> => {
    await ensureInit();
    try {
      await NfcManager.requestTechnology(NfcTech.Ndef);
      const tag = await NfcManager.getTag();
      if (!tag?.ndefMessage?.length) throw new Error('Empty NFC tag');

      const record = tag.ndefMessage[0];
      const text = Ndef.text.decodePayload(record.payload as unknown as Buffer);
      const parsed: NFCCardPayload = JSON.parse(text);

      if (!parsed.w || !parsed.h) throw new Error('Invalid NFC payload');

      return {
        walletId:   parsed.w,
        nfcHalfHex: bytesToHex(base64ToBytes(parsed.h)),
      };
    } finally {
      NfcManager.cancelTechnologyRequest();
    }
  },
};
