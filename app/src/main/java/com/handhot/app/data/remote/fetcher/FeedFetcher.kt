package com.handhot.app.data.remote.fetcher

import com.handhot.app.data.local.entity.FeedSource
import com.handhot.app.data.remote.model.FetchResult
import com.handhot.app.data.remote.model.ParsedItem

/**
 * Abstract fetcher interface. Implementations handle public sources and login-required sources.
 */
interface FeedFetcher {
    suspend fun fetch(source: FeedSource): FetchResult
}

/**
 * Public fetcher: direct OkHttp GET + Jsoup parse.
 */
class PublicFetcher(
    private val okHttpClient: okhttp3.OkHttpClient,
    private val htmlParser: com.handhot.app.data.remote.parser.HtmlParser = com.handhot.app.data.remote.parser.HtmlParser
) : FeedFetcher {

    override suspend fun fetch(source: FeedSource): FetchResult {
        return try {
            val request = okhttp3.Request.Builder()
                .url(source.url)
                .header("User-Agent", com.handhot.app.utils.UserAgentPool.random())
                .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                .header("Accept-Language", "zh-CN,zh;q=0.9,en;q=0.8")
                .header("Accept-Encoding", "gzip, deflate, br")
                .header("Cache-Control", "max-age=0")
                .build()

            val response = okHttpClient.newCall(request).execute()
            val body = response.body?.string() ?: ""

            if (!response.isSuccessful) {
                val isBlocked = response.code in listOf(403, 429)
                val isCaptcha = htmlParser.detectCaptcha(body, response.code)
                return FetchResult(
                    sourceId = source.id,
                    items = emptyList(),
                    success = false,
                    error = "HTTP ${response.code}: ${response.message}",
                    isBlocked = isBlocked,
                    isCaptcha = isCaptcha,
                    statusCode = response.code
                )
            }

            // Detect captcha on 200 (unusual but possible)
            if (htmlParser.detectCaptcha(body, 200)) {
                return FetchResult(
                    sourceId = source.id,
                    items = emptyList(),
                    success = false,
                    error = "检测到验证码",
                    isCaptcha = true,
                    statusCode = 200
                )
            }

            // Parse items
            val parsedItems = htmlParser.parseItems(
                html = body,
                baseUrl = source.url,
                selectorTitle = source.selectorTitle,
                selectorLink = source.selectorLink,
                selectorSummary = source.selectorSummary,
                selectorImage = source.selectorImage,
                selectorTime = source.selectorTime
            )

            if (parsedItems.isEmpty()) {
                return FetchResult(
                    sourceId = source.id,
                    items = emptyList(),
                    success = true,
                    error = "抓取成功但无新内容（可能选择器已失效）",
                    statusCode = 200
                )
            }

            // Also extract favicon from the page
            val mappedItems = parsedItems.map { item ->
                ParsedItem(
                    title = item.title,
                    link = item.link,
                    summary = item.summary,
                    coverImageUrl = item.coverImageUrl,
                    pubTime = item.pubTime
                )
            }

            FetchResult(
                sourceId = source.id,
                items = mappedItems,
                success = true,
                statusCode = 200
            )
        } catch (e: java.net.SocketTimeoutException) {
            FetchResult(
                sourceId = source.id,
                items = emptyList(),
                success = false,
                error = "加载超时",
                statusCode = -1
            )
        } catch (e: java.net.UnknownHostException) {
            FetchResult(
                sourceId = source.id,
                items = emptyList(),
                success = false,
                error = "请检查网络连接",
                statusCode = -1
            )
        } catch (e: Exception) {
            FetchResult(
                sourceId = source.id,
                items = emptyList(),
                success = false,
                error = e.message ?: "未知错误",
                statusCode = -1
            )
        }
    }
}

/**
 * Login fetcher: uses stored cookie via CookieInterceptor (injected into OkHttp client).
 */
class LoginFetcher(
    private val okHttpClient: okhttp3.OkHttpClient,
    private val htmlParser: com.handhot.app.data.remote.parser.HtmlParser = com.handhot.app.data.remote.parser.HtmlParser
) : FeedFetcher {

    override suspend fun fetch(source: FeedSource): FetchResult {
        return try {
            val request = okhttp3.Request.Builder()
                .url(source.url)
                .header("User-Agent", com.handhot.app.utils.UserAgentPool.random())
                .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                .header("Accept-Language", "zh-CN,zh;q=0.9,en;q=0.8")
                .header("Accept-Encoding", "gzip, deflate, br")
                .header("Cache-Control", "max-age=0")
                .build()

            val response = okHttpClient.newCall(request).execute()
            val body = response.body?.string() ?: ""
            val finalUrl = response.request.url.toString()

            // Check for login redirect
            if (htmlParser.detectLoginPage(body, finalUrl)) {
                return FetchResult(
                    sourceId = source.id,
                    items = emptyList(),
                    success = false,
                    error = "登录已过期",
                    isLoginExpired = true,
                    statusCode = response.code
                )
            }

            if (!response.isSuccessful) {
                return FetchResult(
                    sourceId = source.id,
                    items = emptyList(),
                    success = false,
                    error = "HTTP ${response.code}",
                    isBlocked = response.code in listOf(403, 429),
                    statusCode = response.code
                )
            }

            val parsedItems = htmlParser.parseItems(
                html = body,
                baseUrl = source.url,
                selectorTitle = source.selectorTitle,
                selectorLink = source.selectorLink,
                selectorSummary = source.selectorSummary,
                selectorImage = source.selectorImage,
                selectorTime = source.selectorTime
            )

            val mappedItems = parsedItems.map { item ->
                ParsedItem(
                    title = item.title,
                    link = item.link,
                    summary = item.summary,
                    coverImageUrl = item.coverImageUrl,
                    pubTime = item.pubTime
                )
            }

            FetchResult(
                sourceId = source.id,
                items = mappedItems,
                success = true,
                statusCode = 200
            )
        } catch (e: Exception) {
            FetchResult(
                sourceId = source.id,
                items = emptyList(),
                success = false,
                error = e.message ?: "未知错误",
                statusCode = -1
            )
        }
    }
}
