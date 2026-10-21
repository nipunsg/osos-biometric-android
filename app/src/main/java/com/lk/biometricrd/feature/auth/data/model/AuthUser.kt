package com.lk.biometricrd.feature.auth.data.model

data class AuthUser(
    val id: String,
    val email: String,
    val password: String,
    val displayName: String,
    val isActive: Boolean
)
