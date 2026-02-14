package com.xlrr.roambendom.data.search

import androidx.compose.runtime.mutableIntStateOf
import com.xlrr.roambendom.config.StateWithUI
import com.xlrr.roambendom.config.UIType

class SearchConfigs {
    val searchTarget = StateWithUI(
        mutableIntStateOf(0), UIType.SingleSegmentedButton(
        "搜索目标", listOf("NH","PIXIV")
    ))


}