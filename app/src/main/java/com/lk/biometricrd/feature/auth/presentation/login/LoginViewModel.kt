package com.lk.biometricrd.feature.auth.presentation.login

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.osos.biometric.BiometricResult
import com.osos.biometric.BiometricSecretStore
import com.osos.biometric.BiometricSensor
import com.osos.biometric.Biometrics
import com.lk.biometricrd.R
import com.lk.biometricrd.core.data.DBResource
import com.lk.biometricrd.core.data.sources.local.AppPreferences
import com.lk.biometricrd.feature.auth.domain.repository.AuthRepository
import com.lk.biometricrd.feature.auth.presentation.model.LoginState
import com.lk.biometricrd.feature.auth.presentation.model.LoginUiEvent
import com.lk.biometricrd.feature.auth.presentation.model.LoginUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.crypto.Cipher
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val preferences: AppPreferences,
    private val secrets: BiometricSecretStore,
    val biometrics: Biometrics,
    @ApplicationContext private val context: Context,
) : ViewModel() {

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    private val _uiEvent = Channel<LoginUiEvent>()
    val uiEvent = _uiEvent.receiveAsFlow()

    init {
        viewModelScope.launch(Dispatchers.IO) {
            val email = preferences.getAppUserData()?.email?.takeIf { it.isNotBlank() }.orEmpty()
            _uiState.update { it.copy(savedEmail = email) }
            refreshScan()
        }
    }

    fun onResume() {
        viewModelScope.launch(Dispatchers.IO) {
            refreshScan()
        }
    }

    fun onEmailChange(email: String) {
        _uiState.update {
            it.copy(
                formData = it.formData.copy(email = email),
                loginState = LoginState.Idle,
            )
        }
    }

    fun onPasswordChange(password: String) {
        _uiState.update {
            it.copy(
                formData = it.formData.copy(password = password),
                loginState = LoginState.Idle,
            )
        }
    }

    fun onTogglePasswordVisibility() {
        _uiState.update {
            it.copy(
                formData = it.formData.copy(
                    isPasswordVisible = !it.formData.isPasswordVisible,
                ),
            )
        }
    }

    fun onLoginClick() {
        val state = _uiState.value
        val email = state.savedEmail.ifBlank { state.formData.email.trim() }
        val password = state.formData.password
        if (!LoginInputValidator.isValid(email, password)) {
            showError(context.getString(R.string.msg_empty_fields))
            return
        }
        submitLogin(email, password, fromScan = false)
    }

    fun prepareScan(): Cipher? {
        secrets.clearPasswordIfEnrollmentChanged()
        val sensor = biometrics.sensor()
        if (sensor == null || !secrets.biometricEnabled()) {
            _uiState.update { it.copy(showScan = false, scanUsesFace = false) }
            return null
        }
        val cipher = secrets.prepareDecrypt()
        if (cipher == null) {
            _uiState.update { it.copy(showScan = false, scanUsesFace = false) }
        }
        return cipher
    }

    fun prepareEnable(): Cipher? {
        _uiState.update { it.copy(askEnableBiometric = false) }
        val cipher = secrets.prepareEncrypt()
        if (cipher == null) {
            sendEvent(LoginUiEvent.NavigateToDashboard)
        }
        return cipher
    }

    fun onEnableBiometricNo() {
        _uiState.update { it.copy(askEnableBiometric = false) }
        sendEvent(LoginUiEvent.NavigateToDashboard)
    }

    fun onPromptResult(enabling: Boolean, result: BiometricResult) {
        if (result is BiometricResult.Failed) return
        val cipher = (result as? BiometricResult.Success)?.cipher
        if (!enabling) {
            if (cipher == null) return
            val password = secrets.readPassword(cipher)
            val email = _uiState.value.savedEmail
            if (password.isNullOrEmpty() || email.isBlank()) {
                secrets.clearPassword()
                _uiState.update { it.copy(showScan = false, scanUsesFace = false) }
                return
            }
            submitLogin(email, password, fromScan = true)
            return
        }
        if (cipher != null) {
            secrets.savePassword(cipher, _uiState.value.formData.password)
        }
        sendEvent(LoginUiEvent.NavigateToDashboard)
    }

    fun onSignInAsNew() {
        viewModelScope.launch(Dispatchers.IO) {
            preferences.clearDataStore()
            secrets.clearSecrets()
            _uiState.value = LoginUiState()
        }
    }

    fun clearError() {
        _uiState.update { it.copy(loginState = LoginState.Idle) }
    }

    private fun submitLogin(email: String, password: String, fromScan: Boolean) {
        viewModelScope.launch {
            _uiState.update { it.copy(loginState = LoginState.Loading) }
            when (val result = authRepository.login(email, password)) {
                is DBResource.Success -> {
                    val auth = result.value
                    if (!auth.success || auth.token.isNullOrBlank()) {
                        if (fromScan) {
                            secrets.clearPassword()
                            _uiState.update { it.copy(showScan = false, scanUsesFace = false) }
                        }
                        showError(auth.message)
                        return@launch
                    }
                    withContext(Dispatchers.IO) {
                        secrets.saveToken(auth.token)
                    }
                    val ask = !fromScan && !secrets.biometricEnabled() && biometrics.sensor() != null
                    if (ask) {
                        _uiState.update {
                            it.copy(
                                loginState = LoginState.Idle,
                                askEnableBiometric = true,
                                savedEmail = email,
                            )
                        }
                    } else {
                        _uiState.update { it.copy(loginState = LoginState.Success, savedEmail = email) }
                        sendEvent(LoginUiEvent.NavigateToDashboard)
                    }
                }
                is DBResource.Error -> {
                    showError(result.exception.message ?: "Unknown error")
                }
            }
        }
    }

    private fun refreshScan() {
        secrets.clearPasswordIfEnrollmentChanged()
        val sensor = biometrics.sensor()
        _uiState.update {
            it.copy(
                showScan = secrets.biometricEnabled() && sensor != null,
                scanUsesFace = sensor == BiometricSensor.Face,
            )
        }
    }

    private fun showError(message: String) {
        _uiState.update { it.copy(loginState = LoginState.Error(message)) }
    }

    private fun sendEvent(event: LoginUiEvent) {
        viewModelScope.launch {
            _uiEvent.send(event)
        }
    }
}
