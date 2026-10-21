package com.lk.biometricrd.feature.auth.data.model

import kotlinx.serialization.Serializable

@Serializable
data class AppUserData(
    val id: String,
    val email: String,
    val displayName: String
)
