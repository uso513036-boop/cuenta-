package com.example.security

import android.content.Context
import android.content.SharedPreferences

class SecurityPreferences(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("multispace_sec_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_PIN_HASH = "pin_hash"
        private const val KEY_DECOY_PIN_HASH = "decoy_pin_hash"
        private const val KEY_PIN_ENABLED = "pin_enabled"
        private const val KEY_CAMOUFLAGE_ENABLED = "camouflage_enabled"
        private const val KEY_BIOMETRICS_ENABLED = "biometrics_enabled"
        private const val KEY_AUTO_LOCK_MINUTES = "auto_lock_minutes"
        private const val KEY_WIPE_ON_EXIT = "wipe_on_exit"
        private const val KEY_SCREENSHOT_PROTECTION = "screenshot_protection"
        private const val KEY_IS_LOCKED = "is_locked"
    }

    var isPinEnabled: Boolean
        get() = prefs.getBoolean(KEY_PIN_ENABLED, false)
        set(value) = prefs.edit().putBoolean(KEY_PIN_ENABLED, value).apply()

    var isCamouflageEnabled: Boolean
        get() = prefs.getBoolean(KEY_CAMOUFLAGE_ENABLED, false)
        set(value) = prefs.edit().putBoolean(KEY_CAMOUFLAGE_ENABLED, value).apply()

    var isBiometricsEnabled: Boolean
        get() = prefs.getBoolean(KEY_BIOMETRICS_ENABLED, false)
        set(value) = prefs.edit().putBoolean(KEY_BIOMETRICS_ENABLED, value).apply()

    var autoLockMinutes: Int
        get() = prefs.getInt(KEY_AUTO_LOCK_MINUTES, 5)
        set(value) = prefs.edit().putInt(KEY_AUTO_LOCK_MINUTES, value).apply()

    var isWipeOnExitEnabled: Boolean
        get() = prefs.getBoolean(KEY_WIPE_ON_EXIT, false)
        set(value) = prefs.edit().putBoolean(KEY_WIPE_ON_EXIT, value).apply()

    var isScreenshotProtectionEnabled: Boolean
        get() = prefs.getBoolean(KEY_SCREENSHOT_PROTECTION, false)
        set(value) = prefs.edit().putBoolean(KEY_SCREENSHOT_PROTECTION, value).apply()

    fun setMasterPin(pin: String) {
        val hash = CryptoEngine.hashPin(pin)
        prefs.edit()
            .putString(KEY_PIN_HASH, hash)
            .putBoolean(KEY_PIN_ENABLED, pin.isNotEmpty())
            .apply()
    }

    fun setDecoyPin(pin: String) {
        val hash = if (pin.isNotEmpty()) CryptoEngine.hashPin(pin) else ""
        prefs.edit().putString(KEY_DECOY_PIN_HASH, hash).apply()
    }

    fun verifyMasterPin(enteredPin: String): Boolean {
        val storedHash = prefs.getString(KEY_PIN_HASH, "") ?: ""
        if (storedHash.isEmpty()) return true
        return CryptoEngine.verifyPin(enteredPin, storedHash)
    }

    fun verifyDecoyPin(enteredPin: String): Boolean {
        val decoyHash = prefs.getString(KEY_DECOY_PIN_HASH, "") ?: ""
        if (decoyHash.isEmpty()) return false
        return CryptoEngine.verifyPin(enteredPin, decoyHash)
    }

    fun hasPinConfigured(): Boolean {
        val stored = prefs.getString(KEY_PIN_HASH, "") ?: ""
        return stored.isNotEmpty() && isPinEnabled
    }
}
