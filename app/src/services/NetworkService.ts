import AsyncStorage from '@react-native-async-storage/async-storage';
import type { CombinedKeySplit, ServerWalletResponse } from '../types';

const BASE_URL = 'http://62.146.173.162:3004';

async function getToken(): Promise<string | null> {
  return AsyncStorage.getItem('authToken');
}

async function request<T>(path: string, options: RequestInit = {}, auth = true): Promise<T> {
  const headers: Record<string, string> = { 'Content-Type': 'application/json' };
  if (auth) {
    const token = await getToken();
    if (token) headers['Authorization'] = `Bearer ${token}`;
  }
  const res = await fetch(`${BASE_URL}${path}`, { ...options, headers });
  const json = await res.json();
  if (!res.ok) throw new Error(json.error ?? `HTTP ${res.status}`);
  return json as T;
}

export interface AuthResponse {
  token: string;
  user: { id: string; username: string };
}

export const NetworkService = {
  register: (username: string, password: string) =>
    request<AuthResponse>(
      '/auth/register',
      { method: 'POST', body: JSON.stringify({ username, password }) },
      false,
    ),

  login: (username: string, password: string) =>
    request<AuthResponse>(
      '/auth/login',
      { method: 'POST', body: JSON.stringify({ username, password }) },
      false,
    ),

  storeWallet: (split: CombinedKeySplit) =>
    request<{ success: boolean }>('/wallet/store', {
      method: 'POST',
      body: JSON.stringify({
        walletId:   split.walletId,
        serverHalf: split.serverHalf,
        salt:       split.bundle.salt,
        iv:         split.bundle.iv,
        tag:        split.bundle.tag,
        ethAddress: split.ethAddress,
        solAddress: split.solAddress,
      }),
    }),

  fetchWallet: (walletId: string) =>
    request<ServerWalletResponse>(`/wallet/fetch/${walletId}`),

  fetchMyWallet: () =>
    request<{ wallet: { walletId: string; ethAddress: string; solAddress: string; createdAt: string } | null }>(
      '/wallet/me'
    ),
};
