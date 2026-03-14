const router = require('express').Router();
const db = require('../db');
const { requireAuth } = require('../middleware/auth');

// POST /wallet/store
// Called during wallet setup. Stores the server half of the combined encrypted key blob.
// Body: { walletId, serverHalf, salt, iv, tag, ethAddress, solAddress }
router.post('/store', requireAuth, async (req, res) => {
  const { walletId, serverHalf, salt, iv, tag, ethAddress, solAddress } = req.body;
  if (!walletId || !serverHalf || !salt || !iv || !tag || !ethAddress || !solAddress) {
    return res.status(400).json({ error: 'Missing required fields' });
  }
  try {
    await db.query(
      `INSERT INTO wallets (user_id, wallet_id, server_half, salt, iv, tag, eth_address, sol_address)
       VALUES ($1, $2, $3, $4, $5, $6, $7, $8)
       ON CONFLICT (user_id) DO UPDATE SET
         wallet_id   = EXCLUDED.wallet_id,
         server_half = EXCLUDED.server_half,
         salt        = EXCLUDED.salt,
         iv          = EXCLUDED.iv,
         tag         = EXCLUDED.tag,
         eth_address = EXCLUDED.eth_address,
         sol_address = EXCLUDED.sol_address`,
      [req.user.sub, walletId, Buffer.from(serverHalf, 'hex'), salt, iv, tag, ethAddress, solAddress]
    );
    res.status(201).json({ success: true });
  } catch (err) {
    console.error(err);
    res.status(500).json({ error: 'Failed to store wallet' });
  }
});

// GET /wallet/fetch/:walletId
// Called during payment. Returns the server half for the given walletId.
// walletId comes from the NFC card scan.
router.get('/fetch/:walletId', requireAuth, async (req, res) => {
  try {
    const result = await db.query(
      'SELECT server_half, salt, iv, tag, eth_address, sol_address FROM wallets WHERE wallet_id = $1',
      [req.params.walletId]
    );
    if (result.rows.length === 0) {
      return res.status(404).json({ error: 'Wallet not found' });
    }
    const row = result.rows[0];
    res.json({
      serverHalf:  row.server_half.toString('hex'),
      salt:        row.salt,
      iv:          row.iv,
      tag:         row.tag,
      ethAddress:  row.eth_address,
      solAddress:  row.sol_address,
    });
  } catch (err) {
    console.error(err);
    res.status(500).json({ error: 'Failed to fetch wallet' });
  }
});

// GET /wallet/me
// Returns the current user's wallet public addresses (non-sensitive).
router.get('/me', requireAuth, async (req, res) => {
  try {
    const result = await db.query(
      'SELECT wallet_id, eth_address, sol_address, created_at FROM wallets WHERE user_id = $1',
      [req.user.sub]
    );
    if (result.rows.length === 0) {
      return res.json({ wallet: null });
    }
    const row = result.rows[0];
    res.json({
      wallet: {
        walletId:   row.wallet_id,
        ethAddress: row.eth_address,
        solAddress: row.sol_address,
        createdAt:  row.created_at,
      },
    });
  } catch (err) {
    console.error(err);
    res.status(500).json({ error: 'Failed to fetch wallet' });
  }
});

module.exports = router;
