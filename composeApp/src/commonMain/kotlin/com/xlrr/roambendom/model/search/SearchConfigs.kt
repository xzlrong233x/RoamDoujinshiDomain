package com.xlrr.roambendom.model.search

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.xlrr.roambendom.config.StateWithUI
import com.xlrr.roambendom.config.TempChangeConfig
import com.xlrr.roambendom.config.UIType
import com.xlrr.roambendom.data.SearchResult
import com.xlrr.roambendom.data.pixiv.PixivSearchRestriction
import com.xlrr.roambendom.data.pixiv.lowerStr
import com.xlrr.roambendom.network.NHWebHelper
import com.xlrr.roambendom.network.PIXIVApiHelper

class SearchConfigs {
    val searchTarget = TempChangeConfig(
        0, UIType.SingleSegmentedButton(
        "搜索目标", listOf("NH","PIXIV")
    ))

    val pixivSearchRestriction = StateWithUI(
        mutableStateOf(PixivSearchRestriction.All), UIType.DropStringSelectUI(
            "搜索模式", PixivSearchRestriction.entries.toList()
        ) { it.lowerStr() })

    var searchFunction : suspend (key: String, page: Int, extra: HashMap<String, Any>) -> SearchResult = {key, page, extra ->
        if (searchTarget.realValue == 0) NHWebHelper.search(key, page) else
            PIXIVApiHelper.search(key, page, pixivSearchRestriction.value)
    }

    var clearList by mutableStateOf(false)

    fun applyChange() {
        searchTarget.applyChange()
    }
}