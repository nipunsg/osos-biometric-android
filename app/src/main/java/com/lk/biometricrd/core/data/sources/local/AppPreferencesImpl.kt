package com.lk.biometricrd.core.data.sources.local

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.lk.biometricrd.core.utils.constants.AppConstants
import com.lk.biometricrd.feature.auth.data.model.AppUserData
import kotlinx.coroutines.flow.first
import kotlinx.serialization.json.Json

class AppPreferencesImpl(
    private val dataStore: DataStore<Preferences>
) : AppPreferences {

    private val json = Json { ignoreUnknownKeys = true }

    override suspend fun saveAppUserData(userData: AppUserData) {
        val userDataString = json.encodeToString(userData)
        dataStore.edit { preferences ->
            preferences[stringPreferencesKey(AppConstants.Prefs.KEY_USER_DATA)] = userDataString
        }
    }

    override suspend fun getAppUserData(): AppUserData? {
        val preferences = dataStore.data.first()
        val userDataString = preferences[stringPreferencesKey(AppConstants.Prefs.KEY_USER_DATA)]

        if (userDataString != null) {
            return json.decodeFromString<AppUserData>(userDataString)
        }

        return null
    }

    override suspend fun clearDataStore() {
        dataStore.edit { it.clear() }
    }
}
