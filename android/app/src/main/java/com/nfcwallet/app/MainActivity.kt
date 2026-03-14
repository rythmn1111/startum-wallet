package com.nfcwallet.app

import android.app.PendingIntent
import android.content.Intent
import android.content.IntentFilter
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.nfc.tech.Ndef
import android.nfc.tech.NdefFormatable
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.nfcwallet.app.ui.screens.BalanceScreen
import com.nfcwallet.app.ui.screens.CreateWalletScreen
import com.nfcwallet.app.ui.screens.OnboardingScreen
import com.nfcwallet.app.ui.screens.ReceiveScreen
import com.nfcwallet.app.ui.screens.SendScreen
import com.nfcwallet.app.ui.theme.DarkBackground
import com.nfcwallet.app.ui.theme.DarkCard
import com.nfcwallet.app.ui.theme.NFCWalletTheme
import com.nfcwallet.app.ui.theme.OnDarkMuted
import com.nfcwallet.app.ui.theme.PurpleAccent

// ─── Navigation route constants ───────────────────────────────────────────────

private object Routes {
    const val ONBOARDING    = "onboarding"
    const val CREATE_WALLET = "create_wallet"
    const val BALANCE       = "balance"
    const val RECEIVE       = "receive"
    const val SEND          = "send"
}

// ─── Bottom-nav item definition ───────────────────────────────────────────────

private data class BottomNavItem(
    val label:  String,
    val icon:   ImageVector,
    val route:  String
)

private val bottomNavItems = listOf(
    BottomNavItem("Balance", Icons.Default.AccountBalanceWallet, Routes.BALANCE),
    BottomNavItem("Receive", Icons.Default.QrCode,              Routes.RECEIVE),
    BottomNavItem("Send",    Icons.Default.Send,                Routes.SEND)
)

// ─── MainActivity ─────────────────────────────────────────────────────────────

class MainActivity : ComponentActivity() {

    private val viewModel: AppViewModel by viewModels()
    private var nfcAdapter: NfcAdapter? = null
    private lateinit var pendingIntent: PendingIntent

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // ── NFC setup ─────────────────────────────────────────────────────────
        nfcAdapter = NfcAdapter.getDefaultAdapter(this)

        val intent = Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
        }
        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            PendingIntent.FLAG_MUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }
        pendingIntent = PendingIntent.getActivity(this, 0, intent, flags)

        // ── Compose UI ────────────────────────────────────────────────────────
        setContent {
            NFCWalletTheme {
                NFCWalletApp(viewModel)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        nfcAdapter?.let { adapter ->
            val techLists = arrayOf(
                arrayOf(Ndef::class.java.name),
                arrayOf(NdefFormatable::class.java.name)
            )
            val intentFilters = arrayOf(
                IntentFilter(NfcAdapter.ACTION_NDEF_DISCOVERED).apply {
                    try { addDataType("text/plain") } catch (_: IntentFilter.MalformedMimeTypeException) {}
                },
                IntentFilter(NfcAdapter.ACTION_TECH_DISCOVERED),
                IntentFilter(NfcAdapter.ACTION_TAG_DISCOVERED)
            )
            adapter.enableForegroundDispatch(this, pendingIntent, intentFilters, techLists)
        }
    }

    override fun onPause() {
        super.onPause()
        nfcAdapter?.disableForegroundDispatch(this)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)

        when (intent.action) {
            NfcAdapter.ACTION_NDEF_DISCOVERED,
            NfcAdapter.ACTION_TECH_DISCOVERED,
            NfcAdapter.ACTION_TAG_DISCOVERED -> {
                val tag: Tag? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    intent.getParcelableExtra(NfcAdapter.EXTRA_TAG, Tag::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    intent.getParcelableExtra(NfcAdapter.EXTRA_TAG)
                }
                tag?.let { viewModel.onNfcTag(it) }
            }
        }
    }
}

// ─── Root composable ──────────────────────────────────────────────────────────

@Composable
fun NFCWalletApp(viewModel: AppViewModel) {
    val isLoggedIn by viewModel.isLoggedIn.collectAsState()
    val hasWallet  by viewModel.hasWallet.collectAsState()
    val navController = rememberNavController()

    // Decide start destination
    val startDestination = when {
        !isLoggedIn -> Routes.ONBOARDING
        !hasWallet  -> Routes.CREATE_WALLET
        else        -> Routes.BALANCE
    }

    Scaffold(
        containerColor = DarkBackground,
        bottomBar = {
            // Show bottom nav only when logged in and wallet exists
            if (isLoggedIn && hasWallet) {
                WalletBottomNav(
                    navController = navController
                )
            }
        }
    ) { innerPadding ->

        NavHost(
            navController    = navController,
            startDestination = startDestination,
            modifier         = Modifier.padding(innerPadding)
        ) {
            // ── Onboarding ────────────────────────────────────────────────────
            composable(Routes.ONBOARDING) {
                OnboardingScreen(viewModel = viewModel)
            }

            // ── Create Wallet ─────────────────────────────────────────────────
            composable(Routes.CREATE_WALLET) {
                CreateWalletScreen(
                    viewModel       = viewModel,
                    onWalletCreated = {
                        navController.navigate(Routes.BALANCE) {
                            popUpTo(Routes.CREATE_WALLET) { inclusive = true }
                        }
                    }
                )
            }

            // ── Home tabs ─────────────────────────────────────────────────────
            composable(Routes.BALANCE) {
                BalanceScreen(viewModel = viewModel)
            }
            composable(Routes.RECEIVE) {
                ReceiveScreen(viewModel = viewModel)
            }
            composable(Routes.SEND) {
                SendScreen(viewModel = viewModel)
            }
        }
    }

    // Side-effect: react to auth/wallet state changes and navigate accordingly
    LaunchedEffect(isLoggedIn, hasWallet) {
        when {
            !isLoggedIn -> {
                navController.navigate(Routes.ONBOARDING) {
                    popUpTo(0) { inclusive = true }
                }
            }
            isLoggedIn && !hasWallet -> {
                navController.navigate(Routes.CREATE_WALLET) {
                    popUpTo(0) { inclusive = true }
                }
            }
            isLoggedIn && hasWallet -> {
                val currentRoute = navController.currentBackStackEntry?.destination?.route
                if (currentRoute == Routes.ONBOARDING || currentRoute == Routes.CREATE_WALLET || currentRoute == null) {
                    navController.navigate(Routes.BALANCE) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            }
        }
    }
}

// ─── Bottom Navigation Bar ────────────────────────────────────────────────────

@Composable
private fun WalletBottomNav(navController: androidx.navigation.NavController) {
    var selectedRoute by remember { mutableStateOf(Routes.BALANCE) }

    NavigationBar(
        containerColor = DarkCard,
        contentColor   = PurpleAccent
    ) {
        bottomNavItems.forEach { item ->
            NavigationBarItem(
                selected = selectedRoute == item.route,
                onClick  = {
                    if (selectedRoute != item.route) {
                        selectedRoute = item.route
                        navController.navigate(item.route) {
                            popUpTo(navController.graph.startDestinationId) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState    = true
                        }
                    }
                },
                icon  = {
                    Icon(
                        item.icon,
                        contentDescription = item.label,
                        tint = if (selectedRoute == item.route) PurpleAccent else OnDarkMuted
                    )
                },
                label = {
                    Text(
                        text  = item.label,
                        color = if (selectedRoute == item.route) PurpleAccent else OnDarkMuted,
                        fontWeight = if (selectedRoute == item.route) FontWeight.SemiBold else FontWeight.Normal
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor   = PurpleAccent,
                    selectedTextColor   = PurpleAccent,
                    indicatorColor      = PurpleAccent.copy(alpha = 0.15f),
                    unselectedIconColor = OnDarkMuted,
                    unselectedTextColor = OnDarkMuted
                )
            )
        }
    }
}
