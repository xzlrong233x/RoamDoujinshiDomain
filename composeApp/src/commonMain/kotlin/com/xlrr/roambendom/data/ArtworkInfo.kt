package com.xlrr.roambendom.data

import com.xlrr.roambendom.data.pixiv.UgoiraMetadata

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
    var ugoiraMetadata: UgoiraMetadata? = null
)
