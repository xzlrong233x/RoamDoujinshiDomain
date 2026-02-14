package com.xlrr.roambendom.data.search

import androidx.compose.runtime.*
import com.xlrr.roambendom.data.SearchItemData
import com.xlrr.roambendom.network.NHWebHelper

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
            val result = NHWebHelper.search(key, page)
            if (result.items.isNotEmpty()) {
                content.addAll(result.items)
            } else end = true
        } catch (e: Exception) {
            error = e
        }
        loading = false
    }
}