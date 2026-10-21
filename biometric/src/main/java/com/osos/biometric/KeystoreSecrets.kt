package com.osos.biometric

import android.content.Context
import android.os.Build
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyInfo
import android.security.keystore.KeyPermanentlyInvalidatedException
import android.security.keystore.KeyProperties
import android.security.keystore.UserNotAuthenticatedException
import android.util.Base64
import java.security.InvalidKeyException
import java.security.KeyStore
import java.security.UnrecoverableKeyException
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec

/** Stores short secrets with an Android Keystore key. Callers choose the alias. */
class KeystoreSecrets(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun put(alias: String, value: String) {
        write(alias, value, cipher(Cipher.ENCRYPT_MODE, key(alias, auth = false)))
    }

    fun get(alias: String): String? {
        val payload = prefs.getString(alias, null) ?: return null
        return try {
            read(payload, cipher(Cipher.DECRYPT_MODE, key(alias, auth = false), payload))
        } catch (_: Exception) {
            null
        }
    }

    fun has(alias: String): Boolean = !prefs.getString(alias, null).isNullOrEmpty()

    /** Cipher that encrypts [alias] only after a Class 3 scan unlocks it. */
    fun prepareEncrypt(alias: String): Cipher? {
        repeat(2) {
            try {
                return cipher(Cipher.ENCRYPT_MODE, key(alias, auth = true))
            } catch (error: Exception) {
                if (!invalidated(error)) return null
                remove(alias)
            }
        }
        return null
    }

    /** Cipher that decrypts [alias] only after a Class 3 scan unlocks it. */
    fun prepareDecrypt(alias: String): Cipher? {
        val payload = prefs.getString(alias, null) ?: return null
        val secretKey = loadAuthKey(alias) ?: return null
        return try {
            cipher(Cipher.DECRYPT_MODE, secretKey, payload)
        } catch (_: UserNotAuthenticatedException) {
            null
        } catch (error: Exception) {
            if (invalidated(error)) remove(alias)
            null
        }
    }

    fun finishEncrypt(cipher: Cipher, alias: String, value: String): Boolean {
        return try {
            write(alias, value, cipher)
            true
        } catch (_: Exception) {
            false
        }
    }

    fun finishDecrypt(cipher: Cipher, alias: String): String? {
        val payload = prefs.getString(alias, null) ?: return null
        return try {
            read(payload, cipher)
        } catch (_: Exception) {
            null
        }
    }

    fun remove(alias: String) {
        prefs.edit().remove(alias).commit()
        val keyStore = keyStore()
        if (keyStore.containsAlias(alias)) keyStore.deleteEntry(alias)
    }

    private fun write(alias: String, value: String, cipher: Cipher) {
        val encrypted = cipher.doFinal(value.toByteArray(Charsets.UTF_8))
        val payload = Base64.encodeToString(cipher.iv, Base64.NO_WRAP) + ":" +
            Base64.encodeToString(encrypted, Base64.NO_WRAP)
        prefs.edit().putString(alias, payload).commit()
    }

    private fun read(payload: String, cipher: Cipher): String {
        val bytes = Base64.decode(payload.substringAfter(':'), Base64.NO_WRAP)
        return String(cipher.doFinal(bytes), Charsets.UTF_8)
    }

    private fun cipher(mode: Int, secretKey: SecretKey, payload: String? = null): Cipher {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        if (payload == null) {
            cipher.init(mode, secretKey)
        } else {
            val iv = Base64.decode(payload.substringBefore(':'), Base64.NO_WRAP)
            cipher.init(mode, secretKey, GCMParameterSpec(GCM_TAG_BITS, iv))
        }
        return cipher
    }

    private fun loadAuthKey(alias: String): SecretKey? {
        return try {
            val existing = keyStore().getKey(alias, null) as? SecretKey ?: return null
            if (authBound(existing)) existing else {
                remove(alias)
                null
            }
        } catch (error: Exception) {
            if (invalidated(error)) remove(alias)
            null
        }
    }

    private fun key(alias: String, auth: Boolean): SecretKey {
        val keyStore = keyStore()
        val existing = try {
            keyStore.getKey(alias, null) as? SecretKey
        } catch (error: Exception) {
            if (auth && invalidated(error)) remove(alias)
            null
        }
        if (existing != null && (!auth || authBound(existing))) return existing
        if (auth && existing != null) remove(alias)
        val builder = KeyGenParameterSpec.Builder(alias, PURPOSE)
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(256)
        if (auth) {
            builder.setUserAuthenticationRequired(true).setInvalidatedByBiometricEnrollment(true)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                builder.setUserAuthenticationParameters(0, KeyProperties.AUTH_BIOMETRIC_STRONG)
            } else {
                @Suppress("DEPRECATION")
                builder.setUserAuthenticationValidityDurationSeconds(-1)
            }
        }
        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
        generator.init(builder.build())
        return generator.generateKey()
    }

    private fun authBound(secretKey: SecretKey): Boolean {
        return try {
            val info = SecretKeyFactory.getInstance(secretKey.algorithm, ANDROID_KEYSTORE)
                .getKeySpec(secretKey, KeyInfo::class.java) as KeyInfo
            info.isUserAuthenticationRequired && info.isInvalidatedByBiometricEnrollment
        } catch (_: Exception) {
            false
        }
    }

    private fun invalidated(error: Exception): Boolean {
        return error is KeyPermanentlyInvalidatedException ||
            error is UnrecoverableKeyException ||
            (error is InvalidKeyException && error.cause is KeyPermanentlyInvalidatedException)
    }

    private fun keyStore(): KeyStore {
        return KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
    }

    private companion object {
        const val PREFS_NAME = "keystore_secrets"
        const val ANDROID_KEYSTORE = "AndroidKeyStore"
        const val TRANSFORMATION = "AES/GCM/NoPadding"
        const val GCM_TAG_BITS = 128
        const val PURPOSE = KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
    }
}
