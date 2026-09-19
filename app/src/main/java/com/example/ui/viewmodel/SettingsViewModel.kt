package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.example.data.security.SecurePreferencesManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val prefs = SecurePreferencesManager(application)

    private val _apiKey = MutableStateFlow(prefs.getGeminiApiKey())
    val apiKey: StateFlow<String> = _apiKey.asStateFlow()

    private val _defaultCurrency = MutableStateFlow(prefs.getDefaultCurrency())
    val defaultCurrency: StateFlow<String> = _defaultCurrency.asStateFlow()

    private val _language = MutableStateFlow(prefs.getAppLanguage())
    val language: StateFlow<String> = _language.asStateFlow()

    private val _deleteImageAfterOcr = MutableStateFlow(prefs.isDeleteImageAfterOcrEnabled())
    val deleteImageAfterOcr: StateFlow<Boolean> = _deleteImageAfterOcr.asStateFlow()

    private val _themeMode = MutableStateFlow(prefs.getThemeMode())
    val themeMode: StateFlow<String> = _themeMode.asStateFlow()

    private val _themeColor = MutableStateFlow(prefs.getThemeColor())
    val themeColor: StateFlow<String> = _themeColor.asStateFlow()

    private val _themeFont = MutableStateFlow(prefs.getThemeFont())
    val themeFont: StateFlow<String> = _themeFont.asStateFlow()

    private val _saveSuccessMessage = MutableStateFlow<String?>(null)
    val saveSuccessMessage: StateFlow<String?> = _saveSuccessMessage.asStateFlow()

    fun updateApiKey(newKey: String) {
        prefs.setGeminiApiKey(newKey)
        _apiKey.value = newKey
        _saveSuccessMessage.value = "Gemini API Key saved securely"
    }

    fun updateDefaultCurrency(currency: String) {
        prefs.setDefaultCurrency(currency)
        _defaultCurrency.value = currency
    }

    fun updateLanguage(lang: String) {
        prefs.setAppLanguage(lang)
        _language.value = lang
    }

    fun updateDeleteImageAfterOcr(enabled: Boolean) {
        prefs.setDeleteImageAfterOcr(enabled)
        _deleteImageAfterOcr.value = enabled
    }

    fun updateThemeMode(mode: String) {
        prefs.setThemeMode(mode)
        _themeMode.value = mode
    }

    fun updateThemeColor(color: String) {
        prefs.setThemeColor(color)
        _themeColor.value = color
    }

    fun updateThemeFont(font: String) {
        prefs.setThemeFont(font)
        _themeFont.value = font
    }

    fun clearSuccessMessage() {
        _saveSuccessMessage.value = null
    }
}
