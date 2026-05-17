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
                // 使用 App API /v1/user/illusts 直接获取用户作品列表
                // 翻页通过 next_url 中的 offset 参数
                //TODO：让userIllusts直接返回SearchResult
                if (p == 0) {
                    // 首次加载所有类型作品
                    val (items, nextUrl) = PIXIVApiHelper.userIllusts(k, "", 0)
                    e["next_url"] = nextUrl ?: ""
                    SearchResult(-1, items, k, 1)
                } else {
                    val nextUrl = e["next_url"] as? String
                    if (nextUrl.isNullOrEmpty()) {
                        SearchResult(-1, listOf(), k, p)
                    } else {
                        val nextOffset = parseUserIllustsNextUrl(nextUrl)
                        val (items, newNextUrl) = PIXIVApiHelper.userIllusts(
                            k, "", nextOffset
                        )
                        e["next_url"] = newNextUrl ?: ""
                        SearchResult(-1, items, k, p + 1)
                    }
                }
            }
        }, "${userName.ifEmpty { userId }}的作品"
    ))
}

//TODO：有必要吗，改成regex
private fun parseUserIllustsNextUrl(nextUrl: String): Int {
    val query = nextUrl.substringAfter("?", "")
    query.split("&").forEach { param ->
        val eq = param.indexOf("=")
        if (eq < 0) return@forEach
        if (param.substring(0, eq) == "offset") {
            return param.substring(eq + 1).toIntOrNull() ?: 0
        }
    }
    return 0
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