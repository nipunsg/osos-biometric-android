package com.lk.biometricrd.feature.auth.domain.model

data class AuthEntity(
    val success: Boolean,
    val message: String,
    val token: String? = null,
    val user: UserEntity? = null
)

data class UserEntity(
    val id: String,
    val email: String,
    val displayName: String
)
