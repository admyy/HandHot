package com.handhot.app.ui.main

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.handhot.app.data.local.entity.FeedItem
import com.handhot.app.data.local.entity.FeedSource
import com.handhot.app.data.remote.model.FetchResult
import com.handhot.app.data.repository.FeedRepository
import com.handhot.app.domain.usecase.MarkReadUseCase
import com.handhot.app.domain.usecase.RefreshFeedsUseCase
import com.handhot.app.domain.usecase.ToggleStarUseCase
import com.handhot.app.utils.NetworkUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class MainUiState(
    val sources: List<FeedSource> = emptyList(),
    val items: List<FeedItem> = emptyList(),
    val fetchStatus: Map<Long, FetchStatus> = emptyMap(),
    val isRefreshing: Boolean = false,
    val isEmpty: Boolean = true,
    val emptyType: EmptyType = EmptyType.NO_SOURCES,
    val networkAvailable: Boolean = true,
    val unreadCounts: Map<Long, Int> = emptyMap()
)

data class FetchStatus(
    val success: Boolean,
    val error: String? = null,
    val isBlocked: Boolean = false,
    val isLoginExpired: Boolean = false,
    val isCaptcha: Boolean = false
)

enum class EmptyType { NO_SOURCES, ALL_PAUSED, NO_UNREAD }

@HiltViewModel
class MainViewModel @Inject constructor(
    private val repository: FeedRepository,
    private val refreshFeedsUseCase: RefreshFeedsUseCase,
    private val markReadUseCase: MarkReadUseCase,
    private val toggleStarUseCase: ToggleStarUseCase,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _fetchStatus = MutableStateFlow<Map<Long, FetchStatus>>(emptyMap())
    private val _isRefreshing = MutableStateFlow(false)

    val uiState: StateFlow<MainUiState> = combine(
        repository.observeAllSources(),
        repository.observeUnreadAndStarred(),
        _fetchStatus,
        _isRefreshing,
        NetworkUtils.observeNetworkState(context)
    ) { sources, items, fetchStatus, isRefreshing, networkAvailable ->
        val enabledSources = sources.filter { it.enabled }
        val emptyType = when {
            sources.isEmpty() -> EmptyType.NO_SOURCES
            enabledSources.isEmpty() -> EmptyType.ALL_PAUSED
            items.isEmpty() -> EmptyType.NO_UNREAD
            else -> EmptyType.NO_UNREAD
        }
        val isEmpty = sources.isEmpty() || items.isEmpty()

        MainUiState(
            sources = sources,
            items = items,
            fetchStatus = fetchStatus,
            isRefreshing = isRefreshing,
            isEmpty = isEmpty,
            emptyType = emptyType,
            networkAvailable = networkAvailable
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), MainUiState())

    fun refreshAll() {
        if (_isRefreshing.value) return
        viewModelScope.launch {
            _isRefreshing.value = true
            val concurrency = NetworkUtils.getConcurrentFetchLimit(context)
            val results = refreshFeedsUseCase.refreshAll(concurrency)
            _fetchStatus.value = results.mapValues { (_, result) ->
                FetchStatus(
                    success = result.success,
                    error = result.error,
                    isBlocked = result.isBlocked,
                    isLoginExpired = result.isLoginExpired,
                    isCaptcha = result.isCaptcha
                )
            }
            _isRefreshing.value = false
        }
    }

    fun refreshSource(source: FeedSource) {
        viewModelScope.launch {
            val result = refreshFeedsUseCase.refreshSingle(source)
            _fetchStatus.update { current ->
                current + (source.id to FetchStatus(
                    success = result.success,
                    error = result.error,
                    isBlocked = result.isBlocked,
                    isLoginExpired = result.isLoginExpired,
                    isCaptcha = result.isCaptcha
                ))
            }
        }
    }

    fun markRead(itemId: Long) {
        viewModelScope.launch { markReadUseCase.markRead(itemId) }
    }

    fun markAllRead(sourceId: Long) {
        viewModelScope.launch { markReadUseCase.markAllReadBySource(sourceId) }
    }

    fun toggleStar(itemId: Long) {
        viewModelScope.launch { toggleStarUseCase(itemId) }
    }

    fun clearFetchStatus(sourceId: Long) {
        _fetchStatus.update { current -> current - sourceId }
    }
}
