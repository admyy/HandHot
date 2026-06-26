package com.handhot.app.domain.usecase

import com.handhot.app.data.repository.FeedRepository
import javax.inject.Inject

class MarkReadUseCase @Inject constructor(
    private val repository: FeedRepository
) {
    suspend fun markRead(id: Long) = repository.markRead(id)

    suspend fun markAllReadBySource(sourceId: Long): Int = repository.markAllReadBySource(sourceId)
}
