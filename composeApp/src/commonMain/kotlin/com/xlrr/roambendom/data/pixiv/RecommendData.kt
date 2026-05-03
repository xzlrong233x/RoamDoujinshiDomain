package com.xlrr.roambendom.data.pixiv

import com.xlrr.roambendom.data.SearchItemData
import kotlinx.serialization.Serializable

data class RecommendData(
    var details: Map<String, RecommendMethodData> = mapOf(),
    var illusts: List<SearchItemData> = listOf(),
    var novels: List<SearchItemData> = listOf(),
    var nextIds: List<String> = listOf()
)

@Serializable
data class RecommendMethodData(
    var methods: List<String>,
    var score: Double
)