package com.osos.biometric;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.fragment.app.FragmentActivity;

import java.util.function.Consumer;

import javax.crypto.Cipher;

import kotlin.Unit;

/**
 * Password encrypts only after a Class 3 scan.
 * The session token encrypts with a Keystore key that does not need a scan.
 * Prompt texts come from the osos_biometric_* strings, which an app can override.
 */
public final class BiometricStore {
    public static final String DEFAULT_TOKEN_ALIAS = "osos_biometric_token";
    public static final String DEFAULT_PASSWORD_ALIAS = "osos_biometric_password";

    private final Biometrics biometrics;
    private final BiometricSecretStore store;

    public BiometricStore(@NonNull Context context) {
        this(context, DEFAULT_TOKEN_ALIAS, DEFAULT_PASSWORD_ALIAS);
    }

    /** Existing installs keep their saved token and password only if they pass the aliases they used before. */
    public BiometricStore(@NonNull Context context, @NonNull String tokenAlias, @NonNull String passwordAlias) {
        Context app = context.getApplicationContext();
        biometrics = new Biometrics(app);
        store = new BiometricSecretStore(new KeystoreSecrets(app), biometrics, tokenAlias, passwordAlias);
    }

    public boolean shouldOfferEnable() {
        return hasSensor() && !store.biometricEnabled();
    }

    public boolean canUnlock() {
        return hasSensor() && store.biometricEnabled();
    }

    public void clearIfEnrollmentChanged() {
        store.clearPasswordIfEnrollmentChanged();
    }

    public void clear() {
        store.clearPassword();
    }

    public void saveToken(@NonNull String value) {
        store.saveToken(value);
    }

    @Nullable
    public String token() {
        return store.token();
    }

    public void clearToken() {
        store.clearToken();
    }

    /** Scans and saves [password]. [done] runs once the prompt closes, saved or not. */
    public void enable(@NonNull FragmentActivity activity, @NonNull String password, @NonNull Runnable done) {
        Cipher cipher = store.prepareEncrypt();
        if (cipher == null) {
            done.run();
            return;
        }
        prompt(activity, R.string.osos_biometric_title_enable, cipher, result -> {
            if (result instanceof BiometricResult.Failed) return;
            Cipher unlocked = unlockedCipher(result);
            if (unlocked != null) store.savePassword(unlocked, password);
            done.run();
        });
    }

    /** Scans and returns the saved password. A password that can't be used is cleared. */
    public void unlock(@NonNull FragmentActivity activity, @NonNull UnlockCallback callback) {
        Cipher cipher = store.prepareDecrypt();
        if (cipher == null) {
            store.clearPassword();
            callback.onUnavailable();
            return;
        }
        prompt(activity, R.string.osos_biometric_title_scan, cipher, result -> {
            if (!(result instanceof BiometricResult.Success)) {
                if (result instanceof BiometricResult.Unavailable) callback.onUnavailable();
                return;
            }
            Cipher unlocked = unlockedCipher(result);
            String password = unlocked == null ? null : store.readPassword(unlocked);
            if (password == null || password.isEmpty()) {
                store.clearPassword();
                callback.onUnavailable();
            } else {
                callback.onPassword(password);
            }
        });
    }

    private boolean hasSensor() {
        return biometrics.sensor() != null;
    }

    private void prompt(FragmentActivity activity, @StringRes int title, Cipher cipher, Consumer<BiometricResult> onResult) {
        biometrics.prompt(
                activity,
                activity.getString(title),
                activity.getString(R.string.osos_biometric_msg_scan),
                activity.getString(R.string.osos_biometric_action_cancel),
                cipher,
                result -> {
                    onResult.accept(result);
                    return Unit.INSTANCE;
                }
        );
    }

    private static Cipher unlockedCipher(BiometricResult result) {
        return result instanceof BiometricResult.Success
                ? ((BiometricResult.Success) result).getCipher()
                : null;
    }

    public interface UnlockCallback {
        void onPassword(@NonNull String password);

        void onUnavailable();
    }
}
