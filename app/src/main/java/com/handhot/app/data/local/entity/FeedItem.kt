package com.handhot.app.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "feed_items",
    foreignKeys = [
        ForeignKey(
            entity = FeedSource::class,
            parentColumns = ["id"],
            childColumns = ["source_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["hash"], unique = true),
        Index(value = ["source_id"]),
        Index(value = ["is_read", "created_at"]),
        Index(value = ["is_starred", "created_at"])
    ]
)
data class FeedItem(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = "source_id")
    val sourceId: Long,

    @ColumnInfo(name = "title")
    val title: String,

    @ColumnInfo(name = "summary")
    val summary: String? = null,

    @ColumnInfo(name = "cover_image_url")
    val coverImageUrl: String? = null,

    @ColumnInfo(name = "link")
    val link: String,

    @ColumnInfo(name = "hash")
    val hash: String,

    @ColumnInfo(name = "pub_time")
    val pubTime: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "is_read")
    val isRead: Boolean = false,

    @ColumnInfo(name = "is_starred")
    val isStarred: Boolean = false,

    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis()
)
