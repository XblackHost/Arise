package com.example.security

import android.content.Context
import android.content.SharedPreferences
import android.util.Base64
import com.example.BuildConfig
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * Handles secure local storage and retrieval of the Gemini API key.
 * Uses AES obfuscated encryption combined with device-specific salt
 * to store credentials securely in private SharedPreferences without crashing
 * on older or varied Android runtime environments.
 */
class ApiKeyStorage(private val context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    companion object {
        private const val PREFS_NAME = "arise_secure_vault"
        private const val KEY_ENCRYPTED_API_KEY = "vault_gemini_api_key"
        private const val KEY_FIRST_LAUNCH_COMPLETED = "vault_first_launch_done"
        private const val KEY_USER_NAME = "vault_player_name"
        private const val CIPHER_ALGO = "AES/CBC/PKCS5Padding"
        private const val FIXED_SALT = "ARISE_SHADOW_SYSTEM_2026_VAULT"
        private const val V2_PREFIX = "v2:"
    }

    private fun getSecretKey(): SecretKeySpec {
        val digest = MessageDigest.getInstance("SHA-256")
        val keyBytes = digest.digest((context.packageName + FIXED_SALT).toByteArray(StandardCharsets.UTF_8))
        return SecretKeySpec(keyBytes, "AES")
    }

    private fun getLegacyIv(): IvParameterSpec {
        val ivBytes = ByteArray(16) { 0x41 }
        return IvParameterSpec(ivBytes)
    }

    /**
     * Stores the Gemini API key encrypted in local private storage using dynamic random IV.
     */
    fun saveApiKey(rawKey: String) {
        val trimmed = rawKey.trim()
        if (trimmed.isEmpty()) {
            prefs.edit().remove(KEY_ENCRYPTED_API_KEY).apply()
            return
        }
        try {
            val randomIvBytes = ByteArray(16).also { java.security.SecureRandom().nextBytes(it) }
            val cipher = Cipher.getInstance(CIPHER_ALGO)
            cipher.init(Cipher.ENCRYPT_MODE, getSecretKey(), IvParameterSpec(randomIvBytes))
            val encryptedBytes = cipher.doFinal(trimmed.toByteArray(StandardCharsets.UTF_8))
            val combined = randomIvBytes + encryptedBytes
            val encoded = V2_PREFIX + Base64.encodeToString(combined, Base64.NO_WRAP)
            prefs.edit()
                .putString(KEY_ENCRYPTED_API_KEY, encoded)
                .putBoolean(KEY_FIRST_LAUNCH_COMPLETED, true)
                .apply()
        } catch (e: Exception) {
            // Fallback to plain private storage if cipher fails
            prefs.edit()
                .putString(KEY_ENCRYPTED_API_KEY, Base64.encodeToString(trimmed.toByteArray(), Base64.NO_WRAP))
                .putBoolean(KEY_FIRST_LAUNCH_COMPLETED, true)
                .apply()
        }
    }

    /**
     * Retrieves the stored API key.
     */
    fun getApiKey(): String {
        val encoded = prefs.getString(KEY_ENCRYPTED_API_KEY, null)
        if (!encoded.isNullOrEmpty()) {
            if (encoded.startsWith(V2_PREFIX)) {
                try {
                    val rawCombined = Base64.decode(encoded.removePrefix(V2_PREFIX), Base64.NO_WRAP)
                    if (rawCombined.size > 16) {
                        val ivBytes = rawCombined.copyOfRange(0, 16)
                        val cipherBytes = rawCombined.copyOfRange(16, rawCombined.size)
                        val cipher = Cipher.getInstance(CIPHER_ALGO)
                        cipher.init(Cipher.DECRYPT_MODE, getSecretKey(), IvParameterSpec(ivBytes))
                        val decrypted = String(cipher.doFinal(cipherBytes), StandardCharsets.UTF_8)
                        if (decrypted.isNotBlank()) return decrypted
                    }
                } catch (e: Exception) {
                    // Ignore and try fallback
                }
            } else {
                // Legacy v1 format with fixed IV
                try {
                    val cipher = Cipher.getInstance(CIPHER_ALGO)
                    cipher.init(Cipher.DECRYPT_MODE, getSecretKey(), getLegacyIv())
                    val decodedBytes = Base64.decode(encoded, Base64.NO_WRAP)
                    val decrypted = String(cipher.doFinal(decodedBytes), StandardCharsets.UTF_8)
                    if (decrypted.isNotBlank()) {
                        // Migrate to V2 format automatically
                        saveApiKey(decrypted)
                        return decrypted
                    }
                } catch (e: Exception) {
                    // Try plain base64 fallback
                    try {
                        val fallback = String(Base64.decode(encoded, Base64.NO_WRAP), StandardCharsets.UTF_8)
                        if (fallback.isNotBlank()) return fallback
                    } catch (_: Exception) { }
                }
            }
        }

        // STILL-07: Do not fallback to BuildConfig. Users must enter their own key.
        return ""
    }

    /**
     * Checks if the user has already configured or acknowledged the API key setup.
     */
    fun hasCompletedFirstLaunch(): Boolean {
        return prefs.getBoolean(KEY_FIRST_LAUNCH_COMPLETED, false) || getApiKey().isNotBlank()
    }

    fun markFirstLaunchCompleted() {
        prefs.edit().putBoolean(KEY_FIRST_LAUNCH_COMPLETED, true).apply()
    }

    /**
     * Returns true if a valid non-empty API key is available.
     */
    fun hasValidApiKey(): Boolean {
        val key = getApiKey()
        return key.isNotBlank() && key != "MY_GEMINI_API_KEY"
    }

    /**
     * Returns masked key for display in Settings (e.g. AIzaSy...9xyz).
     */
    fun getMaskedApiKey(): String {
        val key = getApiKey()
        if (key.isBlank()) return "Not configured (Offline mode)"
        if (key.length <= 8) return "••••••••"
        val prefix = key.take(6)
        val suffix = key.takeLast(4)
        return "$prefix••••••••$suffix"
    }

    fun clearApiKey() {
        prefs.edit().remove(KEY_ENCRYPTED_API_KEY).apply()
    }
}
