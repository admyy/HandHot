package com.handhot.app.domain.usecase

import com.handhot.app.data.local.entity.FeedSource
import com.handhot.app.data.remote.model.FetchResult
import com.handhot.app.data.repository.FeedRepository
import javax.inject.Inject

class RefreshFeedsUseCase @Inject constructor(
    private val repository: FeedRepository
) {
    suspend fun refreshAll(concurrency: Int): Map<Long, FetchResult> {
        return repository.refreshAllSources(concurrency)
    }

    suspend fun refreshSingle(source: FeedSource): FetchResult {
        return repository.refreshSource(source)
    }
}
