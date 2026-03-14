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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Nfc
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.gson.Gson
import com.nfcwallet.app.AppViewModel
import com.nfcwallet.app.services.NFCService
import com.nfcwallet.app.services.WalletService
import com.nfcwallet.app.ui.theme.DarkCard
import com.nfcwallet.app.ui.theme.OnDarkMuted
import com.nfcwallet.app.ui.theme.PurpleAccent
import com.nfcwallet.app.ui.theme.SuccessGreen
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import net.i2p.crypto.eddsa.EdDSAEngine
import net.i2p.crypto.eddsa.EdDSAPrivateKey
import net.i2p.crypto.eddsa.spec.EdDSANamedCurveTable
import net.i2p.crypto.eddsa.spec.EdDSAPrivateKeySpec
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import org.web3j.crypto.Credentials
import org.web3j.crypto.RawTransaction
import org.web3j.crypto.TransactionEncoder
import org.web3j.protocol.Web3j
import org.web3j.protocol.core.DefaultBlockParameterName
import org.web3j.protocol.http.HttpService
import org.web3j.utils.Convert
import org.web3j.utils.Numeric
import java.io.IOException
import java.math.BigDecimal
import java.math.BigInteger
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.security.MessageDigest
import java.security.Signature
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

private enum class SendStep {
    ENTER_DETAILS,
    WAITING_NFC,
    ENTER_PASSWORD,
    DONE
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SendScreen(viewModel: AppViewModel) {

    val scope = rememberCoroutineScope()
    val ethAddress by viewModel.ethAddress.collectAsState()
    val solAddress by viewModel.solAddress.collectAsState()

    var selectedChain  by remember { mutableStateOf(0) }  // 0 = ETH, 1 = SOL
    var step           by remember { mutableStateOf(SendStep.ENTER_DETAILS) }
    var toAddress      by remember { mutableStateOf("") }
    var amount         by remember { mutableStateOf("") }
    var password       by remember { mutableStateOf("") }
    var nfcHalf        by remember { mutableStateOf("") }
    var nfcWalletId    by remember { mutableStateOf("") }
    var txHash         by remember { mutableStateOf("") }
    var isLoading      by remember { mutableStateOf(false) }
    var errorMessage   by remember { mutableStateOf<String?>(null) }

    val chainLabel = if (selectedChain == 0) "ETH" else "SOL"

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFF0F0C29), Color(0xFF302B63), Color(0xFF24243E))
                )
            )
    ) {
        TopAppBar(
            title = {
                Text(
                    "Send",
                    style = MaterialTheme.typography.titleLarge.copy(
                        color = Color.White, fontWeight = FontWeight.Bold
                    )
                )
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
        )

        // ── Chain tab row ─────────────────────────────────────────────────────
        if (step == SendStep.ENTER_DETAILS) {
            TabRow(
                selectedTabIndex = selectedChain,
                containerColor   = DarkCard,
                contentColor     = PurpleAccent,
                indicator        = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        Modifier.tabIndicatorOffset(tabPositions[selectedChain]),
                        color = PurpleAccent
                    )
                }
            ) {
                listOf("ETH", "SOL").forEachIndexed { index, title ->
                    Tab(
                        selected = selectedChain == index,
                        onClick  = { selectedChain = index; errorMessage = null },
                        text = {
                            Text(
                                title,
                                color = if (selectedChain == index) PurpleAccent else OnDarkMuted,
                                fontWeight = if (selectedChain == index) FontWeight.SemiBold else FontWeight.Normal
                            )
                        }
                    )
                }
            }
        }

        // ── Steps ─────────────────────────────────────────────────────────────
        when (step) {

            // Step 1: Enter to address + amount
            SendStep.ENTER_DETAILS -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 24.dp)
                        .imePadding(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Spacer(Modifier.height(24.dp))

                    OutlinedTextField(
                        value          = toAddress,
                        onValueChange  = { toAddress = it; errorMessage = null },
                        label          = { Text("Recipient Address") },
                        singleLine     = true,
                        modifier       = Modifier.fillMaxWidth(),
                        colors         = walletTextFieldColors()
                    )

                    Spacer(Modifier.height(16.dp))

                    OutlinedTextField(
                        value          = amount,
                        onValueChange  = { amount = it; errorMessage = null },
                        label          = { Text("Amount ($chainLabel)") },
                        singleLine     = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier       = Modifier.fillMaxWidth(),
                        colors         = walletTextFieldColors()
                    )

                    if (errorMessage != null) {
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text  = errorMessage!!,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    Spacer(Modifier.height(28.dp))

                    Button(
                        onClick = {
                            if (toAddress.isBlank()) { errorMessage = "Enter a recipient address."; return@Button }
                            if (amount.isBlank() || amount.toDoubleOrNull() == null || amount.toDouble() <= 0) {
                                errorMessage = "Enter a valid amount."
                                return@Button
                            }
                            step = SendStep.WAITING_NFC
                        },
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        shape    = RoundedCornerShape(12.dp),
                        colors   = ButtonDefaults.buttonColors(containerColor = PurpleAccent)
                    ) {
                        Text("Tap NFC Card", fontWeight = FontWeight.SemiBold, color = Color.White, fontSize = 16.sp)
                    }

                    Spacer(Modifier.height(32.dp))
                }
            }

            // Step 2: Wait for NFC
            SendStep.WAITING_NFC -> {
                LaunchedEffect(Unit) {
                    viewModel.nfcTagEvent.collect { tag ->
                        isLoading = true
                        errorMessage = null
                        try {
                            val payload = NFCService.readPayload(tag)
                            // Verify chain matches
                            if (payload.chain != chainLabel) {
                                errorMessage = "Wrong card: expected $chainLabel but got ${payload.chain}"
                                isLoading = false
                                return@collect
                            }
                            nfcHalf     = payload.nfcHalf
                            nfcWalletId = payload.walletId
                            step        = SendStep.ENTER_PASSWORD
                        } catch (e: Exception) {
                            errorMessage = "NFC read failed: ${e.message}"
                        } finally {
                            isLoading = false
                        }
                    }
                }

                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement   = Arrangement.Center,
                    horizontalAlignment   = Alignment.CenterHorizontally
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(color = PurpleAccent, modifier = Modifier.size(56.dp))
                    } else {
                        Icon(
                            Icons.Default.Nfc,
                            contentDescription = null,
                            tint     = PurpleAccent,
                            modifier = Modifier.size(88.dp)
                        )
                    }
                    Spacer(Modifier.height(24.dp))
                    Text(
                        text  = "Waiting for NFC Card…",
                        style = MaterialTheme.typography.headlineSmall.copy(color = Color.White, fontWeight = FontWeight.Bold)
                    )
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text  = "Hold your NFC card to the back of your phone.",
                        style = MaterialTheme.typography.bodyMedium.copy(color = OnDarkMuted),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 24.dp)
                    )
                    if (errorMessage != null) {
                        Spacer(Modifier.height(16.dp))
                        Text(
                            text = errorMessage!!,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(horizontal = 24.dp),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            // Step 3: Password + send
            SendStep.ENTER_PASSWORD -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 24.dp)
                        .imePadding(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Spacer(Modifier.height(48.dp))

                    Text(
                        "Enter Password",
                        style = MaterialTheme.typography.headlineSmall.copy(color = Color.White, fontWeight = FontWeight.Bold),
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Confirm your encryption password to sign the transaction.",
                        style = MaterialTheme.typography.bodyMedium.copy(color = OnDarkMuted),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )

                    Spacer(Modifier.height(28.dp))

                    // Tx summary card
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(DarkCard, RoundedCornerShape(12.dp))
                            .padding(16.dp)
                    ) {
                        Text("Sending", style = MaterialTheme.typography.labelMedium.copy(color = OnDarkMuted))
                        Spacer(Modifier.height(4.dp))
                        Text("$amount $chainLabel", style = MaterialTheme.typography.titleLarge.copy(color = Color.White, fontWeight = FontWeight.Bold))
                        Spacer(Modifier.height(12.dp))
                        Text("To", style = MaterialTheme.typography.labelMedium.copy(color = OnDarkMuted))
                        Spacer(Modifier.height(4.dp))
                        Text(
                            toAddress,
                            style = MaterialTheme.typography.bodySmall.copy(color = Color.White, fontFamily = FontFamily.Monospace)
                        )
                    }

                    Spacer(Modifier.height(24.dp))

                    OutlinedTextField(
                        value          = password,
                        onValueChange  = { password = it; errorMessage = null },
                        label          = { Text("Password") },
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine     = true,
                        modifier       = Modifier.fillMaxWidth(),
                        colors         = walletTextFieldColors()
                    )

                    if (errorMessage != null) {
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text  = errorMessage!!,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    Spacer(Modifier.height(28.dp))

                    Button(
                        onClick  = {
                            if (password.isBlank()) { errorMessage = "Enter your password."; return@Button }
                            isLoading = true; errorMessage = null
                            scope.launch {
                                try {
                                    val hash = withContext(Dispatchers.IO) {
                                        // Fetch server half
                                        val serverBundle = viewModel.networkService.fetchServerKeyHalf(nfcWalletId)
                                        // Reconstruct private key
                                        val privKeyBytes = WalletService.reconstructPrivateKey(
                                            nfcHalfHex = nfcHalf,
                                            serverHalf = serverBundle.serverKeyHalf,
                                            salt       = serverBundle.salt,
                                            iv         = serverBundle.iv,
                                            tag        = serverBundle.tag,
                                            password   = password
                                        )
                                        if (selectedChain == 0) {
                                            sendEth(
                                                privateKeyBytes = privKeyBytes,
                                                toAddress       = toAddress,
                                                amountEth       = amount
                                            )
                                        } else {
                                            sendSol(
                                                privateKeySeed = privKeyBytes,
                                                fromAddress    = solAddress,
                                                toAddress      = toAddress,
                                                amountSol      = amount
                                            )
                                        }
                                    }
                                    txHash = hash
                                    step   = SendStep.DONE
                                } catch (e: Exception) {
                                    errorMessage = "Transaction failed: ${e.message}"
                                } finally {
                                    isLoading = false
                                }
                            }
                        },
                        enabled  = !isLoading,
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        shape    = RoundedCornerShape(12.dp),
                        colors   = ButtonDefaults.buttonColors(containerColor = PurpleAccent)
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(modifier = Modifier.size(22.dp), color = Color.White, strokeWidth = 2.dp)
                        } else {
                            Text("Send Transaction", fontWeight = FontWeight.SemiBold, color = Color.White, fontSize = 16.sp)
                        }
                    }

                    Spacer(Modifier.height(32.dp))
                }
            }

            // Step 4: Done
            SendStep.DONE -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 24.dp),
                    verticalArrangement   = Arrangement.Center,
                    horizontalAlignment   = Alignment.CenterHorizontally
                ) {
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint     = SuccessGreen,
                        modifier = Modifier.size(88.dp)
                    )
                    Spacer(Modifier.height(20.dp))
                    Text(
                        "Transaction Sent!",
                        style = MaterialTheme.typography.headlineMedium.copy(color = Color.White, fontWeight = FontWeight.Bold),
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(24.dp))
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(DarkCard, RoundedCornerShape(12.dp))
                            .padding(16.dp)
                    ) {
                        Text("Transaction Hash", style = MaterialTheme.typography.labelMedium.copy(color = OnDarkMuted))
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text  = txHash,
                            style = MaterialTheme.typography.bodySmall.copy(color = Color.White, fontFamily = FontFamily.Monospace)
                        )
                    }
                    Spacer(Modifier.height(32.dp))
                    Button(
                        onClick  = {
                            // Reset to send another
                            step = SendStep.ENTER_DETAILS
                            toAddress = ""
                            amount    = ""
                            password  = ""
                            txHash    = ""
                            errorMessage = null
                        },
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        shape    = RoundedCornerShape(12.dp),
                        colors   = ButtonDefaults.buttonColors(containerColor = PurpleAccent)
                    ) {
                        Text("Send Another", fontWeight = FontWeight.SemiBold, color = Color.White, fontSize = 16.sp)
                    }
                }
            }
        }
    }
}

// ─── ETH transaction sender ───────────────────────────────────────────────────

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

private suspend fun sendEth(
    privateKeyBytes: ByteArray,
    toAddress:       String,
    amountEth:       String
): String {
    val web3 = Web3j.build(
        HttpService("https://mainnet.infura.io/v3/9aa3d95b3bc440fa88ea12eaa4456161")
    )

    val credentials = Credentials.create(Numeric.toHexStringNoPrefix(privateKeyBytes))
    val fromAddress = credentials.address

    val nonce      = web3.ethGetTransactionCount(fromAddress, DefaultBlockParameterName.PENDING)
                         .send().transactionCount
    val gasPrice   = web3.ethGasPrice().send().gasPrice
    val gasLimit   = BigInteger.valueOf(21_000)
    val valueWei   = Convert.toWei(amountEth, Convert.Unit.ETHER).toBigInteger()

    val rawTx      = RawTransaction.createEtherTransaction(nonce, gasPrice, gasLimit, toAddress, valueWei)
    val chainId    = web3.ethChainId().send().chainId.toLong()
    val signedData = TransactionEncoder.signMessage(rawTx, chainId, credentials)
    val hexData    = Numeric.toHexString(signedData)

    val ethTx      = web3.ethSendRawTransaction(hexData).send()
    if (ethTx.hasError()) {
        throw IOException("ETH send error: ${ethTx.error.message}")
    }
    return ethTx.transactionHash ?: throw IOException("No transaction hash returned")
}

// ─── SOL transaction sender ───────────────────────────────────────────────────

private suspend fun sendSol(
    privateKeySeed: ByteArray,
    fromAddress:    String,
    toAddress:      String,
    amountSol:      String
): String {
    // 1. Derive ed25519 key pair from 32-byte seed
    val params  = EdDSANamedCurveTable.getByName("ed25519")
    val privSpec = EdDSAPrivateKeySpec(privateKeySeed, params)
    val privKey  = EdDSAPrivateKey(privSpec)

    // 2. Get recent blockhash from Solana mainnet
    val blockhash = fetchSolanaBlockhash()

    // 3. Encode lamports
    val lamports = (amountSol.toDouble() * 1_000_000_000.0).toLong()

    // 4. Build the Solana transaction message manually
    // Accounts: [from, to, SystemProgram]
    val systemProgramId = WalletService.base58Decode("11111111111111111111111111111111")
    val fromPubkey      = WalletService.base58Decode(fromAddress)
    val toPubkey        = WalletService.base58Decode(toAddress)

    // Message Header: num_required_signatures=1, num_readonly_signed=0, num_readonly_unsigned=1
    val header = byteArrayOf(1, 0, 1)

    // Account keys: [from, to, systemProgram]
    val accountKeys = fromPubkey + toPubkey + systemProgramId

    // Recent blockhash (32 bytes, decoded from base58)
    val blockhashBytes = WalletService.base58Decode(blockhash)

    // Instruction: SystemProgram Transfer
    //   programIdIndex = 2 (systemProgram)
    //   accounts       = [0 (from), 1 (to)]
    //   data           = [2, 0, 0, 0, lamports(8 bytes LE)]
    val instructionData = ByteBuffer.allocate(12)
        .order(ByteOrder.LITTLE_ENDIAN)
        .putInt(2)         // Transfer instruction index = 2
        .putLong(lamports)
        .array()

    val message = buildSolanaMessage(
        header       = header,
        accountKeys  = accountKeys,
        blockhash    = blockhashBytes,
        instructions = listOf(
            SolInstruction(programIdx = 2, accountIdxs = byteArrayOf(0, 1), data = instructionData)
        )
    )

    // 5. Sign the message
    val engine = EdDSAEngine(MessageDigest.getInstance("SHA-512"))
    engine.initSign(privKey)
    engine.update(message)
    val signature = engine.sign()

    // 6. Serialize full transaction: [compact_array(signatures)] + message
    val txBytes = byteArrayOf(1.toByte()) +   // 1 signature
                  signature +
                  message

    // 7. Base64-encode and send via JSON-RPC
    val txBase64 = android.util.Base64.encodeToString(txBytes, android.util.Base64.NO_WRAP)

    val body = mapOf(
        "jsonrpc" to "2.0",
        "id"      to 1,
        "method"  to "sendTransaction",
        "params"  to listOf(
            txBase64,
            mapOf("encoding" to "base64")
        )
    )
    val request = Request.Builder()
        .url("https://api.mainnet-beta.solana.com")
        .post(gson.toJson(body).toRequestBody(JSON_MT))
        .build()

    val response = httpClient.executeSuspend(request)
    val text     = response.body?.string() ?: throw IOException("Empty response")
    val map      = gson.fromJson(text, Map::class.java)

    val error = map["error"] as? Map<*, *>
    if (error != null) {
        throw IOException("SOL send error: ${error["message"]}")
    }

    return (map["result"] as? String) ?: throw IOException("No transaction signature returned")
}

private data class SolInstruction(
    val programIdx:  Int,
    val accountIdxs: ByteArray,
    val data:        ByteArray
)

private fun buildSolanaMessage(
    header:       ByteArray,
    accountKeys:  ByteArray,
    blockhash:    ByteArray,
    instructions: List<SolInstruction>
): ByteArray {
    val buf = mutableListOf<Byte>()

    // Header (3 bytes)
    buf.addAll(header.toList())

    // Compact-array of account keys (3 keys × 32 bytes each)
    val numAccounts = accountKeys.size / 32
    buf.add(numAccounts.toByte())
    buf.addAll(accountKeys.toList())

    // Recent blockhash (32 bytes)
    buf.addAll(blockhash.toList())

    // Compact-array of instructions
    buf.add(instructions.size.toByte())
    for (ix in instructions) {
        buf.add(ix.programIdx.toByte())

        // Compact-array of account indices
        buf.add(ix.accountIdxs.size.toByte())
        buf.addAll(ix.accountIdxs.toList())

        // Compact-array of instruction data
        buf.add(ix.data.size.toByte())
        buf.addAll(ix.data.toList())
    }

    return buf.toByteArray()
}

private suspend fun fetchSolanaBlockhash(): String {
    val body = mapOf(
        "jsonrpc" to "2.0",
        "id"      to 1,
        "method"  to "getLatestBlockhash",
        "params"  to listOf(mapOf("commitment" to "finalized"))
    )
    val request = Request.Builder()
        .url("https://api.mainnet-beta.solana.com")
        .post(gson.toJson(body).toRequestBody(JSON_MT))
        .build()

    val response = httpClient.executeSuspend(request)
    val text     = response.body?.string() ?: throw IOException("Empty blockhash response")
    val map      = gson.fromJson(text, Map::class.java)

    @Suppress("UNCHECKED_CAST")
    val result      = map["result"] as? Map<String, Any>
        ?: throw IOException("No result in blockhash response")
    @Suppress("UNCHECKED_CAST")
    val value       = result["value"] as? Map<String, Any>
        ?: throw IOException("No value in blockhash response")

    return value["blockhash"] as? String
        ?: throw IOException("No blockhash in response")
}
