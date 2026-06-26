package com.handhot.app.domain.usecase

import com.handhot.app.data.repository.FeedRepository
import javax.inject.Inject

class ToggleStarUseCase @Inject constructor(
    private val repository: FeedRepository
) {
    suspend operator fun invoke(id: Long) = repository.toggleStar(id)

    suspend fun setStarred(id: Long, starred: Boolean) = repository.setStarred(id, starred)
}
