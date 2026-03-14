package com.nfcwallet.app.services

import com.nfcwallet.app.types.EncryptedKeyBundle
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

object CryptoService {

    private const val AES_GCM_TRANSFORMATION = "AES/GCM/NoPadding"
    private const val GCM_TAG_LENGTH_BITS     = 128
    private const val GCM_IV_LENGTH_BYTES     = 12
    private const val SALT_LENGTH_BYTES       = 16
    private const val KEY_LENGTH_BITS         = 256
    private const val PBKDF2_ITERATIONS       = 310_000
    private const val PBKDF2_ALGORITHM        = "PBKDF2WithHmacSHA256"

    // ─── Hex helpers ──────────────────────────────────────────────────────────

    fun ByteArray.toHex(): String = joinToString("") { "%02x".format(it) }

    fun String.hexToBytes(): ByteArray {
        require(length % 2 == 0) { "Hex string must have even length" }
        return ByteArray(length / 2) { i ->
            substring(i * 2, i * 2 + 2).toInt(16).toByte()
        }
    }

    // ─── Key derivation ───────────────────────────────────────────────────────

    private fun deriveKey(password: String, salt: ByteArray): SecretKeySpec {
        val spec = PBEKeySpec(
            password.toCharArray(),
            salt,
            PBKDF2_ITERATIONS,
            KEY_LENGTH_BITS
        )
        val factory = SecretKeyFactory.getInstance(PBKDF2_ALGORITHM)
        val keyBytes = factory.generateSecret(spec).encoded
        return SecretKeySpec(keyBytes, "AES")
    }

    // ─── AES-256-GCM encrypt ──────────────────────────────────────────────────

    /**
     * Encrypts [plaintext] with AES-256-GCM using a PBKDF2-derived key from [password].
     * Returns an [EncryptedKeyBundle] where:
     *   - [EncryptedKeyBundle.ciphertext] = ciphertext bytes only (tag is stripped off)
     *   - [EncryptedKeyBundle.tag]        = 16-byte GCM authentication tag (hex)
     *   - [EncryptedKeyBundle.iv]         = 12-byte random IV (hex)
     *   - [EncryptedKeyBundle.salt]       = 16-byte random PBKDF2 salt (hex)
     */
    fun encrypt(plaintext: ByteArray, password: String): EncryptedKeyBundle {
        val random = SecureRandom()

        val salt = ByteArray(SALT_LENGTH_BYTES).also { random.nextBytes(it) }
        val iv   = ByteArray(GCM_IV_LENGTH_BYTES).also { random.nextBytes(it) }
        val key  = deriveKey(password, salt)

        val cipher = Cipher.getInstance(AES_GCM_TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, key, GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv))

        // AES/GCM appends the 16-byte tag at the end of the output
        val rawOutput  = cipher.doFinal(plaintext)
        val tagOffset  = rawOutput.size - (GCM_TAG_LENGTH_BITS / 8)
        val ciphertext = rawOutput.copyOfRange(0, tagOffset)
        val tag        = rawOutput.copyOfRange(tagOffset, rawOutput.size)

        return EncryptedKeyBundle(
            ciphertext = ciphertext.toHex(),
            iv         = iv.toHex(),
            tag        = tag.toHex(),
            salt       = salt.toHex()
        )
    }

    // ─── AES-256-GCM decrypt ──────────────────────────────────────────────────

    /**
     * Decrypts [bundle] using the password.
     * Re-assembles ciphertext+tag before passing to GCM so the auth tag is verified.
     */
    fun decrypt(bundle: EncryptedKeyBundle, password: String): ByteArray {
        val salt       = bundle.salt.hexToBytes()
        val iv         = bundle.iv.hexToBytes()
        val ciphertext = bundle.ciphertext.hexToBytes()
        val tag        = bundle.tag.hexToBytes()

        val key = deriveKey(password, salt)

        // GCM expects ciphertext || tag concatenated
        val ciphertextWithTag = ciphertext + tag

        val cipher = Cipher.getInstance(AES_GCM_TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv))

        return cipher.doFinal(ciphertextWithTag)
    }

    // ─── XOR split / combine ─────────────────────────────────────────────────

    /**
     * XOR-splits [data] into two equal-length byte arrays using a random mask.
     * Returns (nfcHalf hex, serverHalf hex).
     */
    fun xorSplit(data: ByteArray): Pair<String, String> {
        val mask      = ByteArray(data.size).also { SecureRandom().nextBytes(it) }
        val otherHalf = ByteArray(data.size) { i -> (data[i].toInt() xor mask[i].toInt()).toByte() }
        return Pair(mask.toHex(), otherHalf.toHex())
    }

    /**
     * Reconstructs the original data from two XOR halves.
     */
    fun xorCombine(hex1: String, hex2: String): ByteArray {
        val b1 = hex1.hexToBytes()
        val b2 = hex2.hexToBytes()
        require(b1.size == b2.size) { "XOR halves must have the same length" }
        return ByteArray(b1.size) { i -> (b1[i].toInt() xor b2[i].toInt()).toByte() }
    }
}
