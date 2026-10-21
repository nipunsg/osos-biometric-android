package com.lk.biometricrd.feature.auth.presentation.splash

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.osos.biometric.BiometricSecretStore
import com.lk.biometricrd.feature.auth.presentation.model.SplashNavigationState
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@HiltViewModel
class SplashViewModel @Inject constructor(
    private val secrets: BiometricSecretStore,
) : ViewModel() {

    private val _navigationState = MutableStateFlow<SplashNavigationState?>(null)
    val navigationState: StateFlow<SplashNavigationState?> = _navigationState.asStateFlow()

    init {
        viewModelScope.launch {
            delay(1500)
            val hasToken = withContext(Dispatchers.IO) {
                secrets.clearPasswordIfEnrollmentChanged()
                !secrets.token().isNullOrBlank()
            }
            _navigationState.value = if (hasToken) {
                SplashNavigationState.NavigateToDashboard
            } else {
                SplashNavigationState.NavigateToLogin
            }
        }
    }
}
