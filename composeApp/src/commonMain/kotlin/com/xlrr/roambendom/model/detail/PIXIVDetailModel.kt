package com.xlrr.roambendom.model.detail

import com.xlrr.roambendom.data.SearchItemData
import com.xlrr.roambendom.data.SearchResult
import com.xlrr.roambendom.data.pixiv.RecommendData
import com.xlrr.roambendom.model.search.SearchParameterModel
import com.xlrr.roambendom.network.PIXIVApiHelper
import com.xlrr.roambendom.utils.MthUtil

class PIXIVDetailModel(searchItemData: SearchItemData) : BaseDetailModel(searchItemData) {
    override suspend fun request() {
        content = PIXIVApiHelper.artwork(searchItemData.id)
    }

    override suspend fun reload() {
        super.reload()
        recommendModel.reset()
        recommendModel.clear()
    }

    val recommendModel: SearchParameterModel = SearchParameterModel(searchItemData.id).config {
        searchFunction = {k,p,e ->
            if (p == 1) e["next"] = Unit
            var next = e["next"] as? RecommendData
            var lis: List<SearchItemData> = listOf()
            if (next != null) {
                lis = PIXIVApiHelper.recommendArtworkInfo(MthUtil.calWindow(p - 1, 9, next.nextIds.size).let {
                    next.nextIds.subList(it.first, it.second)
                })
            } else {
                next = PIXIVApiHelper.recommendArtwork(k)
                lis = next.illusts
                e["next"] = next
            }
            SearchResult(
                next.illusts.size + next.nextIds.size,
                lis,
                k,
                p
            )
        }
    }
}