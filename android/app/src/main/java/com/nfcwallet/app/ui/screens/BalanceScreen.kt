package com.nfcwallet.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.gson.Gson
import com.nfcwallet.app.AppViewModel
import com.nfcwallet.app.ui.theme.DarkBackground
import com.nfcwallet.app.ui.theme.DarkCard
import com.nfcwallet.app.ui.theme.OnDarkMuted
import com.nfcwallet.app.ui.theme.PurpleAccent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import java.io.IOException
import java.math.BigDecimal
import java.math.BigInteger
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

private data class BalanceState(
    val ethBalance: String? = null,
    val solBalance: String? = null,
    val isLoading:  Boolean = false,
    val error:      String? = null
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BalanceScreen(viewModel: AppViewModel) {

    val scope      = rememberCoroutineScope()
    val ethAddress by viewModel.ethAddress.collectAsState()
    val solAddress by viewModel.solAddress.collectAsState()
    var state      by remember { mutableStateOf(BalanceState()) }

    fun fetchBalances() {
        state = state.copy(isLoading = true, error = null)
        scope.launch {
            try {
                val (eth, sol) = withContext(Dispatchers.IO) {
                    Pair(
                        fetchEthBalance(ethAddress),
                        fetchSolBalance(solAddress)
                    )
                }
                state = state.copy(
                    ethBalance = eth,
                    solBalance = sol,
                    isLoading  = false
                )
            } catch (e: Exception) {
                state = state.copy(isLoading = false, error = e.message)
            }
        }
    }

    LaunchedEffect(ethAddress, solAddress) {
        if (ethAddress.isNotBlank() && solAddress.isNotBlank()) fetchBalances()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFF0F0C29), Color(0xFF302B63), Color(0xFF24243E))
                )
            )
    ) {
        // ── Top bar ───────────────────────────────────────────────────────────
        TopAppBar(
            title = {
                Text(
                    "Balance",
                    style = MaterialTheme.typography.titleLarge.copy(
                        color = Color.White, fontWeight = FontWeight.Bold
                    )
                )
            },
            actions = {
                IconButton(onClick = { fetchBalances() }) {
                    Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = PurpleAccent)
                }
                IconButton(onClick = { viewModel.logout() }) {
                    Icon(Icons.Default.Logout, contentDescription = "Sign Out", tint = PurpleAccent)
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
        )

        // ── Content ───────────────────────────────────────────────────────────
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(12.dp))

            if (state.isLoading) {
                Spacer(Modifier.height(48.dp))
                CircularProgressIndicator(color = PurpleAccent, modifier = Modifier.size(48.dp))
            }

            if (state.error != null) {
                Spacer(Modifier.height(24.dp))
                Text(
                    text  = "Error: ${state.error}",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center
                )
            }

            // ETH card
            BalanceCard(
                label    = "Ethereum",
                ticker   = "ETH",
                balance  = state.ethBalance,
                address  = ethAddress,
                color    = Color(0xFF6B7280)
            )

            Spacer(Modifier.height(16.dp))

            // SOL card
            BalanceCard(
                label    = "Solana",
                ticker   = "SOL",
                balance  = state.solBalance,
                address  = solAddress,
                color    = Color(0xFF9945FF)
            )

            Spacer(Modifier.height(32.dp))
        }
    }
}

// ─── Balance card ─────────────────────────────────────────────────────────────

@Composable
private fun BalanceCard(
    label:   String,
    ticker:  String,
    balance: String?,
    address: String,
    color:   Color
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(DarkCard, RoundedCornerShape(16.dp))
            .padding(20.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(color.copy(alpha = 0.2f), RoundedCornerShape(50.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text  = ticker[0].toString(),
                    color = color,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            }
            Spacer(Modifier.padding(8.dp))
            Column {
                Text(label, style = MaterialTheme.typography.titleMedium.copy(color = Color.White, fontWeight = FontWeight.Bold))
                Text(ticker, style = MaterialTheme.typography.bodySmall.copy(color = OnDarkMuted))
            }
        }

        Spacer(Modifier.height(16.dp))

        Text(
            text  = if (balance != null) "$balance $ticker" else "—",
            style = MaterialTheme.typography.headlineSmall.copy(
                color      = Color.White,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
        )

        Spacer(Modifier.height(8.dp))

        Text(
            text  = address,
            style = MaterialTheme.typography.labelSmall.copy(
                color      = OnDarkMuted,
                fontFamily = FontFamily.Monospace
            ),
            maxLines = 1
        )
    }
}

// ─── Balance fetchers ─────────────────────────────────────────────────────────

private val httpClient = OkHttpClient()
private val gson       = Gson()
private val JSON_MT    = "application/json".toMediaType()

private suspend fun OkHttpClient.executeSuspend(request: Request): Response =
    suspendCancellableCoroutine { cont ->
        val call = newCall(request)
        cont.invokeOnCancellation { call.cancel() }
        call.enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) = cont.resumeWithException(e)
            override fun onResponse(call: Call, response: Response) = cont.resume(response)
        })
    }

private suspend fun fetchEthBalance(address: String): String {
    if (address.isBlank()) return "0.0000"

    val body = mapOf(
        "jsonrpc" to "2.0",
        "method"  to "eth_getBalance",
        "params"  to listOf(address, "latest"),
        "id"      to 1
    )
    val request = Request.Builder()
        .url("https://mainnet.infura.io/v3/9aa3d95b3bc440fa88ea12eaa4456161")
        .post(gson.toJson(body).toRequestBody(JSON_MT))
        .build()

    val response = httpClient.executeSuspend(request)
    val text     = response.body?.string() ?: throw IOException("Empty response")
    val map      = gson.fromJson(text, Map::class.java)
    val hexWei   = (map["result"] as? String) ?: "0x0"

    // Convert hex wei → ETH
    val wei = BigInteger(hexWei.removePrefix("0x"), 16)
    val eth = BigDecimal(wei).divide(BigDecimal("1000000000000000000"))
    return "%.6f".format(eth.toDouble())
}

private suspend fun fetchSolBalance(address: String): String {
    if (address.isBlank()) return "0.0000"

    val body = mapOf(
        "jsonrpc" to "2.0",
        "method"  to "getBalance",
        "params"  to listOf(address),
        "id"      to 1
    )
    val request = Request.Builder()
        .url("https://api.mainnet-beta.solana.com")
        .post(gson.toJson(body).toRequestBody(JSON_MT))
        .build()

    val response = httpClient.executeSuspend(request)
    val text     = response.body?.string() ?: throw IOException("Empty response")
    val map      = gson.fromJson(text, Map::class.java)

    @Suppress("UNCHECKED_CAST")
    val resultMap = map["result"] as? Map<String, Any>
    val lamports  = (resultMap?.get("value") as? Double)?.toLong() ?: 0L
    val sol       = lamports / 1_000_000_000.0
    return "%.6f".format(sol)
}
