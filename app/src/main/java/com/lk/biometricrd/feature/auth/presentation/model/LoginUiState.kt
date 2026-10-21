package com.lk.biometricrd.feature.auth.presentation.model

data class LoginUiState(
    val formData: LoginFormData = LoginFormData(),
    val savedEmail: String = "",
    val loginState: LoginState = LoginState.Idle,
    val showScan: Boolean = false,
    val scanUsesFace: Boolean = false,
    val askEnableBiometric: Boolean = false,
)

data class LoginFormData(
    val email: String = "",
    val password: String = "",
    val isPasswordVisible: Boolean = false
)

sealed interface LoginState {
    data object Idle : LoginState
    data object Loading : LoginState
    data object Success : LoginState
    data class Error(val message: String, val title: String? = null) : LoginState
}
