package com.handhot.app.data.local

import kotlinx.serialization.Serializable

@Serializable
data class DefaultSourcesConfig(
    val version: Int,
    val updated: String,
    val sources: List<DefaultSource>
)

@Serializable
data class DefaultSource(
    val name: String,
    val url: String,
    val selectors: DefaultSelectors,
    @Serializable val need_login: Boolean = false,
    @Serializable val use_webview: Boolean = false
)

@Serializable
data class DefaultSelectors(
    val title: String,
    val link: String,
    val summary: String? = null,
    val image: String? = null,
    val time: String? = null
)
