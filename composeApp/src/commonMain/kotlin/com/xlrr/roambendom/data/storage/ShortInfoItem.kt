package com.xlrr.roambendom.data.storage

import com.xlrr.roambendom.data.CLanguage
import com.xlrr.roambendom.data.CRestriction
import com.xlrr.roambendom.data.CSources
import com.xlrr.roambendom.data.SearchItemData
import kotlinx.serialization.Serializable

@Serializable
data class ShortInfoItem(
    var id: String,
    var source: CSources,
    var title: String = "",
    var pageCount: Int = -1,
    var thumb: String,
    var width: Int,
    var height: Int,
    var author: String,
    var time: Long,
    var flag: Int
) {
    fun toSearchItem(): SearchItemData {
        return SearchItemData(
            id = id,
            source = source,
            title = title,
            pageCount = pageCount,
            thumb = thumb,
            width = width,
            height = height,
            author = author,
            time = time,
            ai = flag % 10 > 0,
            isAnimation = (flag / 10) % 10 > 0,
            lang = CLanguage.entries[((flag / 100) % 10).coerceIn(0, 3)],
            restriction = CRestriction.entries[((flag / 1000) % 10).coerceIn(0, 2)]
        )
    }
}
