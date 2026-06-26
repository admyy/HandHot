package com.handhot.app.data.local.dao

import androidx.room.*
import com.handhot.app.data.local.entity.FeedSource
import kotlinx.coroutines.flow.Flow

@Dao
interface FeedSourceDao {
    @Query("SELECT * FROM feed_sources ORDER BY sort_order ASC, created_at ASC")
    fun observeAll(): Flow<List<FeedSource>>

    @Query("SELECT * FROM feed_sources WHERE enabled = 1 ORDER BY sort_order ASC")
    fun observeEnabled(): Flow<List<FeedSource>>

    @Query("SELECT * FROM feed_sources WHERE enabled = 1")
    suspend fun getEnabled(): List<FeedSource>

    @Query("SELECT * FROM feed_sources WHERE id = :id")
    suspend fun getById(id: Long): FeedSource?

    @Query("SELECT * FROM feed_sources WHERE url = :url LIMIT 1")
    suspend fun getByUrl(url: String): FeedSource?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(source: FeedSource): Long

    @Update
    suspend fun update(source: FeedSource)

    @Delete
    suspend fun delete(source: FeedSource)

    @Query("DELETE FROM feed_sources WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("UPDATE feed_sources SET enabled = :enabled WHERE id = :id")
    suspend fun setEnabled(id: Long, enabled: Boolean)

    @Query("UPDATE feed_sources SET last_fetch_time = :time, fail_count = :failCount, last_error = :error WHERE id = :id")
    suspend fun updateFetchStatus(id: Long, time: Long, failCount: Int, error: String?)

    @Query("SELECT COUNT(*) FROM feed_sources WHERE enabled = 1")
    suspend fun getEnabledCount(): Int

    @Query("SELECT COUNT(*) FROM feed_sources")
    suspend fun getTotalCount(): Int
}
