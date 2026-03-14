import React, { createContext, useContext, useState, useEffect } from 'react';
import AsyncStorage from '@react-native-async-storage/async-storage';
import { NetworkService } from '../services/NetworkService';

interface AppState {
  isLoggedIn:     boolean;
  hasWallet:      boolean;
  isCardVerified: boolean; // must tap NFC card once per session after login
  authToken:      string | null;
  ethAddress:     string;
  solAddress:     string;
  walletId:       string; // server walletId — used to verify NFC card tap
  ensName:        string; // user's ENS name e.g. "rythmn.eth" (optional)
}

interface AppContextValue extends AppState {
  login:               (token: string) => Promise<void>;
  logout:              () => Promise<void>;
  saveWalletAddresses: (eth: string, sol: string, wid: string) => Promise<void>;
  setCardVerified:     () => void;
  setEnsName:          (name: string) => Promise<void>;
}

const AppContext = createContext<AppContextValue | null>(null);

export function AppProvider({ children }: { children: React.ReactNode }) {
  const [state, setState] = useState<AppState>({
    isLoggedIn:     false,
    hasWallet:      false,
    isCardVerified: false,
    authToken:      null,
    ethAddress:     '',
    solAddress:     '',
    walletId:       '',
    ensName:        '',
  });

  // Restore session on app start — card always needs re-tap each session
  useEffect(() => {
    (async () => {
      const token   = await AsyncStorage.getItem('authToken');
      const ensName = await AsyncStorage.getItem('ensName') ?? '';
      if (!token) return;
      try {
        const { wallet } = await NetworkService.fetchMyWallet();
        setState({
          isLoggedIn:     true,
          hasWallet:      !!wallet,
          isCardVerified: false,
          authToken:      token,
          ethAddress:     wallet?.ethAddress ?? '',
          solAddress:     wallet?.solAddress ?? '',
          walletId:       wallet?.walletId   ?? '',
          ensName,
        });
      } catch {
        setState(s => ({ ...s, isLoggedIn: true, authToken: token, ensName }));
      }
    })();
  }, []);

  const login = async (token: string) => {
    await AsyncStorage.setItem('authToken', token);
    const ensName = await AsyncStorage.getItem('ensName') ?? '';
    try {
      const { wallet } = await NetworkService.fetchMyWallet();
      setState({
        isLoggedIn:     true,
        hasWallet:      !!wallet,
        isCardVerified: false,
        authToken:      token,
        ethAddress:     wallet?.ethAddress ?? '',
        solAddress:     wallet?.solAddress ?? '',
        walletId:       wallet?.walletId   ?? '',
        ensName,
      });
    } catch {
      setState(s => ({ ...s, isLoggedIn: true, authToken: token, isCardVerified: false, ensName }));
    }
  };

  const logout = async () => {
    await AsyncStorage.removeItem('authToken');
    setState({
      isLoggedIn: false, hasWallet: false, isCardVerified: false,
      authToken: null, ethAddress: '', solAddress: '', walletId: '', ensName: '',
    });
  };

  const setEnsName = async (name: string) => {
    await AsyncStorage.setItem('ensName', name);
    setState(s => ({ ...s, ensName: name }));
  };

  const saveWalletAddresses = async (eth: string, sol: string, wid: string) => {
    setState(s => ({ ...s, hasWallet: true, isCardVerified: true, ethAddress: eth, solAddress: sol, walletId: wid }));
  };

  const setCardVerified = () => {
    setState(s => ({ ...s, isCardVerified: true }));
  };

  return (
    <AppContext.Provider value={{ ...state, login, logout, saveWalletAddresses, setCardVerified, setEnsName }}>
      {children}
    </AppContext.Provider>
  );
}

export const useApp = () => {
  const ctx = useContext(AppContext);
  if (!ctx) throw new Error('useApp must be inside AppProvider');
  return ctx;
};
