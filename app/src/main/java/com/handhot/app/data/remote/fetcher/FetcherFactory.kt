package com.handhot.app.data.remote.fetcher

import android.content.Context
import com.handhot.app.data.local.entity.FeedSource
import dagger.hilt.android.qualifiers.ApplicationContext
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FetcherFactory @Inject constructor(
    private val publicClient: OkHttpClient,
    private val loginClient: OkHttpClient,
    @ApplicationContext private val context: Context
) {
    fun getFetcher(source: FeedSource): FeedFetcher {
        // WebView fetcher for SPA / JS-rendered sites
        if (source.useWebView) {
            return WebViewFetcher(context)
        }
        // Login-required sources use cookie-injected OkHttp client
        return if (source.needLogin) {
            LoginFetcher(loginClient)
        } else {
            PublicFetcher(publicClient)
        }
    }
}

/**
 * Provides OkHttp clients configured for HandHot.
 */
@Singleton
class OkHttpProvider @Inject constructor(
    private val cookieInterceptor: com.handhot.app.data.cookie.CookieInterceptor
) {
    /**
     * Client for public sources (no cookie injection).
     */
    val publicClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .writeTimeout(15, TimeUnit.SECONDS)
            .followRedirects(true)
            .followSslRedirects(true)
            .build()
    }

    /**
     * Client for login-required sources (with cookie injection).
     */
    val loginClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .writeTimeout(15, TimeUnit.SECONDS)
            .followRedirects(true)
            .followSslRedirects(true)
            .addInterceptor(cookieInterceptor)
            .build()
    }
}
