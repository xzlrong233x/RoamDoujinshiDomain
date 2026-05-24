package com.xlrr.roambendom.data.pixiv.search

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