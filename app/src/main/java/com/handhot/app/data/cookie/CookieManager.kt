package com.handhot.app.data.cookie

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CookieManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val prefs: SharedPreferences by lazy {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        EncryptedSharedPreferences.create(
            context,
            "handhot_cookies",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    /**
     * Store cookie for a domain.
     */
    fun saveCookie(domain: String, cookie: String) {
        prefs.edit().putString(cookieKey(domain), cookie).apply()
    }

    /**
     * Get cookie for a domain.
     */
    fun getCookie(domain: String): String? {
        return prefs.getString(cookieKey(domain), null)
    }

    /**
     * Clear cookie for a domain.
     */
    fun clearCookie(domain: String) {
        prefs.edit().remove(cookieKey(domain)).apply()
    }

    /**
     * Check if cookie exists (and is non-empty) for a domain.
     */
    fun hasCookie(domain: String): Boolean {
        return !getCookie(domain).isNullOrBlank()
    }

    private fun cookieKey(domain: String) = "cookie_$domain"
}
