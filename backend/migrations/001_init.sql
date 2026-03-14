-- splitkey_wallet database schema
-- Combined ETH+SOL key storage with XOR-split ciphertext

CREATE EXTENSION IF NOT EXISTS "pgcrypto";

CREATE TABLE users (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    username      TEXT UNIQUE NOT NULL,
    password_hash TEXT NOT NULL,
    created_at    TIMESTAMPTZ DEFAULT NOW()
);

-- One row per user wallet.
-- NFC card stores: nfcHalf (XOR of ciphertext) + walletId.
-- This table stores: serverHalf + iv/tag/salt for decryption.
-- XOR(nfcHalf, serverHalf) = AES-GCM ciphertext
-- decrypt(ciphertext, password) = eth_priv(32 bytes) || sol_priv(32 bytes)
CREATE TABLE wallets (
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id      UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    wallet_id    TEXT NOT NULL UNIQUE,   -- 16-char hex, written to NFC card
    server_half  BYTEA NOT NULL,         -- 64-byte XOR server half of ciphertext
    salt         TEXT NOT NULL,          -- PBKDF2 salt (hex)
    iv           TEXT NOT NULL,          -- AES-GCM IV (hex)
    tag          TEXT NOT NULL,          -- AES-GCM auth tag (hex)
    eth_address  TEXT NOT NULL,
    sol_address  TEXT NOT NULL,
    created_at   TIMESTAMPTZ DEFAULT NOW(),
    UNIQUE(user_id)
);

CREATE INDEX idx_wallets_wallet_id ON wallets(wallet_id);
