package com.xlrr.roambendom.model.detail

import com.xlrr.roambendom.data.SearchItemData
import com.xlrr.roambendom.data.SearchResult
import com.xlrr.roambendom.model.search.SearchParameterModel
import com.xlrr.roambendom.network.PIXIVApiHelper
import okhttp3.HttpUrl.Companion.toHttpUrl

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
        searchFunction = { k, p, e ->
            val nextUrl = e["next_url"] as? String
            var view: List<String> = listOf()
            var off = p
            if (nextUrl != null) {
                if (nextUrl.isEmpty()) {
                    SearchResult(
                        -1, listOf(), k, p+1
                    )
                }
                val url = nextUrl.toHttpUrl()
                off = url.queryParameter("offset")?.toInt() ?: 0
                view = url.queryParameterNames.mapNotNull {
                    if (it.matches("viewed\\[\\d+]".toRegex())) {
                        url.queryParameter(it)
                    } else null
                }
            }
            val (its , next) = PIXIVApiHelper.illustRelated(
                k, off,
                view,
                listOf(k)
            )
            e["next_url"] = next ?: ""
            SearchResult(
                -1,
                its,
                k,
                off+1
            )
        }
    }
}