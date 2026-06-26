package com.handhot.app.data.remote.fetcher

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.webkit.WebView
import android.webkit.WebViewClient
import com.handhot.app.data.local.entity.FeedSource
import com.handhot.app.data.remote.model.FetchResult
import com.handhot.app.data.remote.model.ParsedItem
import com.handhot.app.data.remote.parser.HtmlParser
import com.handhot.app.utils.UserAgentPool
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume

/**
 * Fetcher for SPA (Single Page Application) sites that render content via JavaScript.
 * Uses a headless WebView to load the page, wait for JS rendering, then extract DOM.
 */
class WebViewFetcher(
    private val context: Context
) : FeedFetcher {

    companion object {
        private const val JS_RENDER_DELAY_MS = 4000L  // Wait 4s for JS to render
        private const val FETCH_TIMEOUT_MS = 20_000L  // 20s total timeout
    }

    override suspend fun fetch(source: FeedSource): FetchResult {
        return withContext(Dispatchers.Main) {
            suspendCancellableCoroutine { continuation ->
                var isResumed = false
                val handler = Handler(Looper.getMainLooper())

                // Timeout runnable
                val timeoutRunnable = Runnable {
                    if (!isResumed) {
                        isResumed = true
                        continuation.resume(
                            FetchResult(
                                sourceId = source.id,
                                items = emptyList(),
                                success = false,
                                error = "加载超时",
                                statusCode = -1
                            )
                        )
                    }
                }
                handler.postDelayed(timeoutRunnable, FETCH_TIMEOUT_MS)

                val webView = WebView(context).apply {
                    settings.apply {
                        javaScriptEnabled = true
                        domStorageEnabled = true
                        loadWithOverviewMode = true
                        useWideViewPort = true
                        userAgentString = UserAgentPool.random()
                        blockNetworkImage = true
                    }

                    webViewClient = object : WebViewClient() {
                        override fun onPageFinished(view: WebView?, url: String?) {
                            handler.postDelayed({
                                if (isResumed) return@postDelayed
                                extractHtml(view, source, isResumed, handler, timeoutRunnable, continuation)
                            }, JS_RENDER_DELAY_MS)
                        }

                        override fun onReceivedError(
                            view: WebView?,
                            errorCode: Int,
                            description: String?,
                            failingUrl: String?
                        ) {
                            if (!isResumed) {
                                isResumed = true
                                handler.removeCallbacks(timeoutRunnable)
                                continuation.resume(
                                    FetchResult(
                                        sourceId = source.id,
                                        items = emptyList(),
                                        success = false,
                                        error = description ?: "加载失败",
                                        statusCode = errorCode
                                    )
                                )
                            }
                            view?.destroy()
                        }
                    }

                    loadUrl(source.url)
                }

                continuation.invokeOnCancellation {
                    if (!isResumed) {
                        isResumed = true
                        handler.removeCallbacks(timeoutRunnable)
                        webView.destroy()
                    }
                }
            }
        }
    }

    private fun extractHtml(
        webView: WebView?,
        source: FeedSource,
        isResumed: Boolean,
        handler: Handler,
        timeoutRunnable: Runnable,
        continuation: kotlinx.coroutines.CancellableContinuation<FetchResult>
    ) {
        if (webView == null || isResumed) return

        webView.evaluateJavascript("document.documentElement.outerHTML") { rawHtml ->
            if (isResumed) {
                webView.destroy()
                return@evaluateJavascript
            }

            // Clean up JSON-wrapped HTML string
            val html = if (rawHtml.startsWith("\"") && rawHtml.endsWith("\"")) {
                rawHtml.substring(1, rawHtml.length - 1)
                    .replace("\\\"", "\"")
                    .replace("\\n", "\n")
                    .replace("\\t", "\t")
                    .replace("\\\\", "\\")
            } else rawHtml

            val parsedItems = HtmlParser.parseItems(
                html = html,
                baseUrl = source.url,
                selectorTitle = source.selectorTitle,
                selectorLink = source.selectorLink,
                selectorSummary = source.selectorSummary,
                selectorImage = source.selectorImage,
                selectorTime = source.selectorTime
            )

            handler.removeCallbacks(timeoutRunnable)
            webView.destroy()

            if (isResumed) return@evaluateJavascript

            val mappedItems = parsedItems.map { item ->
                ParsedItem(
                    title = item.title,
                    link = item.link,
                    summary = item.summary,
                    coverImageUrl = item.coverImageUrl,
                    pubTime = item.pubTime
                )
            }

            continuation.resume(
                FetchResult(
                    sourceId = source.id,
                    items = mappedItems,
                    success = true,
                    error = if (mappedItems.isEmpty()) "抓取成功但无新内容（可能选择器已失效）" else null,
                    statusCode = 200
                )
            )
        }
    }
}
