package com.handhot.app.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(onBack: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("关于") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "掌心热榜 HandHot",
                style = MaterialTheme.typography.headlineSmall
            )

            Text(
                text = "版本 1.0.0",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            HorizontalDivider()

            Text(
                text = "免责声明",
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = "本应用所有数据均来自公开网络，仅用于个人学习，版权归原网站所有。请勿高频抓取或商业使用。",
                style = MaterialTheme.typography.bodyMedium
            )

            Text(
                text = "隐私说明",
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = "本应用不包含任何网络上报、统计 SDK 或遥测。所有数据（Cookie、已读状态、源配置）仅存储于本地，不会上传至任何服务器。应用仅在你手动触发刷新时访问目标网站。",
                style = MaterialTheme.typography.bodyMedium
            )

            HorizontalDivider()

            Text(
                text = "开源协议",
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = "MIT License — 完全自由使用、修改和分发",
                style = MaterialTheme.typography.bodyMedium
            )

            Text(
                text = "技术栈",
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = "Kotlin + Jetpack Compose + Material3\nRoom + OkHttp + Jsoup + Coil\nHilt + WorkManager + AndroidX Security",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
