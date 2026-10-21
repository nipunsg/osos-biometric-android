package com.lk.biometricrd.feature.auth.domain.repository

import com.lk.biometricrd.core.data.DBResource
import com.lk.biometricrd.feature.auth.domain.model.AuthEntity

interface AuthRepository {
    suspend fun login(email: String, password: String): DBResource<AuthEntity>
    suspend fun logout()
}
