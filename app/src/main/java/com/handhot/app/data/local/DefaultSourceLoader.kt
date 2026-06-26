package com.handhot.app.data.local

import android.content.Context
import com.handhot.app.data.local.entity.FeedSource
import com.handhot.app.data.repository.FeedRepository
import org.json.JSONObject

object DefaultSourceLoader {

    suspend fun loadIfEmpty(context: Context, repository: FeedRepository) {
        if (repository.getEnabledCount() > 0) return
        // Check total count to avoid re-import
        // Just insert — unique URL constraint prevents duplicates
        val json = context.assets.open("default_sources.json")
            .bufferedReader().use { it.readText() }
        val root = JSONObject(json)
        val sources = root.getJSONArray("sources")

        for (i in 0 until sources.length()) {
            val src = sources.getJSONObject(i)
            val selectors = src.getJSONObject("selectors")

            val source = FeedSource(
                name = src.getString("name"),
                url = src.getString("url"),
                selectorTitle = selectors.getString("title"),
                selectorLink = selectors.getString("link"),
                selectorSummary = selectors.optString("summary", null),
                selectorImage = selectors.optString("image", null),
                selectorTime = selectors.optString("time", null),
                needLogin = src.optBoolean("need_login", false),
                useWebView = src.optBoolean("use_webview", false),
                enabled = true
            )
            repository.insertSource(source)
        }
    }
}
