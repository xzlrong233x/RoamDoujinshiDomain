package com.xlrr.roambendom.data

data class ArtworkInfo(
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
    var time: Long = 0
)
