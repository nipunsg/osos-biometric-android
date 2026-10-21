package com.lk.biometricrd.feature.auth.data.model

data class AuthResponse(
    val success: Boolean,
    val message: String,
    val token: String? = null,
    val data: AuthData? = null
)

data class AuthData(
    val id: String,
    val email: String,
    val displayName: String
)
