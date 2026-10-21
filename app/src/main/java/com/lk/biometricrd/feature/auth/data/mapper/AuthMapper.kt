package com.lk.biometricrd.feature.auth.data.mapper

import com.lk.biometricrd.feature.auth.data.model.AuthData
import com.lk.biometricrd.feature.auth.data.model.AuthResponse
import com.lk.biometricrd.feature.auth.domain.model.AuthEntity
import com.lk.biometricrd.feature.auth.domain.model.UserEntity
import javax.inject.Inject

class AuthMapper @Inject constructor() {

    fun toEntity(authResponse: AuthResponse): AuthEntity {
        return AuthEntity(
            success = authResponse.success,
            message = authResponse.message,
            token = authResponse.token,
            user = authResponse.data?.toUserEntity()
        )
    }

    private fun AuthData.toUserEntity(): UserEntity {
        return UserEntity(
            id = this.id,
            email = this.email,
            displayName = this.displayName
        )
    }
}
