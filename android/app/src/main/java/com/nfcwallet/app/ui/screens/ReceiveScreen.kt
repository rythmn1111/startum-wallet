package com.nfcwallet.app.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.widget.ImageView
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.google.zxing.BarcodeFormat
import com.journeyapps.barcodescanner.BarcodeEncoder
import com.nfcwallet.app.AppViewModel
import com.nfcwallet.app.ui.theme.DarkCard
import com.nfcwallet.app.ui.theme.OnDarkMuted
import com.nfcwallet.app.ui.theme.PurpleAccent

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReceiveScreen(viewModel: AppViewModel) {

    val context    = LocalContext.current
    val ethAddress by viewModel.ethAddress.collectAsState()
    val solAddress by viewModel.solAddress.collectAsState()

    var selectedTab by remember { mutableStateOf(0) }

    val currentAddress = if (selectedTab == 0) ethAddress else solAddress
    val chainLabel     = if (selectedTab == 0) "ETH" else "SOL"

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F0C29))
    ) {
        // ── Top bar ───────────────────────────────────────────────────────────
        TopAppBar(
            title = {
                Text(
                    "Receive",
                    style = MaterialTheme.typography.titleLarge.copy(
                        color = Color.White, fontWeight = FontWeight.Bold
                    )
                )
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
        )

        // ── Tab row ───────────────────────────────────────────────────────────
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor   = DarkCard,
            contentColor     = PurpleAccent,
            indicator        = { tabPositions ->
                TabRowDefaults.SecondaryIndicator(
                    Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                    color = PurpleAccent
                )
            }
        ) {
            listOf("ETH", "SOL").forEachIndexed { index, title ->
                Tab(
                    selected = selectedTab == index,
                    onClick  = { selectedTab = index },
                    text = {
                        Text(
                            title,
                            color = if (selectedTab == index) PurpleAccent else OnDarkMuted,
                            fontWeight = if (selectedTab == index) FontWeight.SemiBold else FontWeight.Normal
                        )
                    }
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(32.dp))

            // ── QR Code ───────────────────────────────────────────────────────
            Box(
                modifier = Modifier
                    .size(240.dp)
                    .background(Color.White, RoundedCornerShape(16.dp))
                    .padding(16.dp)
            ) {
                if (currentAddress.isNotBlank()) {
                    QRCodeView(
                        content  = currentAddress,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }

            Spacer(Modifier.height(24.dp))

            // ── Chain label ───────────────────────────────────────────────────
            Text(
                text  = "Your $chainLabel Address",
                style = MaterialTheme.typography.titleMedium.copy(
                    color      = Color.White,
                    fontWeight = FontWeight.SemiBold
                )
            )
            Spacer(Modifier.height(12.dp))

            // ── Selectable address ────────────────────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(DarkCard, RoundedCornerShape(12.dp))
                    .border(1.dp, Color(0xFF4C1D95), RoundedCornerShape(12.dp))
                    .padding(16.dp)
            ) {
                SelectionContainer {
                    Text(
                        text  = currentAddress,
                        style = MaterialTheme.typography.bodySmall.copy(
                            color      = Color.White,
                            fontFamily = FontFamily.Monospace
                        ),
                        textAlign = TextAlign.Center,
                        modifier  = Modifier.fillMaxWidth()
                    )
                }
            }

            Spacer(Modifier.height(20.dp))

            // ── Action buttons ────────────────────────────────────────────────
            Row(modifier = Modifier.fillMaxWidth()) {
                // Copy
                OutlinedButton(
                    onClick = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        val clip      = ClipData.newPlainText("$chainLabel Address", currentAddress)
                        clipboard.setPrimaryClip(clip)
                        Toast.makeText(context, "Address copied!", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.weight(1f).height(48.dp),
                    shape    = RoundedCornerShape(12.dp),
                    border   = androidx.compose.foundation.BorderStroke(1.dp, PurpleAccent)
                ) {
                    Icon(Icons.Default.ContentCopy, contentDescription = "Copy", tint = PurpleAccent, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.padding(4.dp))
                    Text("Copy", color = PurpleAccent, fontSize = 14.sp)
                }

                Spacer(Modifier.padding(8.dp))

                // Share
                Button(
                    onClick = {
                        val sendIntent = Intent().apply {
                            action = Intent.ACTION_SEND
                            putExtra(Intent.EXTRA_TEXT, "My $chainLabel address: $currentAddress")
                            type = "text/plain"
                        }
                        context.startActivity(Intent.createChooser(sendIntent, "Share $chainLabel Address"))
                    },
                    modifier = Modifier.weight(1f).height(48.dp),
                    shape    = RoundedCornerShape(12.dp),
                    colors   = ButtonDefaults.buttonColors(containerColor = PurpleAccent)
                ) {
                    Icon(Icons.Default.Share, contentDescription = "Share", tint = Color.White, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.padding(4.dp))
                    Text("Share", color = Color.White, fontSize = 14.sp)
                }
            }

            Spacer(Modifier.height(32.dp))
        }
    }
}

// ─── QR Code composable using zxing-android-embedded ─────────────────────────

@Composable
private fun QRCodeView(content: String, modifier: Modifier = Modifier) {
    AndroidView(
        factory = { ctx ->
            ImageView(ctx).apply {
                scaleType = ImageView.ScaleType.FIT_CENTER
            }
        },
        update = { imageView ->
            try {
                val encoder  = BarcodeEncoder()
                val bitmap: Bitmap = encoder.encodeBitmap(
                    content,
                    BarcodeFormat.QR_CODE,
                    512,
                    512
                )
                imageView.setImageBitmap(bitmap)
            } catch (_: Exception) { /* ignore on blank address */ }
        },
        modifier = modifier
    )
}
