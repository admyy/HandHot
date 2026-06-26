package com.handhot.app.ui.source

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.handhot.app.data.local.entity.FeedSource
import com.handhot.app.data.remote.parser.HtmlParser
import com.handhot.app.data.repository.FeedRepository
import com.handhot.app.utils.UserAgentPool
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SourceFormState(
    val name: String = "",
    val url: String = "",
    val selectorTitle: String = "",
    val selectorSummary: String = "",
    val selectorImage: String = "",
    val selectorLink: String = "",
    val selectorTime: String = "",
    val needLogin: Boolean = false,
    val useWebView: Boolean = false,
    val isEditing: Boolean = false,
    val editingId: Long = 0,
    val nameError: String? = null,
    val urlError: String? = null,
    val selectorTitleError: String? = null,
    val selectorLinkError: String? = null,
    val testResult: String? = null,
    val testing: Boolean = false,
    val saved: Boolean = false
)

@HiltViewModel
class SourceViewModel @Inject constructor(
    private val repository: FeedRepository
) : ViewModel() {

    private val _formState = MutableStateFlow(SourceFormState())
    val formState: StateFlow<SourceFormState> = _formState.asStateFlow()

    fun updateName(name: String) { _formState.value = _formState.value.copy(name = name, nameError = null) }
    fun updateUrl(url: String) { _formState.value = _formState.value.copy(url = url, urlError = null) }
    fun updateSelectorTitle(s: String) { _formState.value = _formState.value.copy(selectorTitle = s, selectorTitleError = null) }
    fun updateSelectorSummary(s: String) { _formState.value = _formState.value.copy(selectorSummary = s) }
    fun updateSelectorImage(s: String) { _formState.value = _formState.value.copy(selectorImage = s) }
    fun updateSelectorLink(s: String) { _formState.value = _formState.value.copy(selectorLink = s, selectorLinkError = null) }
    fun updateSelectorTime(s: String) { _formState.value = _formState.value.copy(selectorTime = s) }
    fun updateNeedLogin(v: Boolean) { _formState.value = _formState.value.copy(needLogin = v) }
    fun updateUseWebView(v: Boolean) { _formState.value = _formState.value.copy(useWebView = v) }

    fun loadSource(source: FeedSource) {
        _formState.value = SourceFormState(
            name = source.name,
            url = source.url,
            selectorTitle = source.selectorTitle,
            selectorSummary = source.selectorSummary ?: "",
            selectorImage = source.selectorImage ?: "",
            selectorLink = source.selectorLink,
            selectorTime = source.selectorTime ?: "",
            needLogin = source.needLogin,
            useWebView = source.useWebView,
            isEditing = true,
            editingId = source.id
        )
    }

    fun reset() {
        _formState.value = SourceFormState()
    }

    /**
     * Test the CSS selectors against the target URL.
     */
    fun testSelectors() {
        val state = _formState.value
        if (state.url.isBlank() || state.selectorTitle.isBlank() || state.selectorLink.isBlank()) return

        viewModelScope.launch {
            _formState.value = _formState.value.copy(testing = true, testResult = null)
            try {
                val client = okhttp3.OkHttpClient.Builder()
                    .connectTimeout(10, java.util.concurrent.TimeUnit.SECONDS)
                    .readTimeout(10, java.util.concurrent.TimeUnit.SECONDS)
                    .build()
                val request = okhttp3.Request.Builder()
                    .url(state.url)
                    .header("User-Agent", UserAgentPool.random())
                    .build()
                val response = client.newCall(request).execute()
                val body = response.body?.string() ?: ""

                val items = HtmlParser.parseItems(
                    html = body,
                    baseUrl = state.url,
                    selectorTitle = state.selectorTitle,
                    selectorLink = state.selectorLink,
                    selectorSummary = state.selectorSummary.ifBlank { null },
                    selectorImage = state.selectorImage.ifBlank { null },
                    selectorTime = state.selectorTime.ifBlank { null }
                )

                _formState.value = _formState.value.copy(
                    testing = false,
                    testResult = if (items.isNotEmpty()) {
                        "✅ 测试成功：提取到 ${items.size} 条（预览：${items.first().title}）"
                    } else {
                        "⚠️ 未提取到内容，请检查选择器"
                    }
                )
            } catch (e: Exception) {
                _formState.value = _formState.value.copy(
                    testing = false,
                    testResult = "❌ 请求失败：${e.message}"
                )
            }
        }
    }

    /**
     * Validate and save the source.
     */
    fun save(): Boolean {
        val state = _formState.value
        var valid = true

        val urlRegex = Regex("^https?://.*")
        if (state.name.isBlank()) {
            _formState.value = _formState.value.copy(nameError = "请输入名称")
            valid = false
        }
        if (!state.url.matches(urlRegex)) {
            _formState.value = _formState.value.copy(urlError = "请输入有效URL（http:// 或 https://）")
            valid = false
        }
        if (state.selectorTitle.isBlank()) {
            _formState.value = _formState.value.copy(selectorTitleError = "请输入标题选择器")
            valid = false
        }
        if (state.selectorLink.isBlank()) {
            _formState.value = _formState.value.copy(selectorLinkError = "请输入链接选择器")
            valid = false
        }
        if (!valid) return false

        viewModelScope.launch {
            val source = FeedSource(
                id = if (state.isEditing) state.editingId else 0,
                name = state.name,
                url = state.url,
                selectorTitle = state.selectorTitle,
                selectorSummary = state.selectorSummary.ifBlank { null },
                selectorImage = state.selectorImage.ifBlank { null },
                selectorLink = state.selectorLink,
                selectorTime = state.selectorTime.ifBlank { null },
                needLogin = state.needLogin,
                useWebView = state.useWebView
            )
            if (state.isEditing) {
                repository.updateSource(source)
            } else {
                repository.insertSource(source)
            }
            _formState.value = _formState.value.copy(saved = true)
        }
        return true
    }
}
