package com.handhot.app.data.cookie

import com.handhot.app.utils.UserAgentPool
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject
import javax.inject.Singleton

/**
 * OkHttp interceptor that injects stored cookies for the request domain.
 */
@Singleton
class CookieInterceptor @Inject constructor(
    private val cookieManager: CookieManager
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        val domain = UserAgentPool.extractDomain(originalRequest.url.toString())
        val cookie = cookieManager.getCookie(domain)

        val newRequest = if (cookie != null) {
            originalRequest.newBuilder()
                .header("Cookie", cookie)
                .build()
        } else {
            originalRequest
        }

        return chain.proceed(newRequest)
    }
}
