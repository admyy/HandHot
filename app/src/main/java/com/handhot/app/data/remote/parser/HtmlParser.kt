package com.handhot.app.data.remote.parser

import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import org.jsoup.select.Elements
import java.net.URI

/**
 * HTML parser using Jsoup CSS selectors.
 * Supports automatic og:image fallback and multi-path favicon detection.
 */
object HtmlParser {

    /**
     * Parse a list of items from HTML using CSS selectors.
     */
    fun parseItems(
        html: String,
        baseUrl: String,
        selectorTitle: String,
        selectorLink: String,
        selectorSummary: String?,
        selectorImage: String?,
        selectorTime: String?
    ): List<ParsedItemData> {
        val doc = Jsoup.parse(html, baseUrl)
        val titleElements = doc.select(selectorTitle)
        val summaryElements = if (selectorSummary != null) doc.select(selectorSummary) else Elements()
        val imageElements = if (selectorImage != null) doc.select(selectorImage) else Elements()
        val timeElements = if (selectorTime != null) doc.select(selectorTime) else Elements()

        // Try OG image as fallback for cover images
        val ogImage = extractOgImage(doc, baseUrl)

        val items = mutableListOf<ParsedItemData>()
        for (i in titleElements.indices) {
            val titleEl = titleElements[i]
            val title = titleEl.text().trim()
            if (title.isBlank()) continue

            val link = extractLink(titleEl, selectorLink, baseUrl) ?: continue

            val summary = if (i < summaryElements.size) {
                summaryElements[i].text().trim().take(50)
            } else null

            val image = when {
                i < imageElements.size -> extractImageUrl(imageElements[i], baseUrl)
                ogImage != null -> ogImage
                else -> null
            }

            val pubTime = if (i < timeElements.size) {
                parseTime(timeElements[i])
            } else null

            items.add(ParsedItemData(title, link, summary, image, pubTime))
        }
        return items
    }

    /**
     * Extract the og:image meta tag from the document.
     */
    private fun extractOgImage(doc: Document, baseUrl: String): String? {
        val meta = doc.selectFirst("meta[property=og:image]") ?: return null
        val content = meta.attr("content") ?: return null
        return resolveUrl(content, baseUrl)
    }

    /**
     * Extract favicon URL from multiple possible sources.
     * Priority: <link rel="icon"> → <link rel="shortcut icon"> → /favicon.ico
     */
    fun extractFavicon(html: String, baseUrl: String): String? {
        val doc = Jsoup.parse(html, baseUrl)

        // 1. <link rel="icon" href="...">
        doc.selectFirst("link[rel=icon]")?.attr("href")?.let {
            return resolveUrl(it, baseUrl)
        }

        // 2. <link rel="shortcut icon" href="...">
        doc.selectFirst("link[rel=\"shortcut icon\"]")?.attr("href")?.let {
            return resolveUrl(it, baseUrl)
        }

        // 3. Fallback to /favicon.ico
        return try {
            val uri = URI(baseUrl)
            "${uri.scheme}://${uri.host}/favicon.ico"
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Extract the domain-level favicon URL from just a base URL (without HTML).
     */
    fun faviconUrlFromDomain(baseUrl: String): String? {
        return try {
            val uri = URI(baseUrl)
            "${uri.scheme}://${uri.host}/favicon.ico"
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Detect if the response HTML indicates a login page.
     */
    fun detectLoginPage(html: String, responseUrl: String): Boolean {
        val lower = html.lowercase()
        // Check common login indicators
        val loginKeywords = listOf("login", "passport", "signin", "sign in", "登录", "注册")
        val hasLoginWord = loginKeywords.any { lower.contains(it) }
        // Check URL path
        val urlLower = responseUrl.lowercase()
        val hasLoginUrl = urlLower.contains("login") || urlLower.contains("passport")
                || urlLower.contains("signin")
        return hasLoginWord && hasLoginUrl
    }

    /**
     * Detect if response indicates a captcha challenge.
     * Tightened: requires both keyword match AND specific HTTP status code.
     */
    fun detectCaptcha(html: String, statusCode: Int): Boolean {
        if (statusCode !in listOf(403, 429, 503)) return false
        val lower = html.lowercase()
        return lower.contains("captcha") || lower.contains("验证码")
                || lower.contains("滑块") || lower.contains("window.__captcha")
    }

    private fun extractLink(element: Element, selectorLink: String, baseUrl: String): String? {
        // If the element is an anchor, get href directly
        val linkEl = if (element.tagName() == "a") element else element.selectFirst("a")
        val href = linkEl?.attr("href") ?: element.attr("href")
        if (href.isBlank()) return null
        return resolveUrl(href, baseUrl)
    }

    private fun extractImageUrl(element: Element, baseUrl: String): String? {
        val imgEl = if (element.tagName() == "img") element else element.selectFirst("img")
        val src = imgEl?.attr("src") ?: return null
        if (src.isBlank()) return null
        return resolveUrl(src, baseUrl)
    }

    private fun parseTime(element: Element): Long? {
        // Try datetime attribute first
        val datetime = element.attr("datetime")
        if (datetime.isNotBlank()) {
            return try {
                java.time.Instant.parse(datetime).toEpochMilli()
            } catch (_: Exception) { null }
        }
        // Try text content as ISO timestamp
        val text = element.text().trim()
        return try {
            java.time.Instant.parse(text).toEpochMilli()
        } catch (_: Exception) { null }
    }

    private fun resolveUrl(href: String, baseUrl: String): String {
        return try {
            val base = URI(baseUrl)
            val resolved = base.resolve(href)
            resolved.toString()
        } catch (_: Exception) {
            href
        }
    }
}

/**
 * Parsed item data (before hash generation).
 */
data class ParsedItemData(
    val title: String,
    val link: String,
    val summary: String?,
    val coverImageUrl: String?,
    val pubTime: Long?
)
