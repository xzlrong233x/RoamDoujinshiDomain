package com.xlrr.roambendom.data

data class SearchItemData(
    var id: String,
    var title: String,
    var pageCount: Int = -1,
    var lang: CLanguage,
    var thumb: String,
    var source: CSources,
    var restriction: CRestriction = CRestriction.R18
)