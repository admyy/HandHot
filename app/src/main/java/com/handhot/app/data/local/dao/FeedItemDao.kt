package com.handhot.app.data.local.dao

import androidx.room.*
import com.handhot.app.data.local.entity.FeedItem
import kotlinx.coroutines.flow.Flow

@Dao
interface FeedItemDao {
    // Ordered: starred first, then by pub_time desc
    @Query("""
        SELECT * FROM feed_items 
        WHERE is_read = 0 OR is_starred = 1 
        ORDER BY is_starred DESC, pub_time DESC
    """)
    fun observeUnreadAndStarred(): Flow<List<FeedItem>>

    @Query("SELECT * FROM feed_items WHERE source_id = :sourceId ORDER BY is_starred DESC, pub_time DESC")
    fun observeBySource(sourceId: Long): Flow<List<FeedItem>>

    @Query("SELECT * FROM feed_items WHERE hash = :hash LIMIT 1")
    suspend fun getByHash(hash: String): FeedItem?

    @Query("SELECT * FROM feed_items WHERE id = :id")
    suspend fun getById(id: Long): FeedItem?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: FeedItem): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIgnore(item: FeedItem): Long

    @Update
    suspend fun update(item: FeedItem)

    @Query("UPDATE feed_items SET is_read = 1 WHERE id = :id")
    suspend fun markRead(id: Long)

    @Query("UPDATE feed_items SET is_read = 1 WHERE source_id = :sourceId AND is_read = 0")
    suspend fun markAllReadBySource(sourceId: Long): Int

    @Query("UPDATE feed_items SET is_starred = CASE WHEN is_starred = 1 THEN 0 ELSE 1 END WHERE id = :id")
    suspend fun toggleStar(id: Long)

    @Query("UPDATE feed_items SET is_starred = :starred WHERE id = :id")
    suspend fun setStarred(id: Long, starred: Boolean)

    @Query("""
        DELETE FROM feed_items 
        WHERE is_read = 1 AND is_starred = 0 AND created_at < :beforeTimestamp
    """)
    suspend fun deleteOldReadItems(beforeTimestamp: Long): Int

    @Query("SELECT COUNT(*) FROM feed_items WHERE source_id = :sourceId AND is_read = 0")
    suspend fun getUnreadCountBySource(sourceId: Long): Int

    @Query("SELECT COUNT(*) FROM feed_items WHERE is_read = 0 OR is_starred = 1")
    suspend fun getVisibleCount(): Int

    @Query("""
        UPDATE feed_items 
        SET summary = :summary, cover_image_url = :imageUrl, pub_time = :pubTime, created_at = :now
        WHERE hash = :hash
    """)
    suspend fun updateByHash(hash: String, summary: String?, imageUrl: String?, pubTime: Long, now: Long)

    @Query("DELETE FROM feed_items WHERE source_id = :sourceId")
    suspend fun deleteBySource(sourceId: Long)
}
