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
}

interface AppContextValue extends AppState {
  login:               (token: string) => Promise<void>;
  logout:              () => Promise<void>;
  saveWalletAddresses: (eth: string, sol: string, wid: string) => Promise<void>;
  setCardVerified:     () => void;
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
  });

  // Restore session on app start — card always needs re-tap each session
  useEffect(() => {
    (async () => {
      const token = await AsyncStorage.getItem('authToken');
      if (!token) return;
      try {
        const { wallet } = await NetworkService.fetchMyWallet();
        setState({
          isLoggedIn:     true,
          hasWallet:      !!wallet,
          isCardVerified: false, // always false on fresh app open
          authToken:      token,
          ethAddress:     wallet?.ethAddress ?? '',
          solAddress:     wallet?.solAddress ?? '',
          walletId:       wallet?.walletId   ?? '',
        });
      } catch {
        setState(s => ({ ...s, isLoggedIn: true, authToken: token }));
      }
    })();
  }, []);

  const login = async (token: string) => {
    await AsyncStorage.setItem('authToken', token);
    try {
      const { wallet } = await NetworkService.fetchMyWallet();
      setState({
        isLoggedIn:     true,
        hasWallet:      !!wallet,
        isCardVerified: false, // card tap required after every login
        authToken:      token,
        ethAddress:     wallet?.ethAddress ?? '',
        solAddress:     wallet?.solAddress ?? '',
        walletId:       wallet?.walletId   ?? '',
      });
    } catch {
      setState(s => ({ ...s, isLoggedIn: true, authToken: token, isCardVerified: false }));
    }
  };

  const logout = async () => {
    await AsyncStorage.removeItem('authToken');
    setState({
      isLoggedIn: false, hasWallet: false, isCardVerified: false,
      authToken: null, ethAddress: '', solAddress: '', walletId: '',
    });
  };

  const saveWalletAddresses = async (eth: string, sol: string, wid: string) => {
    setState(s => ({ ...s, hasWallet: true, isCardVerified: true, ethAddress: eth, solAddress: sol, walletId: wid }));
  };

  const setCardVerified = () => {
    setState(s => ({ ...s, isCardVerified: true }));
  };

  return (
    <AppContext.Provider value={{ ...state, login, logout, saveWalletAddresses, setCardVerified }}>
      {children}
    </AppContext.Provider>
  );
}

export const useApp = () => {
  const ctx = useContext(AppContext);
  if (!ctx) throw new Error('useApp must be inside AppProvider');
  return ctx;
};
