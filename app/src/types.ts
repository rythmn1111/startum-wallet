export interface EncryptedKeyBundle {
  ciphertext: string; // hex — the 64-byte AES-GCM ciphertext (eth_priv || sol_priv)
  iv:         string; // hex
  tag:        string; // hex
  salt:       string; // hex
}

// Result of splitting the combined encrypted key blob
export interface CombinedKeySplit {
  walletId:   string; // 16-char hex, written to NFC card
  nfcHalf:    string; // hex — XOR half stored on NFC
  serverHalf: string; // hex — XOR half stored on server
  bundle:     EncryptedKeyBundle;
  ethAddress: string;
  solAddress: string;
}

// What's physically written to the NFC card (compact JSON, base64-encoded nfcHalf)
export interface NFCCardPayload {
  w: string; // walletId (16-char hex)
  h: string; // base64(nfcHalf bytes) — compact for 137-byte NFC storage
}

// Response from GET /wallet/fetch/:walletId
export interface ServerWalletResponse {
  serverHalf: string; // hex
  salt:       string; // hex
  iv:         string; // hex
  tag:        string; // hex
  ethAddress: string;
  solAddress: string;
}
