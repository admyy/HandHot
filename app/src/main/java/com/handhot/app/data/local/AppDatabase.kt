package com.handhot.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.handhot.app.data.local.dao.FeedItemDao
import com.handhot.app.data.local.dao.FeedSourceDao
import com.handhot.app.data.local.entity.FeedItem
import com.handhot.app.data.local.entity.FeedSource

@Database(
    entities = [FeedSource::class, FeedItem::class],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun feedSourceDao(): FeedSourceDao
    abstract fun feedItemDao(): FeedItemDao
}
