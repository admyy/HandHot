package com.handhot.app.domain.usecase

import com.handhot.app.data.repository.FeedRepository
import javax.inject.Inject

class CleanOldDataUseCase @Inject constructor(
    private val repository: FeedRepository
) {
    suspend operator fun invoke(retentionDays: Int): Int {
        return repository.cleanOldData(retentionDays)
    }
}
