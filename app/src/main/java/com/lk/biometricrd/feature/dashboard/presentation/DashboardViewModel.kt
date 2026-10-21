package com.lk.biometricrd.feature.dashboard.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.osos.biometric.BiometricResult
import com.osos.biometric.BiometricSecretStore
import com.osos.biometric.Biometrics
import com.lk.biometricrd.core.data.sources.local.AppPreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class DashboardUiState(
    val displayName: String = "",
    val showPaymentSuccess: Boolean = false,
    val askPaymentPassword: Boolean = false,
)

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val appPreferences: AppPreferences,
    private val secrets: BiometricSecretStore,
    val biometrics: Biometrics,
) : ViewModel() {

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    init {
        loadUser()
    }

    private fun loadUser() {
        viewModelScope.launch {
            val displayName = appPreferences.getAppUserData()?.displayName.orEmpty()
            _uiState.update { it.copy(displayName = displayName) }
        }
    }

    fun startPayment(): Boolean {
        val useBiometric = biometrics.sensor() != null
        _uiState.update {
            it.copy(
                showPaymentSuccess = false,
                askPaymentPassword = !useBiometric,
            )
        }
        return useBiometric
    }

    fun onPromptResult(result: BiometricResult) {
        when (result) {
            is BiometricResult.Success -> _uiState.update {
                it.copy(showPaymentSuccess = true, askPaymentPassword = false)
            }
            BiometricResult.Unavailable -> _uiState.update {
                it.copy(showPaymentSuccess = false, askPaymentPassword = true)
            }
            BiometricResult.Dismissed, BiometricResult.Failed -> Unit
        }
    }

    fun onPaymentPasswordDismiss() {
        _uiState.update { it.copy(askPaymentPassword = false) }
    }

    fun onPaymentPasswordConfirm() {
        _uiState.update { it.copy(askPaymentPassword = false, showPaymentSuccess = true) }
    }

    fun logout(onLoggedOut: () -> Unit) {
        viewModelScope.launch {
            secrets.clearToken()
            onLoggedOut()
        }
    }
}
