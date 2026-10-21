package com.lk.biometricrd.core.data.sources.local

import com.lk.biometricrd.feature.auth.data.model.AppUserData

interface AppPreferences {
    suspend fun saveAppUserData(userData: AppUserData)
    suspend fun getAppUserData(): AppUserData?
    suspend fun clearDataStore()
}
