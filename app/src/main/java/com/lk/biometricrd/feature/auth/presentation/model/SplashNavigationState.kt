package com.lk.biometricrd.feature.auth.presentation.model

sealed interface SplashNavigationState {
    data object NavigateToLogin : SplashNavigationState
    data object NavigateToDashboard : SplashNavigationState
}
