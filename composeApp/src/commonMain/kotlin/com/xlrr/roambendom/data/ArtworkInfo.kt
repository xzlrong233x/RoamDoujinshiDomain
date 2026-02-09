package com.xlrr.roambendom.data

data class ArtworkInfo(
    var title: String = "",
    var altitle: String = "",
    var tags: List<String> = listOf(),
    var groups: List<String> = listOf(),
    var authors: List<String> = listOf(),
    var page: Int = -1,
    var cover: String = "",
    var language: CLanguage = CLanguage.Unknown,
    var translated: Boolean = false,
    var comicUrls: List<String> = listOf(),
    var time: Long = 0
)
