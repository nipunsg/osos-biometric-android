package com.lk.biometricrd.feature.auth.presentation.login

import android.content.Context
import android.content.ContextWrapper
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import androidx.hilt.navigation.compose.hiltViewModel
import com.lk.biometricrd.R
import com.lk.biometricrd.core.presentation.components.BottomErrorAlert
import com.lk.biometricrd.core.presentation.components.ConfirmDialog
import com.lk.biometricrd.core.presentation.components.GlobalLoadingIndicator
import com.lk.biometricrd.feature.auth.presentation.model.LoginState
import com.lk.biometricrd.feature.auth.presentation.model.LoginUiEvent

@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit,
    viewModel: LoginViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val activity = LocalContext.current.findFragmentActivity()
    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) viewModel.onResume()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(Unit) {
        viewModel.uiEvent.collect { event ->
            when (event) {
                LoginUiEvent.NavigateToDashboard -> onLoginSuccess()
            }
        }
    }

    Scaffold { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                if (uiState.savedEmail.isBlank()) {
                    OutlinedTextField(
                        value = uiState.formData.email,
                        onValueChange = viewModel::onEmailChange,
                        label = { Text("Email") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        modifier = Modifier.fillMaxWidth(),
                    )
                } else {
                    Text(
                        text = stringResource(R.string.msg_hi_email, uiState.savedEmail),
                        style = MaterialTheme.typography.titleMedium,
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = uiState.formData.password,
                    onValueChange = viewModel::onPasswordChange,
                    label = { Text("Password") },
                    singleLine = true,
                    visualTransformation = if (uiState.formData.isPasswordVisible) {
                        VisualTransformation.None
                    } else {
                        PasswordVisualTransformation()
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    trailingIcon = {
                        TextButton(onClick = viewModel::onTogglePasswordVisibility) {
                            Text(
                                text = if (uiState.formData.isPasswordVisible) "Hide" else "Show",
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                )

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = viewModel::onLoginClick,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Login", style = TextStyle(color = Color.White))
                }

                if (uiState.savedEmail.isNotBlank()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = viewModel::onSignInAsNew,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("SignIn as New", style = TextStyle(color = Color.White))
                    }
                }

                if (uiState.showScan) {
                    Spacer(modifier = Modifier.height(8.dp))
                    IconButton(
                        onClick = {
                            val cipher = viewModel.prepareScan() ?: return@IconButton
                            viewModel.biometrics.prompt(
                                activity = activity,
                                title = activity.getString(R.string.title_scan),
                                subtitle = activity.getString(R.string.msg_scan),
                                negativeButton = activity.getString(R.string.action_cancel),
                                cipher = cipher,
                                onResult = { viewModel.onPromptResult(enabling = false, it) },
                            )
                        },
                        enabled = uiState.loginState !is LoginState.Loading,
                    ) {
                        Icon(
                            imageVector = if (uiState.scanUsesFace) {
                                Icons.Filled.Face
                            } else {
                                Icons.Filled.Fingerprint
                            },
                            contentDescription = stringResource(R.string.action_scan),
                        )
                    }
                }
            }

            if (uiState.loginState is LoginState.Loading) {
                GlobalLoadingIndicator()
            }

            when (val state = uiState.loginState) {
                is LoginState.Error -> {
                    BottomErrorAlert(
                        title = state.title ?: stringResource(R.string.title_login_failed),
                        message = state.message,
                        onDismiss = { viewModel.clearError() },
                    )
                }
                else -> {}
            }
        }
    }

    if (uiState.askEnableBiometric) {
        ConfirmDialog(
            title = stringResource(R.string.title_enable_biometric),
            message = stringResource(R.string.msg_enable_biometric),
            onConfirm = {
                val cipher = viewModel.prepareEnable() ?: return@ConfirmDialog
                viewModel.biometrics.prompt(
                    activity = activity,
                    title = activity.getString(R.string.title_enable_scan),
                    subtitle = activity.getString(R.string.msg_scan),
                    negativeButton = activity.getString(R.string.action_cancel),
                    cipher = cipher,
                    onResult = { viewModel.onPromptResult(enabling = true, it) },
                )
            },
            onDismiss = viewModel::onEnableBiometricNo,
        )
    }
}

private fun Context.findFragmentActivity(): FragmentActivity {
    var current = this
    while (current is ContextWrapper) {
        if (current is FragmentActivity) return current
        current = current.baseContext
    }
    error("FragmentActivity required")
}
