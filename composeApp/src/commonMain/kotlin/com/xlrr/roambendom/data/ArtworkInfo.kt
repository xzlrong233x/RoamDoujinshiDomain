package com.xlrr.roambendom.data

import com.xlrr.roambendom.data.pixiv.UgoiraMetadata
import com.xlrr.roambendom.data.storage.ShortInfoItem
import com.xlrr.roambendom.network.UrlWithSize

data class ArtworkInfo(
    var source: CSources = CSources.NHENTAI,
    var restriction: CRestriction = CRestriction.R18,
    var title: String = "",
    var altitle: String = "",
    var tags: List<String> = listOf(),
    var groups: List<String> = listOf(),
    var authors: List<String> = listOf(),
    var description: String = "",
    var page: Int = -1,
    var cover: String = "",
    var language: CLanguage = CLanguage.Unknown,
    var translated: Boolean = false,
    var pageUrls: List<String> = listOf(),
    var thumbUrls: List<String> = listOf(),
    var likeCount: Int = 0,
    var time: Long = 0,
    var ai: Boolean = false,
    var isAnimation: Boolean = false,
    var ugoiraMetadata: UgoiraMetadata? = null
) {
    fun toShortInfo(id: String) : ShortInfoItem {
        val flag = CRestriction.entries.indexOf(restriction) * 1000 +
                CLanguage.entries.indexOf(language) * 100 +
                (if (isAnimation) 1 else 0) * 10 + (if (ai) 1 else 0)
        var uws = UrlWithSize.parse(thumbUrls.first())
        var tu = uws.url
        if (tu.isEmpty()) {
            tu = thumbUrls.first()
            uws = UrlWithSize.parse(pageUrls.first())
        }
        return ShortInfoItem(
            id,
            source,
            title,
            page,
            tu,
            uws.w,
            uws.h,
            if (authors.size == 1) authors.first() else "${authors.first()} (${authors.last()})",
            time,
            flag,
        )
    }
}
