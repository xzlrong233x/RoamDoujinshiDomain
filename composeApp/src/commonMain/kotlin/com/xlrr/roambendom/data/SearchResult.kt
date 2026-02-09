package com.xlrr.roambendom.data

data class SearchResult(
    var total: Int,
    var items: List<SearchItemData>,
    var key: String,
    var page: Int
)
