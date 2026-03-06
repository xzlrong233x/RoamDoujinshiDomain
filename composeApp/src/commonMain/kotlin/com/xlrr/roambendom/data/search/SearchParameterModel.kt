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
    var refreshing by mutableStateOf(false)
    var end by mutableStateOf(false)
    var error: Throwable? by mutableStateOf(null)

    var scrollState : ScrollableState? = null

    val content = mutableStateSetOf<SearchItemData>()

    var page by mutableIntStateOf(0)

    suspend fun reload() {
        reset()
        clear()
        request()
    }

    fun clear() {
        content.clear()
        page = 0
    }

    fun reset() {
        error = null
        end = false
    }

    suspend fun refresh() { //纯他妈叠石山
        page = 0
        reset()
        refreshing = true
        request(true)
        refreshing = false
    }

    suspend fun request(clearAfterGet: Boolean = false) {
        page++
        loading = true
        try {
            if (configs.clearList) content.clear()
            val result = configs.searchFunction(key, page)
            if (clearAfterGet) {
                content.clear()
            }
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