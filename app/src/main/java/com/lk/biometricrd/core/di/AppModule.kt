package com.lk.biometricrd.core.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import com.osos.biometric.BiometricSecretStore
import com.osos.biometric.Biometrics
import com.osos.biometric.KeystoreSecrets
import com.lk.biometricrd.core.data.sources.local.AppPreferences
import com.lk.biometricrd.core.data.sources.local.AppPreferencesImpl
import com.lk.biometricrd.core.utils.constants.AppConstants
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(
    name = AppConstants.Prefs.DATASTORE_NAME
)

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideDataStore(
        @ApplicationContext context: Context
    ): DataStore<Preferences> {
        return context.dataStore
    }

    @Provides
    @Singleton
    fun provideAppPreferences(
        dataStore: DataStore<Preferences>
    ): AppPreferences = AppPreferencesImpl(dataStore)

    @Provides
    @Singleton
    fun provideBiometrics(
        @ApplicationContext context: Context
    ): Biometrics = Biometrics(context)

    @Provides
    @Singleton
    fun provideKeystoreSecrets(
        @ApplicationContext context: Context
    ): KeystoreSecrets = KeystoreSecrets(context)

    @Provides
    @Singleton
    fun provideBiometricSecretStore(
        secrets: KeystoreSecrets,
        biometrics: Biometrics,
    ): BiometricSecretStore = BiometricSecretStore(
        secrets = secrets,
        biometrics = biometrics,
        tokenAlias = AppConstants.Session.TOKEN,
        passwordAlias = AppConstants.Session.PASSWORD,
    )
}
