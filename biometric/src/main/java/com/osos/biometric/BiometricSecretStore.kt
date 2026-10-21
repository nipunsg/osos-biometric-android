package com.osos.biometric

import javax.crypto.Cipher

/**
 * Token stays readable without a scan.
 * Password encrypts and decrypts only with a Class 3 [Cipher] unlocked by [Biometrics.prompt].
 * Each app passes its own alias names.
 */
class BiometricSecretStore(
    private val secrets: KeystoreSecrets,
    private val biometrics: Biometrics,
    private val tokenAlias: String,
    private val passwordAlias: String,
) {
    fun token(): String? = secrets.get(tokenAlias)

    fun saveToken(value: String) {
        secrets.put(tokenAlias, value)
    }

    fun biometricEnabled(): Boolean = secrets.has(passwordAlias)

    fun prepareEncrypt(): Cipher? = secrets.prepareEncrypt(passwordAlias)

    fun prepareDecrypt(): Cipher? = secrets.prepareDecrypt(passwordAlias)

    fun savePassword(cipher: Cipher, value: String): Boolean {
        return secrets.finishEncrypt(cipher, passwordAlias, value)
    }

    fun readPassword(cipher: Cipher): String? = secrets.finishDecrypt(cipher, passwordAlias)

    fun clearPasswordIfEnrollmentChanged() {
        if (!biometricEnabled()) return
        secrets.prepareDecrypt(passwordAlias)
        if (!biometricEnabled()) biometrics.clearEnrollment()
    }

    fun clearPassword() {
        secrets.remove(passwordAlias)
        biometrics.clearEnrollment()
    }

    fun clearToken() {
        secrets.remove(tokenAlias)
    }

    fun clearSecrets() {
        secrets.remove(tokenAlias)
        secrets.remove(passwordAlias)
        biometrics.clearEnrollment()
    }
}
