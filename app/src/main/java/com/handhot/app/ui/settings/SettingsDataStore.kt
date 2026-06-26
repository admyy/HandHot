package com.handhot.app.ui.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore

/**
 * Shared DataStore instance for app settings.
 * Used by both SettingsViewModel and CleanupWorker.
 */
val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")
