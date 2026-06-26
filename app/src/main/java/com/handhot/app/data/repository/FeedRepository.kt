package com.handhot.app.data.repository

import com.handhot.app.data.local.dao.FeedItemDao
import com.handhot.app.data.local.dao.FeedSourceDao
import com.handhot.app.data.local.entity.FeedItem
import com.handhot.app.data.local.entity.FeedSource
import com.handhot.app.data.remote.fetcher.FetcherFactory
import com.handhot.app.data.remote.model.FetchResult
import com.handhot.app.utils.HashUtils
import com.handhot.app.utils.UserAgentPool
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FeedRepository @Inject constructor(
    private val sourceDao: FeedSourceDao,
    private val itemDao: FeedItemDao,
    private val fetcherFactory: FetcherFactory
) {
    // --- Source operations ---

    fun observeAllSources(): Flow<List<FeedSource>> = sourceDao.observeAll()
    fun observeEnabledSources(): Flow<List<FeedSource>> = sourceDao.observeEnabled()
    suspend fun getEnabledSources(): List<FeedSource> = sourceDao.getEnabled()
    suspend fun getSourceById(id: Long): FeedSource? = sourceDao.getById(id)
    suspend fun getSourceByUrl(url: String): FeedSource? = sourceDao.getByUrl(url)
    suspend fun insertSource(source: FeedSource): Long = sourceDao.insert(source)
    suspend fun updateSource(source: FeedSource) = sourceDao.update(source)
    suspend fun deleteSource(source: FeedSource) {
        itemDao.deleteBySource(source.id)
        sourceDao.delete(source)
    }
    suspend fun deleteSourceById(id: Long) = sourceDao.deleteById(id)
    suspend fun setSourceEnabled(id: Long, enabled: Boolean) = sourceDao.setEnabled(id, enabled)
    suspend fun getEnabledCount(): Int = sourceDao.getEnabledCount()

    // --- Item operations ---

    fun observeUnreadAndStarred(): Flow<List<FeedItem>> = itemDao.observeUnreadAndStarred()
    fun observeBySource(sourceId: Long): Flow<List<FeedItem>> = itemDao.observeBySource(sourceId)
    suspend fun markRead(id: Long) = itemDao.markRead(id)
    suspend fun markAllReadBySource(sourceId: Long): Int = itemDao.markAllReadBySource(sourceId)
    suspend fun toggleStar(id: Long) = itemDao.toggleStar(id)
    suspend fun setStarred(id: Long, starred: Boolean) = itemDao.setStarred(id, starred)
    suspend fun getUnreadCountBySource(sourceId: Long): Int = itemDao.getUnreadCountBySource(sourceId)
    suspend fun getVisibleCount(): Int = itemDao.getVisibleCount()

    // --- Fetch operations ---

    /**
     * Refresh a single source. Returns the fetch result.
     */
    suspend fun refreshSource(source: FeedSource): FetchResult {
        val fetcher = fetcherFactory.getFetcher(source)
        val result = fetcher.fetch(source)

        // Update source fetch status
        val now = System.currentTimeMillis()
        sourceDao.updateFetchStatus(
            id = source.id,
            time = if (result.success) now else source.lastFetchTime,
            failCount = if (result.success) 0 else source.failCount + 1,
            error = result.error
        )

        // Process items with dedup
        if (result.success && result.items.isNotEmpty()) {
            processFetchedItems(source.id, result)
        }

        return result
    }

    /**
     * Refresh all enabled sources concurrently with adaptive concurrency.
     */
    suspend fun refreshAllSources(concurrency: Int = 5): Map<Long, FetchResult> = coroutineScope {
        val sources = sourceDao.getEnabled()
        if (sources.isEmpty()) return@coroutineScope emptyMap()

        val domainCooldown = mutableMapOf<String, Long>()
        val now = System.currentTimeMillis()

        // Filter sources by domain-level cooldown
        val eligibleSources = sources.filter { source ->
            val domain = UserAgentPool.extractDomain(source.url)
            val lastDomainFetch = domainCooldown[domain] ?: 0
            (now - lastDomainFetch >= 10_000).also { passes ->
                if (passes) domainCooldown[domain] = now
            }
        }

        // Fetch in chunks to respect concurrency limit
        val results = mutableMapOf<Long, FetchResult>()
        eligibleSources.chunked(concurrency).forEach { chunk ->
            val chunkResults = chunk.map { source ->
                async { source.id to refreshSource(source) }
            }.awaitAll()
            results.putAll(chunkResults)
        }
        results
    }

    /**
     * Process fetched items: dedup via hash, insert new, update changed.
     */
    private suspend fun processFetchedItems(sourceId: Long, result: FetchResult) {
        val now = System.currentTimeMillis()
        for (item in result.items) {
            val hash = HashUtils.itemHash(item.title, item.link)
            val existing = itemDao.getByHash(hash)

            if (existing != null) {
                // Update if summary or image changed
                if (existing.summary != item.summary || existing.coverImageUrl != item.coverImageUrl) {
                    itemDao.updateByHash(
                        hash = hash,
                        summary = item.summary,
                        imageUrl = item.coverImageUrl,
                        pubTime = item.pubTime ?: existing.pubTime,
                        now = now
                    )
                }
            } else {
                // Insert new item
                val feedItem = FeedItem(
                    sourceId = sourceId,
                    title = item.title,
                    summary = item.summary,
                    coverImageUrl = item.coverImageUrl,
                    link = item.link,
                    hash = hash,
                    pubTime = item.pubTime ?: now,
                    isRead = false,
                    isStarred = false,
                    createdAt = now
                )
                itemDao.insertIgnore(feedItem)
            }
        }
    }

    // --- Cleanup ---

    /**
     * Delete old read items (non-starred) older than retentionDays.
     */
    suspend fun cleanOldData(retentionDays: Int): Int {
        val before = System.currentTimeMillis() - (retentionDays * 24 * 60 * 60 * 1000L)
        return itemDao.deleteOldReadItems(before)
    }
}
