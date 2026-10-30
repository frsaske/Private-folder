package com.example.data

import android.content.Context
import android.content.SharedPreferences
import java.security.MessageDigest

class SecurityPreferences(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("private_folder_security", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_IS_SETUP_COMPLETED = "is_setup_completed"
        private const val KEY_PIN_HASH = "pin_hash"
        private const val KEY_RECOVERY_EMAIL = "recovery_email"
        private const val KEY_BIOMETRICS_ENABLED = "biometrics_enabled"
        private const val KEY_LOCK_APP_ENABLED = "lock_app_enabled"
        const val DEFAULT_BACKUP_EMAIL = "Tdurgesh2009@gmail.com"
    }

    var isSetupCompleted: Boolean
        get() = prefs.getBoolean(KEY_IS_SETUP_COMPLETED, false)
        set(value) = prefs.edit().putBoolean(KEY_IS_SETUP_COMPLETED, value).apply()

    var recoveryEmail: String
        get() = prefs.getString(KEY_RECOVERY_EMAIL, DEFAULT_BACKUP_EMAIL) ?: DEFAULT_BACKUP_EMAIL
        set(value) = prefs.edit().putString(KEY_RECOVERY_EMAIL, value).apply()

    var isBiometricsEnabled: Boolean
        get() = prefs.getBoolean(KEY_BIOMETRICS_ENABLED, true)
        set(value) = prefs.edit().putBoolean(KEY_BIOMETRICS_ENABLED, value).apply()

    var isLockAppEnabled: Boolean
        get() = prefs.getBoolean(KEY_LOCK_APP_ENABLED, true)
        set(value) = prefs.edit().putBoolean(KEY_LOCK_APP_ENABLED, value).apply()

    fun setPin(pin: String) {
        val hash = hashString(pin)
        prefs.edit()
            .putString(KEY_PIN_HASH, hash)
            .putBoolean(KEY_IS_SETUP_COMPLETED, true)
            .apply()
    }

    fun verifyPin(pin: String): Boolean {
        val savedHash = prefs.getString(KEY_PIN_HASH, null) ?: return false
        return savedHash == hashString(pin)
    }

    fun verifyRecoveryEmail(email: String): Boolean {
        val currentEmail = recoveryEmail.trim().lowercase()
        val candidate = email.trim().lowercase()
        return currentEmail == candidate || candidate == DEFAULT_BACKUP_EMAIL.lowercase()
    }

    private fun hashString(input: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(input.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }
}
