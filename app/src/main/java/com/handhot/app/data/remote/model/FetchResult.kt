package com.handhot.app.data.remote.model

data class FetchResult(
    val sourceId: Long,
    val items: List<ParsedItem>,
    val success: Boolean,
    val error: String? = null,
    val isBlocked: Boolean = false,
    val isLoginExpired: Boolean = false,
    val isCaptcha: Boolean = false,
    val statusCode: Int = 200
)

data class ParsedItem(
    val title: String,
    val link: String,
    val summary: String?,
    val coverImageUrl: String?,
    val pubTime: Long?
)
