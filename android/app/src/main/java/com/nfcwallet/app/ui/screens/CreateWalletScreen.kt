package com.nfcwallet.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Nfc
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import com.nfcwallet.app.AppViewModel
import com.nfcwallet.app.services.NFCService
import com.nfcwallet.app.services.WalletService
import com.nfcwallet.app.types.Chain
import com.nfcwallet.app.types.KeySplit
import com.nfcwallet.app.types.NFCCardPayload
import com.nfcwallet.app.types.WalletResult
import com.nfcwallet.app.ui.theme.DarkBackground
import com.nfcwallet.app.ui.theme.DarkCard
import com.nfcwallet.app.ui.theme.OnDarkMuted
import com.nfcwallet.app.ui.theme.PurpleAccent
import com.nfcwallet.app.ui.theme.SuccessGreen
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private enum class CreateStep {
    GENERATING,
    SHOW_MNEMONIC,
    SET_PASSWORD,
    WRITE_NFC,
    DONE
}

@Composable
fun CreateWalletScreen(
    viewModel: AppViewModel,
    onWalletCreated: () -> Unit
) {
    val scope = rememberCoroutineScope()

    var step         by remember { mutableStateOf(CreateStep.GENERATING) }
    var walletResult by remember { mutableStateOf<WalletResult?>(null) }
    var ethSplit     by remember { mutableStateOf<KeySplit?>(null) }
    var solSplit     by remember { mutableStateOf<KeySplit?>(null) }
    var writingChain by remember { mutableStateOf(Chain.ETH) }  // ETH first, then SOL
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // ── Step 1: generate wallets on first composition ─────────────────────────
    LaunchedEffect(Unit) {
        withContext(Dispatchers.Default) {
            try {
                val result = WalletService.generateWallets()
                walletResult = result
                step = CreateStep.SHOW_MNEMONIC
            } catch (e: Exception) {
                errorMessage = "Failed to generate wallet: ${e.message}"
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFF0F0C29), Color(0xFF302B63), Color(0xFF24243E))
                )
            )
    ) {
        when (step) {

            // ── Step 1: Generating spinner ────────────────────────────────────
            CreateStep.GENERATING -> {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement   = Arrangement.Center,
                    horizontalAlignment   = Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator(color = PurpleAccent, modifier = Modifier.size(56.dp))
                    Spacer(Modifier.height(24.dp))
                    Text(
                        text  = "Generating your wallet…",
                        style = MaterialTheme.typography.bodyLarge.copy(color = Color.White)
                    )
                    if (errorMessage != null) {
                        Spacer(Modifier.height(16.dp))
                        Text(
                            text  = errorMessage!!,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(horizontal = 24.dp),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            // ── Step 2: Show mnemonic ─────────────────────────────────────────
            CreateStep.SHOW_MNEMONIC -> {
                MnemonicStep(
                    walletResult = walletResult!!,
                    onContinue   = { step = CreateStep.SET_PASSWORD }
                )
            }

            // ── Step 3: Set password ──────────────────────────────────────────
            CreateStep.SET_PASSWORD -> {
                PasswordStep(
                    walletResult = walletResult!!,
                    onContinue   = { eth, sol ->
                        ethSplit = eth
                        solSplit = sol
                        writingChain = Chain.ETH
                        step = CreateStep.WRITE_NFC
                    },
                    viewModel    = viewModel,
                    onError      = { errorMessage = it }
                )
            }

            // ── Step 4: NFC write ─────────────────────────────────────────────
            CreateStep.WRITE_NFC -> {
                NFCWriteStep(
                    chain        = writingChain,
                    viewModel    = viewModel,
                    split        = if (writingChain == Chain.ETH) ethSplit!! else solSplit!!,
                    onTagWritten = {
                        if (writingChain == Chain.ETH) {
                            writingChain = Chain.SOL
                        } else {
                            step = CreateStep.DONE
                        }
                    },
                    onError = { errorMessage = it }
                )
            }

            // ── Step 5: Done ──────────────────────────────────────────────────
            CreateStep.DONE -> {
                DoneStep(
                    ethAddress = walletResult!!.ethAddress,
                    solAddress = walletResult!!.solAddress,
                    onOpen     = {
                        viewModel.saveWalletAddresses(
                            walletResult!!.ethAddress,
                            walletResult!!.solAddress
                        )
                        onWalletCreated()
                    }
                )
            }
        }
    }
}

// ─── Step 2: Mnemonic display ─────────────────────────────────────────────────

@Composable
private fun MnemonicStep(walletResult: WalletResult, onContinue: () -> Unit) {
    val words      = walletResult.mnemonic.split(" ")
    var confirmed  by remember { mutableStateOf(false) }
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(48.dp))

        Text(
            text       = "Your Seed Phrase",
            style      = MaterialTheme.typography.headlineMedium.copy(
                color      = Color.White,
                fontWeight = FontWeight.Bold
            )
        )
        Spacer(Modifier.height(12.dp))
        Text(
            text      = "Write these 12 words down in order. They are the ONLY way to recover your wallet.",
            style     = MaterialTheme.typography.bodyMedium.copy(color = OnDarkMuted),
            textAlign = TextAlign.Center,
            modifier  = Modifier.padding(horizontal = 8.dp)
        )
        Spacer(Modifier.height(28.dp))

        // 4-column × 3-row grid
        LazyVerticalGrid(
            columns          = GridCells.Fixed(3),
            modifier         = Modifier
                .fillMaxWidth()
                .height(260.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement   = Arrangement.spacedBy(10.dp)
        ) {
            itemsIndexed(words) { index, word ->
                Box(
                    modifier = Modifier
                        .background(DarkCard, RoundedCornerShape(10.dp))
                        .border(1.dp, Color(0xFF4C1D95), RoundedCornerShape(10.dp))
                        .padding(vertical = 10.dp, horizontal = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text  = "${index + 1}",
                            style = MaterialTheme.typography.labelSmall.copy(color = OnDarkMuted),
                            fontSize = 10.sp
                        )
                        Text(
                            text  = word,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color      = Color.White,
                                fontWeight = FontWeight.SemiBold,
                                fontFamily = FontFamily.Monospace
                            ),
                            fontSize = 13.sp
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(24.dp))

        // Confirmation checkbox
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier          = Modifier.fillMaxWidth()
        ) {
            Checkbox(
                checked         = confirmed,
                onCheckedChange = { confirmed = it },
                colors          = CheckboxDefaults.colors(
                    checkedColor   = PurpleAccent,
                    uncheckedColor = OnDarkMuted
                )
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text  = "I've written it down safely",
                style = MaterialTheme.typography.bodyMedium.copy(color = Color.White)
            )
        }

        Spacer(Modifier.height(24.dp))

        Button(
            onClick  = onContinue,
            enabled  = confirmed,
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape    = RoundedCornerShape(12.dp),
            colors   = ButtonDefaults.buttonColors(
                containerColor         = PurpleAccent,
                disabledContainerColor = Color(0xFF4C1D95).copy(alpha = 0.5f)
            )
        ) {
            Text("Continue", fontWeight = FontWeight.SemiBold, color = Color.White, fontSize = 16.sp)
        }

        Spacer(Modifier.height(32.dp))
    }
}

// ─── Step 3: Password ─────────────────────────────────────────────────────────

@Composable
private fun PasswordStep(
    walletResult: WalletResult,
    onContinue:   (KeySplit, KeySplit) -> Unit,
    viewModel:    AppViewModel,
    onError:      (String) -> Unit
) {
    val scope = rememberCoroutineScope()
    var password    by remember { mutableStateOf("") }
    var confirmPwd  by remember { mutableStateOf("") }
    var isLoading   by remember { mutableStateOf(false) }
    var localError  by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp)
            .imePadding(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(64.dp))

        Icon(
            Icons.Default.Lock,
            contentDescription = null,
            tint     = PurpleAccent,
            modifier = Modifier.size(56.dp)
        )
        Spacer(Modifier.height(16.dp))
        Text(
            "Set Encryption Password",
            style     = MaterialTheme.typography.headlineSmall.copy(color = Color.White, fontWeight = FontWeight.Bold),
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(8.dp))
        Text(
            "This password encrypts your private keys. It cannot be recovered.",
            style     = MaterialTheme.typography.bodyMedium.copy(color = OnDarkMuted),
            textAlign = TextAlign.Center,
            modifier  = Modifier.padding(horizontal = 8.dp)
        )
        Spacer(Modifier.height(32.dp))

        OutlinedTextField(
            value          = password,
            onValueChange  = { password = it; localError = null },
            label          = { Text("Password (min 8 chars)") },
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            singleLine     = true,
            modifier       = Modifier.fillMaxWidth(),
            colors         = walletTextFieldColors()
        )

        Spacer(Modifier.height(16.dp))

        OutlinedTextField(
            value          = confirmPwd,
            onValueChange  = { confirmPwd = it; localError = null },
            label          = { Text("Confirm Password") },
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            singleLine     = true,
            modifier       = Modifier.fillMaxWidth(),
            colors         = walletTextFieldColors()
        )

        if (localError != null) {
            Spacer(Modifier.height(8.dp))
            Text(
                text  = localError!!,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.fillMaxWidth()
            )
        }

        Spacer(Modifier.height(28.dp))

        Button(
            onClick = {
                if (password.length < 8) { localError = "Password must be at least 8 characters."; return@Button }
                if (password != confirmPwd) { localError = "Passwords do not match."; return@Button }
                isLoading = true

                scope.launch {
                    try {
                        val (ethSplit, solSplit) = withContext(Dispatchers.Default) {
                            val eth = WalletService.splitKey(
                                chain         = Chain.ETH,
                                privateKey    = walletResult.ethPrivateKey,
                                publicAddress = walletResult.ethAddress,
                                password      = password
                            )
                            val sol = WalletService.splitKey(
                                chain         = Chain.SOL,
                                privateKey    = walletResult.solPrivateKey,
                                publicAddress = walletResult.solAddress,
                                password      = password
                            )
                            Pair(eth, sol)
                        }

                        // Store server halves
                        withContext(Dispatchers.IO) {
                            viewModel.networkService.storeKeyHalf(ethSplit)
                            viewModel.networkService.storeKeyHalf(solSplit)
                        }

                        onContinue(ethSplit, solSplit)
                    } catch (e: Exception) {
                        localError = "Error: ${e.message}"
                        onError(e.message ?: "Unknown error")
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
                Text("Encrypt & Prepare Keys", fontWeight = FontWeight.SemiBold, color = Color.White, fontSize = 16.sp)
            }
        }

        Spacer(Modifier.height(32.dp))
    }
}

// ─── Step 4: NFC write ────────────────────────────────────────────────────────

@Composable
private fun NFCWriteStep(
    chain:       Chain,
    viewModel:   AppViewModel,
    split:       KeySplit,
    onTagWritten: () -> Unit,
    onError:     (String) -> Unit
) {
    val scope       = rememberCoroutineScope()
    var statusText  by remember { mutableStateOf<String?>(null) }
    var isWriting   by remember { mutableStateOf(false) }

    // Collect NFC tag events
    LaunchedEffect(chain) {
        viewModel.nfcTagEvent.collect { tag ->
            if (isWriting) return@collect
            isWriting = true
            statusText = "Writing…"
            try {
                val payload = NFCCardPayload(
                    walletId      = split.walletId,
                    chain         = chain.name,
                    nfcHalf       = split.nfcHalf,
                    publicAddress = split.publicAddress
                )
                NFCService.writePayload(tag, payload)
                statusText = "Written successfully!"
                onTagWritten()
            } catch (e: Exception) {
                statusText = "Write failed: ${e.message}"
                onError(e.message ?: "NFC write failed")
                isWriting = false
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        verticalArrangement   = Arrangement.Center,
        horizontalAlignment   = Alignment.CenterHorizontally
    ) {
        Icon(
            Icons.Default.Nfc,
            contentDescription = null,
            tint     = PurpleAccent,
            modifier = Modifier.size(88.dp)
        )
        Spacer(Modifier.height(24.dp))
        Text(
            text       = "Tap Your NFC Card",
            style      = MaterialTheme.typography.headlineSmall.copy(
                color      = Color.White,
                fontWeight = FontWeight.Bold
            )
        )
        Spacer(Modifier.height(12.dp))
        Text(
            text = if (chain == Chain.ETH)
                "Hold your NFC card to the back of your phone to write the ETH key."
            else
                "Hold your NFC card to the back of your phone to write the SOL key.",
            style     = MaterialTheme.typography.bodyMedium.copy(color = OnDarkMuted),
            textAlign = TextAlign.Center
        )

        if (isWriting) {
            Spacer(Modifier.height(24.dp))
            CircularProgressIndicator(color = PurpleAccent)
        }

        if (statusText != null) {
            Spacer(Modifier.height(16.dp))
            Text(
                text  = statusText!!,
                color = if (statusText!!.startsWith("Write failed")) MaterialTheme.colorScheme.error
                        else SuccessGreen,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center
            )
        }

        Spacer(Modifier.height(12.dp))
        Text(
            text  = "Writing ${chain.name} key…",
            style = MaterialTheme.typography.labelMedium.copy(color = OnDarkMuted)
        )
    }
}

// ─── Step 5: Done ─────────────────────────────────────────────────────────────

@Composable
private fun DoneStep(
    ethAddress: String,
    solAddress: String,
    onOpen:     () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(80.dp))

        Icon(
            Icons.Default.CheckCircle,
            contentDescription = null,
            tint     = SuccessGreen,
            modifier = Modifier.size(88.dp)
        )
        Spacer(Modifier.height(20.dp))
        Text(
            "Wallet Created!",
            style     = MaterialTheme.typography.headlineMedium.copy(color = Color.White, fontWeight = FontWeight.Bold),
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(8.dp))
        Text(
            "Your keys are encrypted and split between your NFC card and the server.",
            style     = MaterialTheme.typography.bodyMedium.copy(color = OnDarkMuted),
            textAlign = TextAlign.Center,
            modifier  = Modifier.padding(horizontal = 8.dp)
        )

        Spacer(Modifier.height(36.dp))

        // ETH address card
        AddressCard(label = "ETH Address", address = ethAddress)
        Spacer(Modifier.height(16.dp))
        AddressCard(label = "SOL Address", address = solAddress)

        Spacer(Modifier.height(40.dp))

        Button(
            onClick  = onOpen,
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape    = RoundedCornerShape(12.dp),
            colors   = ButtonDefaults.buttonColors(containerColor = PurpleAccent)
        ) {
            Text("Open Wallet", fontWeight = FontWeight.SemiBold, color = Color.White, fontSize = 16.sp)
        }

        Spacer(Modifier.height(32.dp))
    }
}

@Composable
private fun AddressCard(label: String, address: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(DarkCard, RoundedCornerShape(12.dp))
            .border(1.dp, Color(0xFF4C1D95), RoundedCornerShape(12.dp))
            .padding(16.dp)
    ) {
        Text(label, style = MaterialTheme.typography.labelMedium.copy(color = OnDarkMuted))
        Spacer(Modifier.height(6.dp))
        Text(
            address,
            style = MaterialTheme.typography.bodySmall.copy(
                color      = Color.White,
                fontFamily = FontFamily.Monospace
            )
        )
    }
}
