package com.handhot.app.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "feed_sources",
    indices = [
        Index(value = ["url"], unique = true),
        Index(value = ["enabled", "sort_order"])
    ]
)
data class FeedSource(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = "name")
    val name: String,

    @ColumnInfo(name = "url")
    val url: String,

    @ColumnInfo(name = "selector_title")
    val selectorTitle: String,

    @ColumnInfo(name = "selector_summary")
    val selectorSummary: String? = null,

    @ColumnInfo(name = "selector_image")
    val selectorImage: String? = null,

    @ColumnInfo(name = "selector_link")
    val selectorLink: String,

    @ColumnInfo(name = "selector_time")
    val selectorTime: String? = null,

    @ColumnInfo(name = "need_login")
    val needLogin: Boolean = false,

    @ColumnInfo(name = "use_webview")
    val useWebView: Boolean = false,

    @ColumnInfo(name = "enabled")
    val enabled: Boolean = true,

    @ColumnInfo(name = "last_fetch_time")
    val lastFetchTime: Long = 0,

    @ColumnInfo(name = "fail_count")
    val failCount: Int = 0,

    @ColumnInfo(name = "last_error")
    val lastError: String? = null,

    @ColumnInfo(name = "sort_order")
    val sortOrder: Int = 0,

    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis()
)
