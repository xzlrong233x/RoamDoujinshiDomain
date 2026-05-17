package com.xlrr.roambendom.model.detail

import com.xlrr.roambendom.data.SearchItemData
import com.xlrr.roambendom.data.SearchResult
import com.xlrr.roambendom.model.search.SearchParameterModel
import com.xlrr.roambendom.network.PIXIVApiHelper

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
            if (p == 0) {
                // 首次加载
                val (items, nextUrl) = PIXIVApiHelper.illustRelated(
                    k, 0,
                    seedIllustIds = listOf(k)
                )
                e["next_url"] = nextUrl ?: ""
                SearchResult(-1, items, k, 1)
            } else {
                val nextUrl = e["next_url"] as? String
                if (nextUrl.isNullOrEmpty()) {
                    SearchResult(-1, listOf(), k, p)
                } else {
                    // 解析 next_url 提取 offset 和 viewed[]
                    val (nextOffset, nextViewed) = parseRelatedNextUrl(nextUrl)
                    val (items, newNextUrl) = PIXIVApiHelper.illustRelated(
                        k, nextOffset, nextViewed
                    )
                    e["next_url"] = newNextUrl ?: ""
                    SearchResult(-1, items, k, p + 1)
                }
            }
        }
    }

    companion object {
        // 解析 illustRelated 返回的 next_url，提取 offset 和 viewed[]
        // TODO: Regex修改，deepseek写的什么东西
        internal fun parseRelatedNextUrl(nextUrl: String): Pair<Int, List<String>> {
            var offset = 0
            val viewed = mutableListOf<String>()
            val query = nextUrl.substringAfter("?", "")
            query.split("&").forEach { param ->
                val eq = param.indexOf("=")
                if (eq < 0) return@forEach
                val key = param.substring(0, eq)
                val value = param.substring(eq + 1)
                when {
                    key == "offset" -> offset = value.toIntOrNull() ?: 0
                    key.startsWith("viewed") -> viewed.add(value)
                }
            }
            return Pair(offset, viewed)
        }
    }
}