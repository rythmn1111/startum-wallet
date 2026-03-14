/**
 * ENSService — resolves ENS names and reads nfcwallet.* text records.
 *
 * ENS is resolved against Ethereum MAINNET (where real ENS names live).
 * Transactions still happen on Sepolia/Devnet — ENS is read-only here.
 *
 * Supported text record keys:
 *   nfcwallet.receive-as   → preferred token e.g. "USDC" (display hint)
 *   nfcwallet.auto-split   → "address_or_ens:percentage" e.g. "savings.eth:20"
 *   nfcwallet.privacy      → "true" = route through BitGo fresh address
 *   nfcwallet.nfc-required → "true" = NFC tap mandatory (always true for us)
 *   nfcwallet.max-amount   → max ETH accepted per payment e.g. "0.5"
 */
import { ethers } from 'ethers';

const MAINNET_RPC = 'https://ethereum-rpc.publicnode.com';

const TEXT_KEYS = [
  'nfcwallet.receive-as',
  'nfcwallet.auto-split',
  'nfcwallet.privacy',
  'nfcwallet.nfc-required',
  'nfcwallet.max-amount',
] as const;

export interface ENSPaymentProfile {
  ensName:     string;
  address:     string;
  receiveAs?:  string;   // 'USDC', 'ETH', etc.
  autoSplit?:  string;   // 'savings.eth:20' — parsed at payment time
  privacy?:    boolean;  // route through BitGo fresh address
  nfcRequired?: boolean; // require NFC (always true in our system)
  maxAmount?:  number;   // max ETH per payment
}

function getProvider() {
  // Fresh provider each call — avoids any background polling
  return new ethers.JsonRpcProvider(MAINNET_RPC);
}

export const ENSService = {
  /**
   * Resolve an ENS name → address + payment profile from text records.
   * Returns null if the name doesn't exist or resolution fails.
   */
  resolveProfile: async (ensName: string): Promise<ENSPaymentProfile | null> => {
    if (!ensName.includes('.')) return null;
    try {
      const provider = getProvider();
      const address = await provider.resolveName(ensName);
      if (!address) return null;

      const resolver = await provider.getResolver(ensName);
      const profile: ENSPaymentProfile = { ensName, address };

      if (resolver) {
        const values = await Promise.all(
          TEXT_KEYS.map(k => resolver.getText(k).catch(() => ''))
        );
        const [receiveAs, autoSplit, privacy, nfcRequired, maxAmount] = values;
        if (receiveAs)           profile.receiveAs   = receiveAs;
        if (autoSplit)           profile.autoSplit   = autoSplit;
        if (privacy === 'true')  profile.privacy     = true;
        if (nfcRequired === 'true') profile.nfcRequired = true;
        if (maxAmount)           profile.maxAmount   = parseFloat(maxAmount);
      }

      return profile;
    } catch {
      return null;
    }
  },

  /**
   * Reverse lookup: ETH address → ENS name (or null).
   */
  reverseResolve: async (address: string): Promise<string | null> => {
    try {
      const provider = getProvider();
      return await provider.lookupAddress(address);
    } catch {
      return null;
    }
  },

  /**
   * Parse auto-split config "savings.eth:20" → { target, percentage }.
   * Resolves ENS names in the target if needed.
   */
  parseSplit: async (autoSplit: string): Promise<{ targetAddress: string; percentage: number } | null> => {
    try {
      const parts = autoSplit.split(':');
      if (parts.length !== 2) return null;
      const [target, pctStr] = parts;
      const percentage = parseFloat(pctStr);
      if (isNaN(percentage) || percentage <= 0 || percentage >= 100) return null;

      let targetAddress = target.trim();
      if (targetAddress.includes('.')) {
        // It's an ENS name — resolve it
        const provider = getProvider();
        const resolved = await provider.resolveName(targetAddress);
        if (!resolved) return null;
        targetAddress = resolved;
      }

      return { targetAddress, percentage };
    } catch {
      return null;
    }
  },
};
