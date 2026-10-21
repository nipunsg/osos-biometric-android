package com.lk.biometricrd.core.data

sealed class DBResource<out T> {
    data class Success<out T>(val value: T) : DBResource<T>()
    data class Error(val exception: Exception) : DBResource<Nothing>()
}
