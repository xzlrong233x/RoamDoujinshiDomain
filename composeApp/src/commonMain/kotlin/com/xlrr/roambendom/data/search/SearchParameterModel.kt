package com.xlrr.roambendom.data.search

import androidx.compose.foundation.gestures.ScrollableState
import androidx.compose.runtime.*
import com.xlrr.roambendom.data.SearchItemData
import kotlin.coroutines.cancellation.CancellationException

class SearchParameterModel(
    var key: String,
    var configs: SearchConfigs = SearchConfigs()
) {
    fun config(fc: SearchConfigs.() -> Unit) : SearchParameterModel {
        fc(configs)
        return this
    }

    var loading by mutableStateOf(false)
    var end by mutableStateOf(false)
    var error: Throwable? by mutableStateOf(null)

    var scrollState : ScrollableState? = null

    val content = mutableStateSetOf<SearchItemData>()

    var page by mutableIntStateOf(0)

    suspend fun reload() {
        error = null
        clear()
        request()
    }

    fun clear() {
        content.clear()
        page = 0
    }

    suspend fun request() {
        page++
        loading = true
        try {
            if (configs.clearList) content.clear()
            val result = configs.searchFunction(key, page)
            if (result.items.isNotEmpty()) {
                content.addAll(result.items)
            } else end = true
        } catch (e: Exception) {
            if (e !is CancellationException)
                error = e
        }
        loading = false
    }
}