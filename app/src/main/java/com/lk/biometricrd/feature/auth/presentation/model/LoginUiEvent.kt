package com.lk.biometricrd.feature.auth.presentation.model

sealed interface LoginUiEvent {
    data object NavigateToDashboard : LoginUiEvent
}
