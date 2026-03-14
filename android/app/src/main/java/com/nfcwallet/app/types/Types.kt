package com.nfcwallet.app.types

import com.google.gson.annotations.SerializedName

// ─── Chain ───────────────────────────────────────────────────────────────────

enum class Chain { ETH, SOL }

// ─── Crypto types ────────────────────────────────────────────────────────────

/**
 * AES-256-GCM encrypted key bundle.
 * All fields are lowercase hex strings.
 */
data class EncryptedKeyBundle(
    @SerializedName("ciphertext") val ciphertext: String,  // hex, ciphertext only (no tag)
    @SerializedName("iv")         val iv: String,          // hex, 12 bytes
    @SerializedName("tag")        val tag: String,         // hex, 16 bytes
    @SerializedName("salt")       val salt: String         // hex, 16 bytes
)

/**
 * Result of XOR-splitting the ciphertext + metadata needed for server storage.
 */
data class KeySplit(
    val nfcHalf:       String,         // hex – half of ciphertext XOR'd
    val serverHalf:    String,         // hex – other half of ciphertext XOR'd
    val bundle:        EncryptedKeyBundle,
    val walletId:      String,         // UUID
    val publicAddress: String,
    val chain:         Chain
)

/**
 * What gets written to the NFC card as a JSON text record.
 */
data class NFCCardPayload(
    @SerializedName("walletId")      val walletId:      String,
    @SerializedName("chain")         val chain:         String,  // "ETH" or "SOL"
    @SerializedName("nfcHalf")       val nfcHalf:       String,  // hex
    @SerializedName("publicAddress") val publicAddress: String
)

// ─── Network / API types ─────────────────────────────────────────────────────

data class AuthRequest(
    @SerializedName("email")    val email:    String,
    @SerializedName("password") val password: String
)

data class AuthUser(
    @SerializedName("id")    val id:    String?,
    @SerializedName("email") val email: String?
)

data class AuthResponse(
    @SerializedName("token") val token: String,
    @SerializedName("user")  val user:  AuthUser?
)

data class StoreKeyHalfRequest(
    @SerializedName("chain")          val chain:          String,
    @SerializedName("walletId")       val walletId:       String,
    @SerializedName("serverKeyHalf")  val serverKeyHalf:  String,
    @SerializedName("salt")           val salt:           String,
    @SerializedName("iv")             val iv:             String,
    @SerializedName("tag")            val tag:            String,
    @SerializedName("publicAddress")  val publicAddress:  String
)

data class ServerKeyHalfResponse(
    @SerializedName("chain")          val chain:          String,
    @SerializedName("serverKeyHalf")  val serverKeyHalf:  String,
    @SerializedName("salt")           val salt:           String,
    @SerializedName("iv")             val iv:             String,
    @SerializedName("tag")            val tag:            String,
    @SerializedName("publicAddress")  val publicAddress:  String
)

data class WalletEntry(
    @SerializedName("chain")          val chain:          String,
    @SerializedName("walletId")       val walletId:       String,
    @SerializedName("publicAddress")  val publicAddress:  String
)

data class MyWalletsResponse(
    @SerializedName("wallets") val wallets: List<WalletEntry>
)

// ─── Wallet generation result ─────────────────────────────────────────────────

data class WalletResult(
    val mnemonic:       String,
    val ethPrivateKey:  ByteArray,
    val ethAddress:     String,
    val solPrivateKey:  ByteArray,    // 32-byte seed (expandable to 64-byte ed25519 keypair)
    val solAddress:     String        // Base58-encoded public key
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is WalletResult) return false
        return mnemonic == other.mnemonic &&
               ethPrivateKey.contentEquals(other.ethPrivateKey) &&
               ethAddress == other.ethAddress &&
               solPrivateKey.contentEquals(other.solPrivateKey) &&
               solAddress == other.solAddress
    }

    override fun hashCode(): Int {
        var result = mnemonic.hashCode()
        result = 31 * result + ethPrivateKey.contentHashCode()
        result = 31 * result + ethAddress.hashCode()
        result = 31 * result + solPrivateKey.contentHashCode()
        result = 31 * result + solAddress.hashCode()
        return result
    }
}
