package com.lk.biometricrd.feature.auth.presentation.login

object LoginInputValidator {

    fun isValid(email: String, password: String): Boolean {
        return email.isNotBlank() && password.isNotBlank()
    }
}
