package com.xlrr.roambendom.data.pixiv

import androidx.compose.ui.util.fastJoinToString
import com.xlrr.roambendom.utils.camelToSnack

enum class PixivSearchTarget {
    PartialMatchForTags,
    ExactMatchForTags,
    TitleAndCaption,
    Keyword;

    override fun toString(): String {
        return super.toString().camelToSnack()
    }
}