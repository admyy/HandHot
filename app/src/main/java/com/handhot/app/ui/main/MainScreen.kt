package com.handhot.app.ui.main

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.handhot.app.data.local.entity.FeedItem
import com.handhot.app.data.local.entity.FeedSource
import com.handhot.app.utils.NetworkUtils
import com.handhot.app.utils.UserAgentPool

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    viewModel: MainViewModel,
    onAddSource: () -> Unit,
    onEditSource: (Long) -> Unit,
    onOpenDetail: (Long) -> Unit,
    onSettings: () -> Unit,
    onAbout: () -> Unit,
    onRefreshSource: (FeedSource) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("掌心热榜") },
                actions = {
                    IconButton(onClick = onSettings) {
                        Icon(Icons.Default.Settings, contentDescription = "设置")
                    }
                    IconButton(onClick = onAddSource) {
                        Icon(Icons.Default.Add, contentDescription = "添加数据源")
                    }
                }
            )
        }
    ) { padding ->
        PullToRefreshBox(
            isRefreshing = uiState.isRefreshing,
            onRefresh = { viewModel.refreshAll() },
            modifier = Modifier.padding(padding)
        ) {
            if (uiState.isEmpty && !uiState.isRefreshing) {
                EmptyStateView(uiState.emptyType, onAddSource)
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Group items by source
                    val grouped = uiState.items.groupBy { it.sourceId }
                    val sources = uiState.sources.associateBy { it.id }

                    grouped.forEach { (sourceId, items) ->
                        val source = sources[sourceId] ?: return@forEach
                        val fetchStatus = uiState.fetchStatus[sourceId]

                        item(key = "source_$sourceId") {
                            SourceHeader(
                                source = source,
                                unreadCount = items.size,
                                fetchStatus = fetchStatus,
                                onToggleExpand = { },
                                onEdit = { onEditSource(sourceId) },
                                onRefresh = { onRefreshSource(source) },
                                onMarkAllRead = { viewModel.markAllRead(sourceId) },
                                onToggleEnabled = { viewModel.clearFetchStatus(sourceId) }
                            )
                        }

                        items(
                            items = items,
                            key = { it.id }
                        ) { item ->
                            FeedItemCard(
                                item = item,
                                sourceName = source.name,
                                onClick = { onOpenDetail(item.id) },
                                onSwipeRead = { viewModel.markRead(item.id) },
                                onToggleStar = { viewModel.toggleStar(item.id) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SourceHeader(
    source: FeedSource,
    unreadCount: Int,
    fetchStatus: FetchStatus?,
    onToggleExpand: () -> Unit,
    onEdit: () -> Unit,
    onRefresh: () -> Unit,
    onMarkAllRead: () -> Unit,
    onToggleEnabled: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .combinedClickable(
                    onClick = onToggleExpand,
                    onLongClick = { showMenu = true }
                )
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = source.name,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    if (!source.enabled) {
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "已暂停",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
                Spacer(Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "$unreadCount 条未读",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    // Status icon
                    fetchStatus?.let { status ->
                        Spacer(Modifier.width(8.dp))
                        when {
                            status.isLoginExpired -> {
                                Icon(
                                    Icons.Default.Lock,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp),
                                    tint = Color(0xFFFF5252)
                                )
                                Text(
                                    "登录过期",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFFFF5252)
                                )
                            }
                            status.isCaptcha || status.isBlocked -> {
                                Icon(
                                    Icons.Default.Warning,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp),
                                    tint = Color(0xFFFFA726)
                                )
                                Text(
                                    "需验证",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFFFFA726)
                                )
                            }
                            !status.success && status.error != null -> {
                                Icon(
                                    Icons.Default.Warning,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp),
                                    tint = MaterialTheme.colorScheme.error
                                )
                                Text(
                                    status.error.take(15),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.error,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }

            IconButton(onClick = onRefresh) {
                Icon(Icons.Default.Refresh, "刷新", modifier = Modifier.size(20.dp))
            }
            IconButton(onClick = { showMenu = true }) {
                Icon(Icons.Default.MoreVert, "更多", modifier = Modifier.size(20.dp))
            }
        }
    }

    DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
        DropdownMenuItem(
            text = { Text("编辑") },
            onClick = { showMenu = false; onEdit() },
            leadingIcon = { Icon(Icons.Default.Edit, null) }
        )
        DropdownMenuItem(
            text = { Text("全部已读") },
            onClick = { showMenu = false; onMarkAllRead() },
            leadingIcon = { Icon(Icons.Default.DoneAll, null) }
        )
        DropdownMenuItem(
            text = { Text(if (source.enabled) "暂停" else "启用") },
            onClick = { showMenu = false; onToggleEnabled() },
            leadingIcon = { Icon(if (source.enabled) Icons.Default.Pause else Icons.Default.PlayArrow, null) }
        )
    }
}

@Composable
fun FeedItemCard(
    item: FeedItem,
    sourceName: String,
    onClick: () -> Unit,
    onSwipeRead: () -> Unit,
    onToggleStar: () -> Unit
) {
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            when (value) {
                SwipeToDismissBoxValue.EndToStart -> onSwipeRead()
                SwipeToDismissBoxValue.StartToEnd -> onToggleStar()
                else -> {}
            }
            false // Don't dismiss automatically
        }
    )

    SwipeToDismissBox(
        state = dismissState,
        backgroundContent = {
            val direction = dismissState.dismissDirection
            val color by animateColorAsState(
                when (dismissState.targetValue) {
                    SwipeToDismissBoxValue.StartToEnd -> Color(0xFFFFD54F)
                    SwipeToDismissBoxValue.EndToStart -> Color(0xFF4CAF50)
                    else -> Color.Transparent
                }
            )
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .background(color, RoundedCornerShape(12.dp))
                    .padding(horizontal = 20.dp),
                horizontalArrangement = if (direction == SwipeToDismissBoxValue.StartToEnd)
                    Arrangement.Start else Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = when (dismissState.targetValue) {
                        SwipeToDismissBoxValue.StartToEnd -> Icons.Default.Star
                        else -> Icons.Default.Done
                    },
                    contentDescription = null,
                    tint = Color.White
                )
            }
        },
        enableDismissFromStartToEnd = true,
        enableDismissFromEndToStart = true
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick),
            shape = RoundedCornerShape(12.dp)
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.Top
            ) {
                // Cover image or favicon placeholder
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(item.coverImageUrl ?: UserAgentPool.extractDomain(item.link).let {
                            "https://$it/favicon.ico"
                        })
                        .crossfade(true)
                        .build(),
                    contentDescription = null,
                    modifier = Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentScale = ContentScale.Crop
                )

                Spacer(Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.title,
                        style = MaterialTheme.typography.bodyLarge,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        fontWeight = if (!item.isRead) FontWeight.Medium else FontWeight.Normal
                    )

                    if (!item.summary.isNullOrBlank()) {
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = item.summary,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Spacer(Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = sourceName,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(Modifier.width(8.dp))
                        if (item.isStarred) {
                            Icon(
                                Icons.Default.Star,
                                contentDescription = "星标",
                                modifier = Modifier.size(14.dp),
                                tint = Color(0xFFFFD54F)
                            )
                        }
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = formatRelativeTime(item.createdAt),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun EmptyStateView(type: EmptyType, onAddSource: () -> Unit) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = when (type) {
                    EmptyType.NO_SOURCES -> "点击「+」添加你的第一个数据源"
                    EmptyType.ALL_PAUSED -> "所有数据源已暂停，请开启后刷新"
                    EmptyType.NO_UNREAD -> "🎉 暂无未读内容，下拉刷新试试"
                },
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (type == EmptyType.NO_SOURCES || type == EmptyType.ALL_PAUSED) {
                Spacer(Modifier.height(16.dp))
                Button(onClick = onAddSource) {
                    Text("添加数据源")
                }
            }
        }
    }
}

fun formatRelativeTime(timestamp: Long): String {
    val now = System.currentTimeMillis()
    val diff = now - timestamp
    return when {
        diff < 60_000 -> "刚刚"
        diff < 3_600_000 -> "${diff / 60_000}分钟前"
        diff < 86_400_000 -> "${diff / 3_600_000}小时前"
        diff < 604_800_000 -> "${diff / 86_400_000}天前"
        else -> {
            val sdf = java.text.SimpleDateFormat("MM-dd", java.util.Locale.getDefault())
            sdf.format(java.util.Date(timestamp))
        }
    }
}
