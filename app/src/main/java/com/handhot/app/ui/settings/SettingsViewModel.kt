package com.handhot.app.ui.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

data class SettingsState(
    val allowMobileData: Boolean = false,
    val wifiOnlyImages: Boolean = true,
    val retentionDays: Int = 7,
    val darkMode: String = "system", // system, light, dark
    val coolDownSeconds: Int = 60,
    val fontSize: String = "medium", // small, medium, large, xlarge
    val autoReadDelay: String = "2" // off, 1, 2, 5
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    @ApplicationContext private val context: Context
) : ViewModel() {

    companion object {
        val KEY_MOBILE_DATA = booleanPreferencesKey("allow_mobile_data")
        val KEY_WIFI_ONLY_IMAGES = booleanPreferencesKey("wifi_only_images")
        val KEY_RETENTION_DAYS = intPreferencesKey("retention_days")
        val KEY_DARK_MODE = stringPreferencesKey("dark_mode")
        val KEY_COOL_DOWN = intPreferencesKey("cool_down_seconds")
        val KEY_FONT_SIZE = stringPreferencesKey("font_size")
        val KEY_AUTO_READ_DELAY = stringPreferencesKey("auto_read_delay")
    }

    val settingsState: StateFlow<SettingsState> = context.dataStore.data
        .map { prefs ->
            SettingsState(
                allowMobileData = prefs[KEY_MOBILE_DATA] ?: false,
                wifiOnlyImages = prefs[KEY_WIFI_ONLY_IMAGES] ?: true,
                retentionDays = prefs[KEY_RETENTION_DAYS] ?: 7,
                darkMode = prefs[KEY_DARK_MODE] ?: "system",
                coolDownSeconds = prefs[KEY_COOL_DOWN] ?: 60,
                fontSize = prefs[KEY_FONT_SIZE] ?: "medium",
                autoReadDelay = prefs[KEY_AUTO_READ_DELAY] ?: "2"
            )
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SettingsState())

    fun setAllowMobileData(value: Boolean) = set(KEY_MOBILE_DATA, value)
    fun setWifiOnlyImages(value: Boolean) = set(KEY_WIFI_ONLY_IMAGES, value)
    fun setRetentionDays(days: Int) = set(KEY_RETENTION_DAYS, days)
    fun setDarkMode(mode: String) = set(KEY_DARK_MODE, mode)
    fun setCoolDownSeconds(seconds: Int) = set(KEY_COOL_DOWN, seconds)
    fun setFontSize(size: String) = set(KEY_FONT_SIZE, size)
    fun setAutoReadDelay(delay: String) = set(KEY_AUTO_READ_DELAY, delay)

    private fun <T> set(key: Preferences.Key<T>, value: T) {
        viewModelScope.launch {
            context.dataStore.edit { prefs -> prefs[key] = value }
        }
    }
}
