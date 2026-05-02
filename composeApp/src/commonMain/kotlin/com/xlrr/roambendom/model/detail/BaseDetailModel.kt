package com.xlrr.roambendom.model.detail

import com.xlrr.roambendom.data.ArtworkInfo
import com.xlrr.roambendom.data.CSources
import com.xlrr.roambendom.data.SearchItemData
import com.xlrr.roambendom.model.RequestRefreshModel

open class BaseDetailModel(var searchItemData: SearchItemData) : RequestRefreshModel<ArtworkInfo?>({null}) {
    protected open suspend fun request() {

    }

    fun isSuccessful() : Boolean {
        return !loading && content != null && error == null
    }

    override suspend fun reload() {
        error = null
        _loading = true
        try {
            request()
        } catch (e: Exception) {
            error = e
        }
        _loading = false
    }

    override suspend fun refresh() {
        _refreshing = true
        reload()
        _refreshing = false
    }

    suspend fun reloadIfEmpty() {
        if (content == null || error != null) {
            reload()
        }
    }
}

fun SearchItemData.asDetail() : BaseDetailModel {
    return when (this.source) {
        CSources.NHENTAI -> NHDetailModel(this)
        CSources.PIXIV -> PIXIVDetailModel(this)
    }
}