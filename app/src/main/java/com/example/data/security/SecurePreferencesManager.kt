package com.example.data.security

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.example.BuildConfig

class SecurePreferencesManager(context: Context) {
    private val prefs: SharedPreferences = try {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        EncryptedSharedPreferences.create(
            context,
            "enexpense_secure_prefs",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    } catch (e: Exception) {
        Log.e("SecurePrefs", "Fallback to standard shared preferences: ${e.message}")
        context.getSharedPreferences("enexpense_prefs_fallback", Context.MODE_PRIVATE)
    }

    companion object {
        private const val KEY_GEMINI_API_KEY = "gemini_api_key"
        private const val KEY_ACCOUNT_MODE = "account_mode"
        private const val KEY_DEFAULT_CURRENCY = "default_currency"
        private const val KEY_APP_LANGUAGE = "app_language"
        private const val KEY_DELETE_IMAGE_AFTER_OCR = "delete_image_after_ocr"
        private const val KEY_THEME_MODE = "theme_mode" // SYSTEM, LIGHT, DARK
        private const val KEY_THEME_COLOR = "theme_color" // EMERALD, SAPPHIRE, AMETHYST, AMBER, ROSE, CYAN
        private const val KEY_THEME_FONT = "theme_font" // DEFAULT, SERIF, MONOSPACE, SANS_SERIF
    }

    fun getGeminiApiKey(): String {
        val userKey = prefs.getString(KEY_GEMINI_API_KEY, "") ?: ""
        if (userKey.isNotBlank()) return userKey
        return try {
            val buildConfigKey = BuildConfig.GEMINI_API_KEY
            if (buildConfigKey.isNotBlank() && buildConfigKey != "MY_GEMINI_API_KEY") {
                buildConfigKey
            } else ""
        } catch (e: Exception) {
            ""
        }
    }

    fun setGeminiApiKey(apiKey: String) {
        prefs.edit().putString(KEY_GEMINI_API_KEY, apiKey.trim()).apply()
    }

    fun getAccountMode(): String {
        return prefs.getString(KEY_ACCOUNT_MODE, "PERSONAL") ?: "PERSONAL"
    }

    fun setAccountMode(mode: String) {
        prefs.edit().putString(KEY_ACCOUNT_MODE, mode).apply()
    }

    fun getDefaultCurrency(): String {
        return prefs.getString(KEY_DEFAULT_CURRENCY, "$") ?: "$"
    }

    fun setDefaultCurrency(currency: String) {
        prefs.edit().putString(KEY_DEFAULT_CURRENCY, currency).apply()
    }

    fun getAppLanguage(): String {
        return prefs.getString(KEY_APP_LANGUAGE, "en") ?: "en"
    }

    fun setAppLanguage(language: String) {
        prefs.edit().putString(KEY_APP_LANGUAGE, language).apply()
    }

    fun isDeleteImageAfterOcrEnabled(): Boolean {
        return prefs.getBoolean(KEY_DELETE_IMAGE_AFTER_OCR, false)
    }

    fun setDeleteImageAfterOcr(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_DELETE_IMAGE_AFTER_OCR, enabled).apply()
    }

    fun getThemeMode(): String {
        return prefs.getString(KEY_THEME_MODE, "SYSTEM") ?: "SYSTEM"
    }

    fun setThemeMode(mode: String) {
        prefs.edit().putString(KEY_THEME_MODE, mode).apply()
    }

    fun getThemeColor(): String {
        return prefs.getString(KEY_THEME_COLOR, "EMERALD") ?: "EMERALD"
    }

    fun setThemeColor(color: String) {
        prefs.edit().putString(KEY_THEME_COLOR, color).apply()
    }

    fun getThemeFont(): String {
        return prefs.getString(KEY_THEME_FONT, "DEFAULT") ?: "DEFAULT"
    }

    fun setThemeFont(font: String) {
        prefs.edit().putString(KEY_THEME_FONT, font).apply()
    }
}
