package com.xlrr.roambendom.model.search

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.xlrr.roambendom.config.StateWithUI
import com.xlrr.roambendom.config.TempChangeConfig
import com.xlrr.roambendom.config.UIType
import com.xlrr.roambendom.data.SearchResult
import com.xlrr.roambendom.data.nh.NHSearchSortType
import com.xlrr.roambendom.data.pixiv.PixivSearchRestriction
import com.xlrr.roambendom.data.pixiv.lowerStr
import com.xlrr.roambendom.network.NHWebHelper
import com.xlrr.roambendom.network.PIXIVApiHelper

class SearchConfigs {
    val searchTarget = TempChangeConfig(
        0, UIType.SingleSegmentedButton(
        "搜索目标", listOf("NH","PIXIV")
    ))

    val nhSearchSort = StateWithUI(
        mutableStateOf(NHSearchSortType.DATE), UIType.DropStringSelectUI(
            "排序方式", NHSearchSortType.entries.toList()
        ) { it.toString() }
    )

    val pixivSearchRestriction = StateWithUI(
        mutableStateOf(PixivSearchRestriction.All), UIType.DropStringSelectUI(
            "搜索模式", PixivSearchRestriction.entries.toList()
        ) { it.lowerStr() })

    val pixivHideAI = StateWithUI(
        mutableStateOf(false), UIType.SwitchUI(
            "不显示ai内容"
        )
    )

    val dateRange = StateWithUI(
        mutableStateOf(Pair(0,0)), UIType.NoUI<Pair<Long, Long>>()
    )

    //因要适配p站的软件api里的offset，page会从0开始，但不会在调用这个函数前增加，需要注意。
    var searchFunction : suspend (key: String, page: Int, extra: HashMap<String, Any>) -> SearchResult = {key, page, extra ->
        if (searchTarget.realValue == 0) {
            NHWebHelper.search(key, page + 1, nhSearchSort.value)
        } else
            PIXIVApiHelper.searchIllust(
                key, page, aiType = if (pixivHideAI.value) 1 else 0,
                timeRange = dateRange.value.let { if (it.second == 0L) null else it }
            )
    }

    var clearList by mutableStateOf(false)

    fun applyChange() {
        searchTarget.applyChange()
    }
}