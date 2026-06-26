package com.handhot.app.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onBack: () -> Unit
) {
    val settings by viewModel.settingsState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("设置") },
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
        ) {
            // Network section
            SettingsSectionHeader("网络")

            SettingsSwitch(
                title = "允许使用移动网络刷新",
                subtitle = "关闭后仅WiFi下可刷新",
                checked = settings.allowMobileData,
                onCheckedChange = { viewModel.setAllowMobileData(it) }
            )

            SettingsSwitch(
                title = "仅在WiFi下加载图片",
                subtitle = "移动网络下封面图显示灰色占位",
                checked = settings.wifiOnlyImages,
                onCheckedChange = { viewModel.setWifiOnlyImages(it) }
            )

            // Display section
            SettingsSectionHeader("显示")

            SettingsDropdown(
                title = "夜间模式",
                current = when (settings.darkMode) {
                    "dark" -> "深色"
                    "light" -> "浅色"
                    else -> "跟随系统"
                },
                options = listOf(
                    "system" to "跟随系统",
                    "light" to "浅色",
                    "dark" to "深色"
                ),
                onSelect = { viewModel.setDarkMode(it) }
            )

            SettingsDropdown(
                title = "字体大小",
                current = when (settings.fontSize) {
                    "small" -> "小"
                    "large" -> "大"
                    "xlarge" -> "特大"
                    else -> "中"
                },
                options = listOf(
                    "small" to "小",
                    "medium" to "中",
                    "large" to "大",
                    "xlarge" to "特大"
                ),
                onSelect = { viewModel.setFontSize(it) }
            )

            SettingsDropdown(
                title = "详情页自动标已读延迟",
                current = when (settings.autoReadDelay) {
                    "off" -> "关闭"
                    "1" -> "1秒"
                    "5" -> "5秒"
                    else -> "2秒"
                },
                options = listOf(
                    "off" to "关闭",
                    "1" to "1秒",
                    "2" to "2秒",
                    "5" to "5秒"
                ),
                onSelect = { viewModel.setAutoReadDelay(it) }
            )

            // Data section
            SettingsSectionHeader("数据")

            SettingsDropdown(
                title = "数据保留天数",
                current = when (settings.retentionDays) {
                    3 -> "3天"
                    15 -> "15天"
                    30 -> "30天"
                    else -> "7天"
                },
                options = listOf(
                    "3" to "3天",
                    "7" to "7天",
                    "15" to "15天",
                    "30" to "30天"
                ),
                onSelect = { viewModel.setRetentionDays(it.toInt()) }
            )

            SettingsDropdown(
                title = "刷新冷却时间",
                current = when (settings.coolDownSeconds) {
                    30 -> "30秒"
                    120 -> "120秒"
                    else -> "60秒"
                },
                options = listOf(
                    "30" to "30秒",
                    "60" to "60秒",
                    "120" to "120秒"
                ),
                onSelect = { viewModel.setCoolDownSeconds(it.toInt()) }
            )
        }
    }
}

@Composable
fun SettingsSectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
    )
    HorizontalDivider()
}

@Composable
fun SettingsSwitch(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
fun SettingsDropdown(
    title: String,
    current: String,
    options: List<Pair<String, String>>,
    onSelect: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { expanded = true }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        Text(current, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.width(8.dp))
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { (value, label) ->
                DropdownMenuItem(
                    text = { Text(label) },
                    onClick = { onSelect(value); expanded = false }
                )
            }
        }
    }
}
