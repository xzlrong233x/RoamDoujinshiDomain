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
import roambendom.composeapp.generated.resources.Res
import roambendom.composeapp.generated.resources.author_artworks
import roambendom.composeapp.generated.resources.recommend_with_author
import roambendom.composeapp.generated.resources.signal_recommend_label
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
        }
    ) { Res.string.author_artworks.orResource(userName.ifEmpty { userId }) }) //author_artworks
}

fun Navigator.pushRecommend(detail: PIXIVDetailModel) {
    push(Routes.Root.FixedSearch(
        detail.recommendModel
    ) {
        detail.content?.title?.let {
            Res.string.recommend_with_author.orResource(it) //recommend_with_author
        } ?: Res.string.signal_recommend_label.orResource() //signal_recommend_label
    })
}

fun Navigator.pushDetail(id: String, source: CSources) {
    if (id.toIntOrNull() == null) return
    push(Routes.Root.Detail(
        SearchItemData(id, source).asDetail()
    ))
}