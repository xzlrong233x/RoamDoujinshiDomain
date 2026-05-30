package com.xlrr.roambendom.model.search

import androidx.compose.foundation.gestures.ScrollableState
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateSet
import com.xlrr.roambendom.config.LocalPlatformForUI
import com.xlrr.roambendom.config.UIEnablePlatform
import com.xlrr.roambendom.data.SearchItemData
import com.xlrr.roambendom.model.RequestRefreshModel
import kotlin.coroutines.cancellation.CancellationException

class SearchParameterModel(
    var key: String,
    var configs: SearchConfigs = SearchConfigs()
) : RequestRefreshModel<SnapshotStateSet<SearchItemData>>({mutableStateSetOf()}) {
    fun config(fc: SearchConfigs.() -> Unit) : SearchParameterModel {
        fc(configs)
        return this
    }
    private var _complete by mutableStateOf(false)

    val completed
        get() = _complete
    var scrollState : ScrollableState? = null

    var page by mutableIntStateOf(0)
    val extra = HashMap<String, Any>()

    override suspend fun reload() {
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
        _complete = false
    }

    @Composable
    fun isFullLoading() : Boolean {
        return isFullLoading(LocalPlatformForUI.current)
    }

    fun isFullLoading(ty: UIEnablePlatform) : Boolean {
        return ((if (ty == UIEnablePlatform.DESKTOP) _refreshing else false) || content.isEmpty()) && loading
    }

    override suspend fun refresh() { //纯他妈叠石山
        page = 0
        reset()
        _refreshing = true
        request(true)
        _refreshing = false
    }

    suspend fun request(clearAfterGet: Boolean = false) {
        _loading = true
        try {
            if (configs.clearList) content.clear()
            val result = configs.searchFunction(key, page, extra)
            if (clearAfterGet) {
                content.clear()
            }
            if (result.items.isNotEmpty()) {
                content.addAll(result.items)
                page = result.page
            } else _complete = true
        } catch (e: Exception) {
            if (e !is CancellationException)
                error = e
        }
        _loading = false
    }
}