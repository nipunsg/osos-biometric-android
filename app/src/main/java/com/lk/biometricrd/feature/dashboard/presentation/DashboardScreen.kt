package com.lk.biometricrd.feature.dashboard.presentation

import android.content.Context
import android.content.ContextWrapper
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import androidx.hilt.navigation.compose.hiltViewModel
import com.lk.biometricrd.R
import com.lk.biometricrd.core.presentation.components.ConfirmDialog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    onLogout: () -> Unit,
    viewModel: DashboardViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val activity = LocalContext.current.findFragmentActivity()
    var showLogoutDialog by remember { mutableStateOf(false) }
    var paymentPassword by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(text = stringResource(R.string.title_dashboard))
                },
                actions = {
                    TextButton(onClick = { showLogoutDialog = true }) {
                        Text(text = stringResource(R.string.action_logout))
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.primary,
                    actionIconContentColor = MaterialTheme.colorScheme.primary
                )
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(24.dp)
        ) {
            Column {
                if (uiState.displayName.isNotBlank()) {
                    Text(
                        text = "Hello, ${uiState.displayName}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                }
                Button(
                    onClick = {
                        if (!viewModel.startPayment()) return@Button
                        viewModel.biometrics.prompt(
                            activity = activity,
                            title = activity.getString(R.string.title_payment),
                            subtitle = activity.getString(R.string.msg_scan),
                            negativeButton = activity.getString(R.string.action_cancel),
                            onResult = viewModel::onPromptResult,
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        text = stringResource(R.string.action_make_payment),
                        style = TextStyle(color = Color.White),
                    )
                }
                if (uiState.showPaymentSuccess) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = stringResource(R.string.msg_payment_success),
                        style = MaterialTheme.typography.titleMedium,
                    )
                }
            }
        }
    }

    if (uiState.askPaymentPassword) {
        AlertDialog(
            onDismissRequest = {
                paymentPassword = ""
                viewModel.onPaymentPasswordDismiss()
            },
            title = { Text(text = stringResource(R.string.title_payment)) },
            text = {
                OutlinedTextField(
                    value = paymentPassword,
                    onValueChange = { paymentPassword = it },
                    label = { Text(text = "Password") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        paymentPassword = ""
                        viewModel.onPaymentPasswordConfirm()
                    },
                    enabled = paymentPassword.isNotBlank(),
                ) {
                    Text(text = stringResource(R.string.action_yes))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        paymentPassword = ""
                        viewModel.onPaymentPasswordDismiss()
                    },
                ) {
                    Text(text = stringResource(R.string.action_cancel))
                }
            },
        )
    }

    if (showLogoutDialog) {
        ConfirmDialog(
            title = stringResource(R.string.title_logout),
            message = stringResource(R.string.msg_logout_confirm),
            onConfirm = {
                showLogoutDialog = false
                viewModel.logout(onLogout)
            },
            onDismiss = { showLogoutDialog = false }
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
