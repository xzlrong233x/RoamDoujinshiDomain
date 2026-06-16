package com.xlrr.roambendom.model

import androidx.compose.runtime.snapshots.SnapshotStateSet
import com.xlrr.roambendom.data.CSources
import com.xlrr.roambendom.data.SearchItemData
import com.xlrr.roambendom.data.SearchResult
import com.xlrr.roambendom.manager.FavoriteDataManager
import com.xlrr.roambendom.manager.HistoryDataManager
import com.xlrr.roambendom.model.search.SearchConfigs
import com.xlrr.roambendom.model.search.SearchParameterModel
import com.xlrr.roambendom.utils.MthUtil

class FavoriteScreenModel(
    val path: String,
) : SearchParameterModel(path, SearchConfigs().also {
        sc -> sc.searchFunction = {k, p, e ->
        val l = FavoriteDataManager.getItemsByFolder(k)
        SearchResult(
            l.size,
            MthUtil.calWindow(p+1, 30, l.size)
                .let { l.subList(it.first, it.second).map {x->
                    HistoryDataManager.getCacheByUid(x.uid).copy(time = x.time)
                } },
            k,
            p+1
        )
    }
}) {
    val folderInfos = SnapshotStateSet<Pair<String, String>>()
    override suspend fun reload() {
        folderInfos.clear()
        super.reload()
        if (folderInfos.isNotEmpty()) {
            content.add(
                SearchItemData(
                    "theElementIsNotDisplayed",
                    CSources.PIXIV
                )
            )
        }
    }

    override suspend fun request(clearAfterGet: Boolean) {
        _loading = true
        folderInfos.addAll(FavoriteDataManager.getFoldersByPath(path))
        super.request(clearAfterGet)
    }
}