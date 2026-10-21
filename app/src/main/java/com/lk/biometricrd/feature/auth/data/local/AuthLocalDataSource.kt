package com.lk.biometricrd.feature.auth.data.local

import com.lk.biometricrd.core.utils.constants.AppConstants
import com.lk.biometricrd.feature.auth.data.model.AuthData
import com.lk.biometricrd.feature.auth.data.model.AuthResponse
import com.lk.biometricrd.feature.auth.data.model.AuthUser
import javax.inject.Inject

class AuthLocalDataSource @Inject constructor() {

    private val users = listOf(
        AuthUser(
            id = "usr_001",
            email = AppConstants.Auth.DEMO_EMAIL,
            password = AppConstants.Auth.DEMO_PASSWORD,
            displayName = "Demo User",
            isActive = true
        ),
        AuthUser(
            id = "usr_002",
            email = AppConstants.Auth.INACTIVE_EMAIL,
            password = AppConstants.Auth.DEMO_PASSWORD,
            displayName = "Inactive User",
            isActive = false
        )
    )

    fun login(email: String, password: String): AuthResponse {
        val user = users.find {
            it.email.equals(email, ignoreCase = true) && it.password == password
        }

        return if (user != null) {
            if (user.isActive) {
                AuthResponse(
                    success = true,
                    message = "Authentication successful",
                    token = "0123456789",
                    data = AuthData(
                        id = user.id,
                        email = user.email,
                        displayName = user.displayName
                    )
                )
            } else {
                AuthResponse(
                    success = false,
                    message = "User account is inactive",
                    data = null
                )
            }
        } else {
            AuthResponse(
                success = false,
                message = "Invalid email or password",
                data = null
            )
        }
    }
}
