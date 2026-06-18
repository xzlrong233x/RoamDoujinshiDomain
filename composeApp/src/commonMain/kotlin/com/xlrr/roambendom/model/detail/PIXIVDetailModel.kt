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
        searchFunction = PIXIVApiHelper.nextUrlSearchFunction { key, offset, viewed ->
            PIXIVApiHelper.illustRelated(key, offset, viewed, listOf(key))
        }
    }
}