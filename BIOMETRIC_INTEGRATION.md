# Biometric Integration

Sign in with fingerprint or face instead of typing the password, following Google's guidance for apps that protect credentials:

- Only **Class 3 (`BIOMETRIC_STRONG`)** biometrics are accepted.
- The password is encrypted with an **Android Keystore key that only a successful Class 3 scan can unlock** (a `CryptoObject` is passed to the system prompt).
- The key is **destroyed automatically** when a fingerprint or face is added or changed.
- Biometric login is **off below Android 9 (API 28)**.

## Biometric flow

### First login
1. The user enters username/email and password and taps **Login**.
2. On success, the app saves the username and token (the host app decides where).
3. If the device has a Class 3 sensor, the app asks **"Enable biometric login?"**
   - **Yes + successful scan:** the password is encrypted and saved, then home opens.
   - **No or Cancel:** nothing is saved, then home opens.
4. With no Class 3 sensor, the dialog is skipped and the user always types the password.

### Next launch
1. Splash checks for a biometric enrollment change. If it changed, the saved password is deleted.
2. Token present → home with no scan. No token → login screen.
3. The login screen shows the saved username, a password field, Login, and **Scan**. Scan appears only when a password is saved and a Class 3 sensor exists.
4. **Scan + success:** the password is decrypted, the normal login runs, and the token is replaced.

### Other events

| Event | Result |
|---|---|
| Logout | Clears the token. Username and password stay, so Scan is still available. |
| Different user logs in | The old password is cleared, and the new user is offered biometric login. |
| Wrong finger or face | The prompt stays open. |
| Fingerprint or face added or changed | The key is invalidated, the password is deleted, and Scan is hidden. |

## Proofs

**Stored on disk:** encrypted data only.

```bash
adb shell run-as <your.package> cat shared_prefs/keystore_secrets.xml
```

```text
lk+j6GV63n3mf6J8 : IlkQUib5aXIitgG6J9s0Wferw55e0i7ACA==
      IV              encrypted password + tag
```

Neither the password nor the key is in this file. The key stays in the Keystore's secure hardware and cannot be exported.

**Runtime logs (debug build only):**

```text
D BIO : Scan incorrect, encrypted = lk+j6GV63n3mf6J8:IlkQUib5aXIitgG6J9s0Wferw55e0i7ACA==
D BIO : Scan Dismissed, encrypted = lk+j6GV63n3mf6J8:IlkQUib5aXIitgG6J9s0Wferw55e0i7ACA==
D BIO : Scan success,   password  = ********
```

Wrong or cancelled scans only see the encrypted value. The real password appears **only after a successful Class 3 match**.

> Remove `BIO` debug logs before release.

## Measuring device capability

```bash
adb shell pm list features | grep -i -E "biometric|fingerprint|face"   # Windows: findstr -i
adb shell dumpsys biometric
```

| Field | Value | Meaning |
|---|---|---|
| Feature | `android.hardware.fingerprint` | Fingerprint sensor present |
| Feature | `android.hardware.biometrics.face` | Face sensor present |
| `modality` | `2` / `8` | Fingerprint / Face |
| `updatedStrength` | `15` (0x0F) | Class 3 (STRONG): can unlock the password |
| `updatedStrength` | `255` (0xFF) | Class 2 (WEAK): cannot unlock it |

**Test device:** fingerprint and face both reported `15`, so either unlocks the password. When both are enrolled, **Android picks which one to use, not the app**, and whichever matches first wins.

## Limitations

- **API 24–27 (Android 7/8):** some devices don't invalidate the key when a fingerprint is added, so an old saved password could unlock with the new finger. The app cannot detect or fix this, so biometric login is **turned off below API 28** (`Biometrics.sensor()` returns `null`). Everyone can still install the app, and Android 7/8 users just type the password.
- **Class 2 face** cannot unlock the password. Those users fall back to fingerprint or typing.
- **Azure AD login** is not connected to biometric login.

---

## Architecture

```text
your-app/
├── app/          Your features (login, splash, home)
└── biometric/    Android library com.osos.biometric (minSdk 24, no Hilt, no app code)
    ├── Biometrics.kt            Class 3 check + system prompt
    ├── KeystoreSecrets.kt       Keystore keys + encrypted storage
    └── BiometricSecretStore.kt  Save, read, and clear by alias
```

- `BiometricSecretStore` uses `KeystoreSecrets` for storage and `Biometrics` for the prompt.
- Prompt results: `BiometricResult.Success(cipher)`, `Failed` (wrong finger or face, prompt stays open, if your copy includes it), `Dismissed`, or `Unavailable`.
- No DI framework, so Kotlin and Java apps can both use it.

---

## Integration

### 1. Add the module
1. Copy the `biometric/` folder into the project root.
2. In `settings.gradle`, add `include ':biometric'`.
3. In the app module's `build.gradle`, add `implementation project(':biometric')`.
4. Set the app's `minSdk` to 24 or higher.
5. **Java projects only:** add the Kotlin Gradle plugin to the root `build.gradle`, because the module is written in Kotlin.

### 2. Startup setup
Create the biometric objects **once**, when the app starts.

**Kotlin (DI):**
- Provide `Biometrics` as a singleton.
- Provide `BiometricSecretStore` as a singleton, built from `KeystoreSecrets`, `Biometrics`, `tokenAlias`, and `passwordAlias`.
- Inject the store and `Biometrics` wherever they're needed, such as the Login and Splash ViewModels.

**Java (wrapper class):**
- Add a class named `BiometricStore` that creates `Biometrics` and `BiometricSecretStore` in an `init(context)` method.
- Call `BiometricStore.init(this)` in `Application.onCreate()`.
- Expose simple static methods for screens to use: `hasSensor()`, `enabled()`, `prepareEncrypt()`, `savePassword()`, `prepareDecrypt()`, `readPassword()`, `clearIfEnrollmentChanged()`, `clearPassword()`, `saveToken()`, `token()`, and `prompt()`.
- The wrapper hides Kotlin details, so screens never see `Unit.INSTANCE`.

### 3. Set the aliases (the only project-specific values)

| Alias | What it stores | What to do |
|---|---|---|
| `passwordAlias` | The encrypted password. It can only be read after a Class 3 scan. | **Change it to a unique name per project**, for example `"gears_biometric_password"`. |
| `tokenAlias` | The encrypted login token. It can be read without a scan, so splash can skip login. | Use it to encrypt and save the token with `saveToken(token)`, and read it back with `token()`. |

> Each alias is the name of both the Keystore key and the saved entry. Different projects must use different names.

### 4. First login (typed)
1. The user enters username and password and taps **Login**.
2. On success, save the token with `saveToken(token)` and save the username in the app's preferences.
3. If `sensor()` is not `null` and `biometricEnabled()` is `false`, show **"Enable biometric login?"**
4. **No:** open home.
5. **Yes:** call `prepareEncrypt()`, then `prompt()` with that cipher.
   - **Success:** call `savePassword(cipher, password)`, then open home.
   - **Failed:** wait. The prompt stays open.
   - **Dismissed / Unavailable:** open home without saving.

### 5. Next launch (splash)
1. Call `clearPasswordIfEnrollmentChanged()`.
2. If `token()` returns a value, open home with no scan.
3. Otherwise, open the login screen.

### 6. Login screen
1. Fill in the saved username.
2. On every open or resume, call `clearPasswordIfEnrollmentChanged()`.
3. Show **Scan** only if `biometricEnabled()` is `true` and `sensor()` is not `null`.

### 7. Scan login
1. The user taps **Scan**.
2. Call `prepareDecrypt()`. If it returns `null`, hide Scan.
3. Call `prompt()` with that cipher.
   - **Success:** call `readPassword(cipher)`, run the normal login, save the new token with `saveToken()`, and open home.
   - **Failed:** wait. The prompt stays open.
   - **Dismissed:** stay on the login screen.
   - **Unavailable:** hide Scan.

### 8. Logout and user change
- **Logout:** call `clearToken()`. Keep the password and username, so Scan stays available.
- **Different user logs in:** after a successful login, if the username differs from the saved one, call `clearPassword()` **before checking `biometricEnabled()`**, so the new user is offered biometric login again.
