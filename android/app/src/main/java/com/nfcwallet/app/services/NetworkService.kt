package com.nfcwallet.app.services

import android.content.Context
import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import com.nfcwallet.app.types.AuthRequest
import com.nfcwallet.app.types.AuthResponse
import com.nfcwallet.app.types.KeySplit
import com.nfcwallet.app.types.MyWalletsResponse
import com.nfcwallet.app.types.ServerKeyHalfResponse
import com.nfcwallet.app.types.StoreKeyHalfRequest
import com.nfcwallet.app.types.WalletEntry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okhttp3.logging.HttpLoggingInterceptor
import java.io.IOException
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class NetworkService(context: Context) {

    companion object {
        private const val BASE_URL = "http://62.146.173.162:3004"
        private const val PREFS_NAME = "nfc_wallet_prefs"
        private const val KEY_AUTH_TOKEN = "auth_token"
        private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()
    }

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val gson  = Gson()

    private val client: OkHttpClient = OkHttpClient.Builder()
        .addInterceptor(HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        })
        .build()

    // ─── Token management ─────────────────────────────────────────────────────

    var authToken: String?
        get()      = prefs.getString(KEY_AUTH_TOKEN, null)
        set(value) = if (value == null) {
            prefs.edit().remove(KEY_AUTH_TOKEN).apply()
        } else {
            prefs.edit().putString(KEY_AUTH_TOKEN, value).apply()
        }

    val isLoggedIn: Boolean get() = authToken != null

    // ─── Low-level helpers ────────────────────────────────────────────────────

    private fun buildAuthHeaders(builder: Request.Builder): Request.Builder {
        val token = authToken
        if (token != null) {
            builder.header("Authorization", "Bearer $token")
        }
        return builder
    }

    private suspend fun OkHttpClient.executeSuspend(request: Request): Response =
        suspendCancellableCoroutine { cont ->
            val call = newCall(request)
            cont.invokeOnCancellation { call.cancel() }
            call.enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    cont.resumeWithException(e)
                }
                override fun onResponse(call: Call, response: Response) {
                    cont.resume(response)
                }
            })
        }

    private suspend inline fun <reified T> post(endpoint: String, body: Any, requireAuth: Boolean = false): T =
        withContext(Dispatchers.IO) {
            val json        = gson.toJson(body)
            val requestBody = json.toRequestBody(JSON_MEDIA_TYPE)
            var builder     = Request.Builder()
                .url("$BASE_URL$endpoint")
                .post(requestBody)
            if (requireAuth) builder = buildAuthHeaders(builder)

            val response = client.executeSuspend(builder.build())
            val responseBody = response.body?.string()
                ?: throw IOException("Empty response body from $endpoint")

            if (!response.isSuccessful) {
                throw IOException("HTTP ${response.code} from $endpoint: $responseBody")
            }

            try {
                gson.fromJson(responseBody, T::class.java)
                    ?: throw IOException("Null JSON response from $endpoint")
            } catch (e: JsonSyntaxException) {
                throw IOException("Invalid JSON from $endpoint: $responseBody", e)
            }
        }

    private suspend inline fun <reified T> get(endpoint: String): T =
        withContext(Dispatchers.IO) {
            val builder = buildAuthHeaders(
                Request.Builder().url("$BASE_URL$endpoint").get()
            )
            val response     = client.executeSuspend(builder.build())
            val responseBody = response.body?.string()
                ?: throw IOException("Empty response body from $endpoint")

            if (!response.isSuccessful) {
                throw IOException("HTTP ${response.code} from $endpoint: $responseBody")
            }

            try {
                gson.fromJson(responseBody, T::class.java)
                    ?: throw IOException("Null JSON response from $endpoint")
            } catch (e: JsonSyntaxException) {
                throw IOException("Invalid JSON from $endpoint: $responseBody", e)
            }
        }

    // ─── Auth ─────────────────────────────────────────────────────────────────

    suspend fun register(email: String, password: String): AuthResponse {
        val response: AuthResponse = post("/auth/register", AuthRequest(email, password))
        authToken = response.token
        return response
    }

    suspend fun login(email: String, password: String): AuthResponse {
        val response: AuthResponse = post("/auth/login", AuthRequest(email, password))
        authToken = response.token
        return response
    }

    fun logout() {
        authToken = null
    }

    // ─── Wallet ───────────────────────────────────────────────────────────────

    suspend fun storeKeyHalf(split: KeySplit): Unit =
        withContext(Dispatchers.IO) {
            val requestBody = StoreKeyHalfRequest(
                chain         = split.chain.name,
                walletId      = split.walletId,
                serverKeyHalf = split.serverHalf,
                salt          = split.bundle.salt,
                iv            = split.bundle.iv,
                tag           = split.bundle.tag,
                publicAddress = split.publicAddress
            )
            val json = gson.toJson(requestBody)
            val req  = buildAuthHeaders(
                Request.Builder()
                    .url("$BASE_URL/wallet/store-key-half")
                    .post(json.toRequestBody(JSON_MEDIA_TYPE))
            ).build()

            val response = client.executeSuspend(req)
            val body     = response.body?.string() ?: ""
            if (!response.isSuccessful) {
                throw IOException("HTTP ${response.code} storing key half: $body")
            }
        }

    suspend fun fetchServerKeyHalf(walletId: String): ServerKeyHalfResponse =
        get("/wallet/key-half/$walletId")

    suspend fun fetchMyWallets(): List<WalletEntry> {
        val resp: MyWalletsResponse = get("/wallet/my-wallets")
        return resp.wallets
    }
}
