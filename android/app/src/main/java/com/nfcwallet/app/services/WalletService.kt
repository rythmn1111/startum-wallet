package com.nfcwallet.app.services

import com.nfcwallet.app.services.CryptoService.hexToBytes
import com.nfcwallet.app.services.CryptoService.toHex
import com.nfcwallet.app.types.Chain
import com.nfcwallet.app.types.EncryptedKeyBundle
import com.nfcwallet.app.types.KeySplit
import com.nfcwallet.app.types.WalletResult
import net.i2p.crypto.eddsa.EdDSAPublicKey
import net.i2p.crypto.eddsa.spec.EdDSANamedCurveTable
import net.i2p.crypto.eddsa.spec.EdDSAPublicKeySpec
import org.web3j.crypto.Bip32ECKeyPair
import org.web3j.crypto.Credentials
import org.web3j.crypto.MnemonicUtils
import org.web3j.utils.Numeric
import java.math.BigInteger
import java.util.UUID
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

object WalletService {

    // ─── BIP-44 derivation paths ──────────────────────────────────────────────

    // ETH:  m/44'/60'/0'/0/0
    private val ETH_PATH = intArrayOf(
        Bip32ECKeyPair.HARDENED_BIT or 44,
        Bip32ECKeyPair.HARDENED_BIT or 60,
        Bip32ECKeyPair.HARDENED_BIT or 0,
        0,
        0
    )

    // SOL:  m/44'/501'/0'/0'   (SLIP-0010 / ed25519 – all hardened)
    private val SOL_PATH = intArrayOf(44, 501, 0, 0)   // hardened flag applied inside slip10

    // ─── Base58 alphabet (Bitcoin / Solana) ───────────────────────────────────

    private const val BASE58_ALPHABET =
        "123456789ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz"

    fun base58Encode(input: ByteArray): String {
        // Count leading zeros
        var leadingZeros = 0
        for (b in input) {
            if (b == 0.toByte()) leadingZeros++ else break
        }

        // Convert to BigInteger
        var intVal = BigInteger(1, input)
        val base   = BigInteger.valueOf(58)
        val sb     = StringBuilder()

        while (intVal > BigInteger.ZERO) {
            val (quotient, remainder) = intVal.divideAndRemainder(base)
            sb.append(BASE58_ALPHABET[remainder.toInt()])
            intVal = quotient
        }

        repeat(leadingZeros) { sb.append(BASE58_ALPHABET[0]) }
        return sb.reverse().toString()
    }

    fun base58Decode(input: String): ByteArray {
        var intVal  = BigInteger.ZERO
        val base    = BigInteger.valueOf(58)

        for (ch in input) {
            val idx = BASE58_ALPHABET.indexOf(ch)
            require(idx >= 0) { "Invalid Base58 character: $ch" }
            intVal = intVal.multiply(base).add(BigInteger.valueOf(idx.toLong()))
        }

        var leadingZeros = 0
        for (ch in input) {
            if (ch == BASE58_ALPHABET[0]) leadingZeros++ else break
        }

        val decoded       = intVal.toByteArray()
        val stripSignByte = if (decoded.isNotEmpty() && decoded[0] == 0.toByte()) 1 else 0
        val result        = ByteArray(leadingZeros + decoded.size - stripSignByte)
        decoded.copyInto(result, leadingZeros, stripSignByte)
        return result
    }

    // ─── SLIP-0010 ed25519 derivation ─────────────────────────────────────────

    /**
     * Derives an ed25519 key via SLIP-0010.
     * Every level is hardened (required by ed25519 in SLIP-0010).
     *
     * @param seed    64-byte BIP39 seed
     * @param path    array of unhardened indices (hardened flag added internally)
     * @return 32-byte private key scalar
     */
    fun slip10DeriveEd25519(seed: ByteArray, path: IntArray): ByteArray {
        val hmac = Mac.getInstance("HmacSHA512")

        // Master key: HMAC-SHA512( key = "ed25519 seed", data = seed )
        hmac.init(SecretKeySpec("ed25519 seed".toByteArray(Charsets.UTF_8), "HmacSHA512"))
        var il = hmac.doFinal(seed)   // 64 bytes: IL (key) || IR (chain code)

        var privKey   = il.copyOfRange(0, 32)
        var chainCode = il.copyOfRange(32, 64)

        for (index in path) {
            // All indices are hardened in ed25519 SLIP-0010
            val hardenedIndex = index or 0x80000000.toInt()

            // data = 0x00 || privKey || ser32(index)
            val data = ByteArray(37)
            data[0] = 0x00
            privKey.copyInto(data, 1)
            data[33] = ((hardenedIndex ushr 24) and 0xFF).toByte()
            data[34] = ((hardenedIndex ushr 16) and 0xFF).toByte()
            data[35] = ((hardenedIndex ushr  8) and 0xFF).toByte()
            data[36] = ((hardenedIndex       ) and 0xFF).toByte()

            hmac.init(SecretKeySpec(chainCode, "HmacSHA512"))
            il        = hmac.doFinal(data)
            privKey   = il.copyOfRange(0, 32)
            chainCode = il.copyOfRange(32, 64)
        }

        return privKey   // 32-byte ed25519 private key seed
    }

    // ─── ed25519 public key from private key seed ─────────────────────────────

    private fun ed25519PublicKey(privateKeySeed: ByteArray): ByteArray {
        val params  = EdDSANamedCurveTable.getByName("ed25519")
        val pubSpec = EdDSAPublicKeySpec(privateKeySeed, params)
        val pubKey  = EdDSAPublicKey(pubSpec)
        return pubKey.abyte   // 32-byte compressed public key
    }

    // ─── generateWallets ──────────────────────────────────────────────────────

    /**
     * Generates a new BIP39 mnemonic and derives ETH + SOL keys.
     */
    fun generateWallets(): WalletResult {
        // 1. BIP39 mnemonic (128-bit entropy → 12 words)
        val mnemonic = MnemonicUtils.generateMnemonic(
            ByteArray(16).also { java.security.SecureRandom().nextBytes(it) }
        )

        // 2. BIP39 seed (no passphrase)
        val seed = MnemonicUtils.generateSeed(mnemonic, "")

        // 3. ETH key via BIP-32 (secp256k1)
        val masterKey = Bip32ECKeyPair.generateKeyPair(seed)
        val ethKey    = Bip32ECKeyPair.deriveKeyPair(masterKey, ETH_PATH)
        val creds     = Credentials.create(ethKey)
        // Extract 32-byte private key from BigInteger (zero-pad on left to 32 bytes)
        val ethPrivBig = ethKey.privateKey  // BigInteger (from ECKeyPair)
        val ethPriv    = Numeric.toBytesPadded(ethPrivBig, 32)

        // 4. SOL key via SLIP-0010 / ed25519
        val solPriv   = slip10DeriveEd25519(seed, SOL_PATH)
        val solPubKey = ed25519PublicKey(solPriv)
        val solAddr   = base58Encode(solPubKey)

        return WalletResult(
            mnemonic       = mnemonic,
            ethPrivateKey  = ethPriv,
            ethAddress     = creds.address,
            solPrivateKey  = solPriv,
            solAddress     = solAddr
        )
    }

    // ─── splitKey ─────────────────────────────────────────────────────────────

    /**
     * Encrypts a private key and XOR-splits the ciphertext into two halves.
     *
     * @param chain          ETH or SOL
     * @param privateKey     raw private key bytes
     * @param publicAddress  the corresponding public address
     * @param password       user-supplied password for AES-GCM encryption
     */
    fun splitKey(
        chain: Chain,
        privateKey: ByteArray,
        publicAddress: String,
        password: String
    ): KeySplit {
        val bundle    = CryptoService.encrypt(privateKey, password)
        val cipherBytes = bundle.ciphertext.hexToBytes()
        val (nfcHalf, serverHalf) = CryptoService.xorSplit(cipherBytes)

        return KeySplit(
            nfcHalf       = nfcHalf,
            serverHalf    = serverHalf,
            bundle        = bundle,
            walletId      = UUID.randomUUID().toString(),
            publicAddress = publicAddress,
            chain         = chain
        )
    }

    // ─── reconstructPrivateKey ────────────────────────────────────────────────

    /**
     * XOR-combines the two halves to get the ciphertext, then AES-decrypts to recover
     * the private key bytes.
     *
     * @param nfcHalfHex   the half stored on the NFC card (hex)
     * @param serverBundle the [ServerKeyHalfResponse] from the backend
     * @param password     user's password
     */
    fun reconstructPrivateKey(
        nfcHalfHex: String,
        serverHalf: String,
        salt: String,
        iv: String,
        tag: String,
        password: String
    ): ByteArray {
        val cipherBytes = CryptoService.xorCombine(nfcHalfHex, serverHalf)
        val bundle      = EncryptedKeyBundle(
            ciphertext = cipherBytes.toHex(),
            iv         = iv,
            tag        = tag,
            salt       = salt
        )
        return CryptoService.decrypt(bundle, password)
    }
}
