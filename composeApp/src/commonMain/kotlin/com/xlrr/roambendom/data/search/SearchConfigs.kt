package com.xlrr.roambendom.data.search

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.xlrr.roambendom.config.StateWithUI
import com.xlrr.roambendom.config.UIType
import com.xlrr.roambendom.data.SearchResult
import com.xlrr.roambendom.network.NHWebHelper
import com.xlrr.roambendom.network.PIXIVApiHelper

class SearchConfigs {
    val searchTarget = StateWithUI(
        mutableIntStateOf(0), UIType.SingleSegmentedButton(
        "搜索目标", listOf("NH","PIXIV")
    ))

    var searchFunction : suspend (key: String, page: Int) -> SearchResult = {key, page ->
        if (searchTarget.state.value == 0) NHWebHelper.search(key, page) else
            PIXIVApiHelper.search(key, page)
    }

    var clearList by mutableStateOf(false)
}