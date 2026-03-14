package com.nfcwallet.app.ui.screens

import androidx.compose.animation.animateContentSize
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
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nfcwallet.app.AppViewModel
import com.nfcwallet.app.ui.theme.DarkBackground
import com.nfcwallet.app.ui.theme.DarkCard
import com.nfcwallet.app.ui.theme.OnDarkMuted
import com.nfcwallet.app.ui.theme.PurpleAccent
import kotlinx.coroutines.launch

@Composable
fun OnboardingScreen(viewModel: AppViewModel) {

    val scope        = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current

    var selectedTab  by remember { mutableStateOf(0) }   // 0 = Login, 1 = Register
    var email        by remember { mutableStateOf("") }
    var password     by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var isLoading    by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFF0F0C29), Color(0xFF302B63), Color(0xFF24243E))
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 28.dp)
                .imePadding(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            Spacer(Modifier.height(72.dp))

            // ── Logo / heading ────────────────────────────────────────────────
            Text(
                text       = "NFC Wallet",
                style      = MaterialTheme.typography.displaySmall.copy(
                    fontWeight = FontWeight.Bold,
                    color      = Color.White,
                    fontSize   = 36.sp
                ),
                textAlign  = TextAlign.Center
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text  = "Secure crypto in your pocket",
                style = MaterialTheme.typography.bodyMedium.copy(color = OnDarkMuted),
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(48.dp))

            // ── Tab row ───────────────────────────────────────────────────────
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor   = DarkCard,
                contentColor     = PurpleAccent,
                indicator        = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                        color = PurpleAccent
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                listOf("Login", "Register").forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick  = {
                            selectedTab  = index
                            errorMessage = null
                        },
                        text = {
                            Text(
                                text  = title,
                                color = if (selectedTab == index) PurpleAccent else OnDarkMuted,
                                fontWeight = if (selectedTab == index) FontWeight.SemiBold else FontWeight.Normal
                            )
                        }
                    )
                }
            }

            Spacer(Modifier.height(28.dp))

            // ── Email field ───────────────────────────────────────────────────
            OutlinedTextField(
                value          = email,
                onValueChange  = { email = it; errorMessage = null },
                label          = { Text("Email") },
                leadingIcon    = { Icon(Icons.Default.Email, contentDescription = null, tint = OnDarkMuted) },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Email,
                    imeAction    = ImeAction.Next
                ),
                keyboardActions = KeyboardActions(
                    onNext = { focusManager.moveFocus(FocusDirection.Down) }
                ),
                singleLine     = true,
                modifier       = Modifier.fillMaxWidth(),
                colors         = walletTextFieldColors()
            )

            Spacer(Modifier.height(16.dp))

            // ── Password field ────────────────────────────────────────────────
            OutlinedTextField(
                value          = password,
                onValueChange  = { password = it; errorMessage = null },
                label          = { Text("Password") },
                leadingIcon    = { Icon(Icons.Default.Lock, contentDescription = null, tint = OnDarkMuted) },
                trailingIcon   = {
                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                        Icon(
                            if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            contentDescription = if (passwordVisible) "Hide password" else "Show password",
                            tint = OnDarkMuted
                        )
                    }
                },
                visualTransformation = if (passwordVisible) VisualTransformation.None
                                       else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    imeAction    = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(
                    onDone = { focusManager.clearFocus() }
                ),
                singleLine     = true,
                modifier       = Modifier.fillMaxWidth(),
                colors         = walletTextFieldColors()
            )

            Spacer(Modifier.height(8.dp))

            // ── Error message ─────────────────────────────────────────────────
            if (errorMessage != null) {
                Text(
                    text   = errorMessage!!,
                    color  = MaterialTheme.colorScheme.error,
                    style  = MaterialTheme.typography.bodySmall,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                )
            }

            Spacer(Modifier.height(24.dp))

            // ── Submit button ─────────────────────────────────────────────────
            Button(
                onClick  = {
                    if (email.isBlank() || password.isBlank()) {
                        errorMessage = "Please fill in all fields."
                        return@Button
                    }
                    focusManager.clearFocus()
                    isLoading    = true
                    errorMessage = null

                    scope.launch {
                        try {
                            val response = if (selectedTab == 0) {
                                viewModel.networkService.login(email.trim(), password)
                            } else {
                                viewModel.networkService.register(email.trim(), password)
                            }
                            viewModel.login(response.token)
                        } catch (e: Exception) {
                            errorMessage = e.message ?: "An error occurred. Please try again."
                        } finally {
                            isLoading = false
                        }
                    }
                },
                enabled  = !isLoading,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .animateContentSize(),
                shape  = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PurpleAccent)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(22.dp),
                        color    = Color.White,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(
                        text       = if (selectedTab == 0) "Login" else "Register",
                        fontWeight = FontWeight.SemiBold,
                        fontSize   = 16.sp,
                        color      = Color.White
                    )
                }
            }

            Spacer(Modifier.height(32.dp))
        }
    }
}

// ─── Shared text-field colour helper ─────────────────────────────────────────

@Composable
fun walletTextFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor      = PurpleAccent,
    unfocusedBorderColor    = Color(0xFF4C1D95),
    focusedLabelColor       = PurpleAccent,
    unfocusedLabelColor     = OnDarkMuted,
    cursorColor             = PurpleAccent,
    focusedTextColor        = Color.White,
    unfocusedTextColor      = Color.White,
    focusedContainerColor   = DarkCard,
    unfocusedContainerColor = DarkCard
)
