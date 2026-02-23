package com.xlrr.roambendom.utils

import androidx.compose.foundation.lazy.LazyListItemInfo
import androidx.compose.foundation.lazy.LazyListState
import kotlin.math.max

fun LazyListState.visibleItems(itemVisiblePercentThreshold: Float) =
    layoutInfo
        .visibleItemsInfo
        .filter {
            visibilityPercent(it) >= itemVisiblePercentThreshold
        }

fun LazyListState.visibilityPercent(info: LazyListItemInfo): Float {
    val cutTop = max(0, layoutInfo.viewportStartOffset - info.offset)
    val cutBottom = max(0, info.offset + info.size - layoutInfo.viewportEndOffset)

    return max(0f, 100f - (cutTop + cutBottom) * 100f / info.size)
}