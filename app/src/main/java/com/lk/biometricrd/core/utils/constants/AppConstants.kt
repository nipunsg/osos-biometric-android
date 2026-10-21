package com.lk.biometricrd.core.utils.constants

object AppConstants {

    object Auth {
        const val DEMO_EMAIL = "demo@example.com"
        const val DEMO_PASSWORD = "password123"
        const val INACTIVE_EMAIL = "inactive@example.com"
        const val MOCK_DELAY_MS = 800L
    }

    object Prefs {
        const val DATASTORE_NAME = "app_preferences"
        const val KEY_USER_DATA = "user_data"
    }

    object Session {
        const val TOKEN = "session_token"
        const val PASSWORD = "biometric_password"
    }
}
