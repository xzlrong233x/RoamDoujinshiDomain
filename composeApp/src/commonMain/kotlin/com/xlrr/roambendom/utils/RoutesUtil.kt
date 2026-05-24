package com.xlrr.roambendom.utils

import com.xlrr.roambendom.data.CSources
import com.xlrr.roambendom.data.SearchItemData
import com.xlrr.roambendom.data.SearchResult
import com.xlrr.roambendom.model.detail.PIXIVDetailModel
import com.xlrr.roambendom.model.detail.asDetail
import com.xlrr.roambendom.model.search.SearchParameterModel
import com.xlrr.roambendom.nav.Navigator
import com.xlrr.roambendom.nav.Routes
import com.xlrr.roambendom.network.PIXIVApiHelper
import kotlin.text.ifEmpty

fun Navigator.pushAuthorSearch(userId: String, userName: String) {
    push(Routes.Root.FixedSearch(
        SearchParameterModel(userId).config {
            searchFunction = { k, p, e ->
                if (p >= 0) {
                    PIXIVApiHelper.userIllusts(k, "", p)
                } else {
                    SearchResult(-1, listOf(), k, p)
                }
            }
        }, "${userName.ifEmpty { userId }}的作品"
    ))
}

fun Navigator.pushRecommend(detail: PIXIVDetailModel) {
    push(Routes.Root.FixedSearch(
        detail.recommendModel, detail.content?.title?.let {
            "${it}的推荐作品"
        } ?: "推荐"
    ))
}

fun Navigator.pushDetail(id: String, source: CSources) {
    if (id.toIntOrNull() == null) return
    push(Routes.Root.Detail(
        SearchItemData(id, source).asDetail()
    ))
}