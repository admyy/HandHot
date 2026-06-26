package com.handhot.app.ui.source

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddSourceScreen(
    sourceId: Long? = null,
    onBack: () -> Unit,
    onSaved: () -> Unit,
    viewModel: SourceViewModel = hiltViewModel()
) {
    LaunchedEffect(sourceId) {
        if (sourceId != null) {
            // Load source for editing - handled by caller
        }
    }

    val formState by viewModel.formState.collectAsState()

    // Navigate back on save
    LaunchedEffect(formState.saved) {
        if (formState.saved) onSaved()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (formState.isEditing) "编辑数据源" else "添加数据源") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回")
                    }
                },
                actions = {
                    TextButton(
                        onClick = { viewModel.save() },
                        enabled = !formState.testing
                    ) {
                        Text("保存")
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
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Name
            OutlinedTextField(
                value = formState.name,
                onValueChange = { viewModel.updateName(it) },
                label = { Text("源名称 *") },
                placeholder = { Text("如：知乎热榜") },
                isError = formState.nameError != null,
                supportingText = formState.nameError?.let { { Text(it) } },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            // URL
            OutlinedTextField(
                value = formState.url,
                onValueChange = { viewModel.updateUrl(it) },
                label = { Text("目标 URL *") },
                placeholder = { Text("https://www.zhihu.com/hot") },
                isError = formState.urlError != null,
                supportingText = formState.urlError?.let { { Text(it) } },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            // Selector: Title
            OutlinedTextField(
                value = formState.selectorTitle,
                onValueChange = { viewModel.updateSelectorTitle(it) },
                label = { Text("CSS选择器-标题 *") },
                placeholder = { Text("a.QuestionLink") },
                isError = formState.selectorTitleError != null,
                supportingText = formState.selectorTitleError?.let { { Text(it) } },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            // Selector: Link
            OutlinedTextField(
                value = formState.selectorLink,
                onValueChange = { viewModel.updateSelectorLink(it) },
                label = { Text("CSS选择器-链接 *") },
                placeholder = { Text("a.QuestionLink[href]") },
                isError = formState.selectorLinkError != null,
                supportingText = formState.selectorLinkError?.let { { Text(it) } },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            // Selector: Summary (optional)
            OutlinedTextField(
                value = formState.selectorSummary,
                onValueChange = { viewModel.updateSelectorSummary(it) },
                label = { Text("CSS选择器-摘要（可选）") },
                placeholder = { Text("div.ContentItem") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            // Selector: Cover Image (optional)
            OutlinedTextField(
                value = formState.selectorImage,
                onValueChange = { viewModel.updateSelectorImage(it) },
                label = { Text("CSS选择器-封面图（可选）") },
                placeholder = { Text("img.cover") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            // Selector: Time (optional)
            OutlinedTextField(
                value = formState.selectorTime,
                onValueChange = { viewModel.updateSelectorTime(it) },
                label = { Text("CSS选择器-时间（可选）") },
                placeholder = { Text("time.timestamp") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            // Need login toggle
            Row(modifier = Modifier.fillMaxWidth()) {
                Text("需要登录", modifier = Modifier.weight(1f))
                Switch(
                    checked = formState.needLogin,
                    onCheckedChange = { viewModel.updateNeedLogin(it) }
                )
            }

            // JavaScript rendering toggle (for SPA sites)
            Row(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("JavaScript 渲染")
                    Text(
                        "用于知乎、微博等动态加载的网站",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = formState.useWebView,
                    onCheckedChange = { viewModel.updateUseWebView(it) }
                )
            }

            // Test button
            Button(
                onClick = { viewModel.testSelectors() },
                enabled = !formState.testing && formState.url.isNotBlank()
                        && formState.selectorTitle.isNotBlank() && formState.selectorLink.isNotBlank(),
                modifier = Modifier.fillMaxWidth()
            ) {
                if (formState.testing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("测试中…")
                } else {
                    Text("测试选择器")
                }
            }

            // Test result
            formState.testResult?.let { result ->
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = if (result.startsWith("✅"))
                            MaterialTheme.colorScheme.primaryContainer
                        else if (result.startsWith("⚠️"))
                            MaterialTheme.colorScheme.secondaryContainer
                        else
                            MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Text(
                        text = result,
                        modifier = Modifier.padding(12.dp),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }
}
