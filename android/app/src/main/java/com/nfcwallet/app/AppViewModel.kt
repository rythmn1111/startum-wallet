package com.nfcwallet.app

import android.app.Application
import android.content.Context
import android.nfc.Tag
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.nfcwallet.app.services.NetworkService
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AppViewModel(app: Application) : AndroidViewModel(app) {

    // ─── Persistence ──────────────────────────────────────────────────────────

    private val prefs = app.getSharedPreferences("nfc_wallet_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_AUTH_TOKEN  = "auth_token"
        private const val KEY_ETH_ADDRESS = "eth_address"
        private const val KEY_SOL_ADDRESS = "sol_address"
    }

    // ─── Network service (shared across screens) ──────────────────────────────

    val networkService: NetworkService = NetworkService(app)

    // ─── State ────────────────────────────────────────────────────────────────

    private val _isLoggedIn  = MutableStateFlow(prefs.getString(KEY_AUTH_TOKEN, null) != null)
    val isLoggedIn: StateFlow<Boolean> = _isLoggedIn.asStateFlow()

    private val _hasWallet   = MutableStateFlow(prefs.getString(KEY_ETH_ADDRESS, null) != null)
    val hasWallet: StateFlow<Boolean>  = _hasWallet.asStateFlow()

    private val _ethAddress  = MutableStateFlow(prefs.getString(KEY_ETH_ADDRESS, "") ?: "")
    val ethAddress: StateFlow<String> = _ethAddress.asStateFlow()

    private val _solAddress  = MutableStateFlow(prefs.getString(KEY_SOL_ADDRESS, "") ?: "")
    val solAddress: StateFlow<String> = _solAddress.asStateFlow()

    // NFC tag events – replayed to the latest subscriber (capacity = 1 acts as a buffer)
    val nfcTagEvent: MutableSharedFlow<Tag> = MutableSharedFlow(extraBufferCapacity = 1)

    // ─── Auth ─────────────────────────────────────────────────────────────────

    fun login(token: String) {
        prefs.edit().putString(KEY_AUTH_TOKEN, token).apply()
        _isLoggedIn.value = true
    }

    fun logout() {
        prefs.edit()
            .remove(KEY_AUTH_TOKEN)
            .remove(KEY_ETH_ADDRESS)
            .remove(KEY_SOL_ADDRESS)
            .apply()
        networkService.authToken = null
        _isLoggedIn.value  = false
        _hasWallet.value   = false
        _ethAddress.value  = ""
        _solAddress.value  = ""
    }

    // ─── Wallet state ─────────────────────────────────────────────────────────

    fun saveWalletAddresses(eth: String, sol: String) {
        prefs.edit()
            .putString(KEY_ETH_ADDRESS, eth)
            .putString(KEY_SOL_ADDRESS, sol)
            .apply()
        _ethAddress.value = eth
        _solAddress.value = sol
        _hasWallet.value  = true
    }

    // ─── NFC ──────────────────────────────────────────────────────────────────

    /**
     * Called from MainActivity.onNewIntent when an NFC tag is discovered.
     * Emits the Tag so whichever screen is currently collecting can handle it.
     */
    fun onNfcTag(tag: Tag) {
        viewModelScope.launch {
            nfcTagEvent.emit(tag)
        }
    }
}
