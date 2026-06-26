package com.handhot.app.ui.detail

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.view.ViewGroup
import android.webkit.*
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.handhot.app.data.local.entity.FeedItem
import com.handhot.app.data.repository.FeedRepository
import com.handhot.app.ui.main.MainViewModel
import com.handhot.app.ui.settings.SettingsViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WebViewScreen(
    itemId: Long,
    mainViewModel: MainViewModel,
    settingsViewModel: SettingsViewModel,
    onBack: () -> Unit
) {
    // In a real app, we'd get the item from repository
    // For now, we need to pass the item data via a different mechanism
    // This is a simplified version — in production, use SavedStateHandle or a shared ViewModel
    var webView by remember { mutableStateOf<WebView?>(null) }
    var canGoBack by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(true) }
    var loadError by remember { mutableStateOf(false) }
    var pageFinished by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val settings by settingsViewModel.settingsState.collectAsState()

    val autoReadDelayMs = when (settings.autoReadDelay) {
        "off" -> -1L
        "1" -> 1000L
        "2" -> 2000L
        "5" -> 5000L
        else -> 2000L
    }

    // Mark as read after delay
    LaunchedEffect(pageFinished) {
        if (pageFinished && autoReadDelayMs > 0) {
            delay(autoReadDelayMs)
            mainViewModel.markRead(itemId)
        }
    }

    // Handle system back
    BackHandler {
        if (canGoBack && webView?.canGoBack() == true) {
            webView?.goBack()
        } else {
            onBack()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("详情", maxLines = 1) },
                navigationIcon = {
                    IconButton(onClick = {
                        if (canGoBack && webView?.canGoBack() == true) {
                            webView?.goBack()
                        } else {
                            onBack()
                        }
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回")
                    }
                },
                actions = {
                    IconButton(onClick = {
                        // Share link
                        val shareIntent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(android.content.Intent.EXTRA_TEXT, webView?.url ?: "")
                        }
                        LocalContext.current.startActivity(
                            android.content.Intent.createChooser(shareIntent, "分享链接")
                        )
                    }) {
                        Icon(Icons.Default.Share, "分享")
                    }
                    IconButton(onClick = {
                        // Open in external browser
                        webView?.url?.let { url ->
                            val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(url))
                            LocalContext.current.startActivity(intent)
                        }
                    }) {
                        Icon(Icons.Default.OpenInBrowser, "在浏览器中打开")
                    }
                }
            )
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding)) {
            // WebView
            AndroidView(
                factory = { context ->
                    WebView(context).apply {
                        webView = this
                        layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )

                        @SuppressLint("SetJavaScriptEnabled")
                        this.settings.apply {
                            javaScriptEnabled = true
                            domStorageEnabled = true
                            useWideViewPort = true
                            loadWithOverviewMode = true
                            setSupportZoom(true)
                            builtInZoomControls = true
                            displayZoomControls = false
                            cacheMode = WebSettings.LOAD_DEFAULT
                            // Security hardening
                            allowFileAccess = false
                            allowContentAccess = false
                            allowFileAccessFromFileURLs = false
                            allowUniversalAccessFromFileURLs = false
                            mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
                            userAgentString = "Mozilla/5.0 (Linux; Android 14; HandHot) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36"
                        }

                        webViewClient = object : WebViewClient() {
                            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                                isLoading = true
                                loadError = false
                                pageFinished = false
                            }

                            override fun onPageFinished(view: WebView?, url: String?) {
                                isLoading = false
                                canGoBack = view?.canGoBack() ?: false
                                pageFinished = true
                            }

                            override fun onReceivedError(
                                view: WebView?,
                                request: WebResourceRequest?,
                                error: WebResourceError?
                            ) {
                                if (request?.isForMainFrame == true) {
                                    isLoading = false
                                    loadError = true
                                }
                            }

                            override fun shouldOverrideUrlLoading(
                                view: WebView?,
                                request: WebResourceRequest?
                            ): Boolean {
                                val url = request?.url?.toString() ?: return false
                                // Block external app schemes
                                if (url.startsWith("weixin://") || url.startsWith("alipay://") ||
                                    url.startsWith("tbopen://") || url.startsWith("intent://")) {
                                    // Show dialog
                                    return true
                                }
                                return false
                            }
                        }

                        webChromeClient = object : WebChromeClient() {
                            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                                // progress handling
                            }
                        }
                    }
                },
                modifier = Modifier.fillMaxSize()
            )

            // Loading indicator
            if (isLoading) {
                LinearProgressIndicator(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.TopCenter)
                )
            }

            // Error overlay
            if (loadError) {
                Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        "加载失败",
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Spacer(Modifier.height(8.dp))
                    Button(onClick = { webView?.reload() }) {
                        Text("重试")
                    }
                }
            }
        }
    }
}
