package com.lk.biometricrd.feature.auth.data.repository

import com.lk.biometricrd.core.data.DBResource
import com.lk.biometricrd.core.data.sources.BaseRepo
import com.lk.biometricrd.core.data.sources.local.AppPreferences
import com.lk.biometricrd.core.utils.constants.AppConstants
import com.lk.biometricrd.feature.auth.data.local.AuthLocalDataSource
import com.lk.biometricrd.feature.auth.data.mapper.AuthMapper
import com.lk.biometricrd.feature.auth.data.model.AppUserData
import com.lk.biometricrd.feature.auth.domain.model.AuthEntity
import com.lk.biometricrd.feature.auth.domain.repository.AuthRepository
import kotlinx.coroutines.delay
import javax.inject.Inject

class AuthRepositoryImpl @Inject constructor(
    private val localDataSource: AuthLocalDataSource,
    private val appPreferences: AppPreferences,
    private val authMapper: AuthMapper,
) : BaseRepo(), AuthRepository {

    override suspend fun login(
        email: String,
        password: String
    ): DBResource<AuthEntity> {
        return safeDbCall {
            delay(AppConstants.Auth.MOCK_DELAY_MS)

            val authEntity = authMapper.toEntity(localDataSource.login(email, password))

            authEntity.user?.let { user ->
                appPreferences.saveAppUserData(
                    AppUserData(user.id, user.email, user.displayName)
                )
            }

            authEntity
        }
    }

    override suspend fun logout() {
        appPreferences.clearDataStore()
    }
}
