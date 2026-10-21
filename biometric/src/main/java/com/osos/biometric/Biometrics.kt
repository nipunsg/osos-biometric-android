package com.osos.biometric

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyPermanentlyInvalidatedException
import android.security.keystore.KeyProperties
import android.security.keystore.UserNotAuthenticatedException
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import java.security.InvalidKeyException
import java.security.KeyStore
import java.security.UnrecoverableKeyException
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import kotlin.jvm.JvmOverloads

enum class BiometricSensor {
    Fingerprint,
    Face,
}

sealed interface BiometricResult {
    data class Success(val cipher: Cipher?) : BiometricResult
    data object Dismissed : BiometricResult
    /** Wrong finger or face. The prompt stays open, so more results can follow. */
    data object Failed : BiometricResult
    data object Unavailable : BiometricResult
}

/**
 * Class 3 prompt any feature can call.
 * A Class 2 face is not offered. Pass a [Cipher] to unlock a Keystore key with the scan.
 * [markEnrollment] arms a Class 3 check. [enrollmentChanged] is true after that fingerprint or face is replaced.
 */
class Biometrics(context: Context) {
    private val appContext = context.applicationContext
    private val manager = BiometricManager.from(appContext)

    fun sensor(): BiometricSensor? {
        if (!canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG)) return null
        val hasFingerprint = appContext.packageManager.hasSystemFeature(PackageManager.FEATURE_FINGERPRINT)
        return if (hasFingerprint) BiometricSensor.Fingerprint else BiometricSensor.Face
    }

    @JvmOverloads
    fun prompt(
        activity: FragmentActivity,
        title: String,
        subtitle: String,
        negativeButton: String,
        cipher: Cipher? = null,
        onResult: (BiometricResult) -> Unit,
    ) {
        val allowed = allowedAuthenticators()
        if (allowed == null) {
            onResult(BiometricResult.Unavailable)
            return
        }
        val prompt = BiometricPrompt(
            activity,
            ContextCompat.getMainExecutor(activity),
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    onResult(BiometricResult.Success(result.cryptoObject?.cipher))
                }

                override fun onAuthenticationFailed() {
                    onResult(BiometricResult.Failed)
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    val cancelled = errorCode == BiometricPrompt.ERROR_CANCELED ||
                        errorCode == BiometricPrompt.ERROR_USER_CANCELED ||
                        errorCode == BiometricPrompt.ERROR_NEGATIVE_BUTTON
                    onResult(if (cancelled) BiometricResult.Dismissed else BiometricResult.Unavailable)
                }
            },
        )
        val info = BiometricPrompt.PromptInfo.Builder()
            .setTitle(title)
            .setSubtitle(subtitle)
            .setNegativeButtonText(negativeButton)
            .setAllowedAuthenticators(allowed)
            .build()
        try {
            if (cipher == null) {
                prompt.authenticate(info)
            } else {
                prompt.authenticate(info, BiometricPrompt.CryptoObject(cipher))
            }
        } catch (_: Exception) {
            onResult(BiometricResult.Unavailable)
        }
    }

    fun markEnrollment() {
        if (!canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG)) return
        clearEnrollment()
        val builder = KeyGenParameterSpec.Builder(GUARD_ALIAS, PURPOSE)
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setUserAuthenticationRequired(true)
            .setInvalidatedByBiometricEnrollment(true)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            builder.setUserAuthenticationParameters(0, KeyProperties.AUTH_BIOMETRIC_STRONG)
        } else {
            @Suppress("DEPRECATION")
            builder.setUserAuthenticationValidityDurationSeconds(-1)
        }
        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
        generator.init(builder.build())
        generator.generateKey()
    }

    fun enrollmentChanged(): Boolean {
        return try {
            val key = keyStore().getKey(GUARD_ALIAS, null) as? SecretKey ?: return false
            Cipher.getInstance(TRANSFORMATION).init(Cipher.ENCRYPT_MODE, key)
            false
        } catch (_: UserNotAuthenticatedException) {
            false
        } catch (_: KeyPermanentlyInvalidatedException) {
            true
        } catch (_: UnrecoverableKeyException) {
            true
        } catch (error: InvalidKeyException) {
            error.cause is KeyPermanentlyInvalidatedException
        } catch (_: Exception) {
            false
        }
    }

    fun clearEnrollment() {
        val keyStore = keyStore()
        if (keyStore.containsAlias(GUARD_ALIAS)) {
            keyStore.deleteEntry(GUARD_ALIAS)
        }
    }

    private fun allowedAuthenticators(): Int? {
        return if (canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG)) {
            BiometricManager.Authenticators.BIOMETRIC_STRONG
        } else {
            null
        }
    }

    private fun canAuthenticate(authenticators: Int): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) return false
        return manager.canAuthenticate(authenticators) == BiometricManager.BIOMETRIC_SUCCESS
    }

    private fun keyStore(): KeyStore {
        return KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
    }

    private companion object {
        const val ANDROID_KEYSTORE = "AndroidKeyStore"
        const val GUARD_ALIAS = "biometric_enrollment_guard"
        const val TRANSFORMATION = "AES/GCM/NoPadding"
        const val PURPOSE = KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
    }
}
