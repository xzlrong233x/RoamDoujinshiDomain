package com.xlrr.roambendom.data.pixiv

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class KeywordSuggestionItem(
    @SerialName("access_count")
    var accessCount: String,
    @SerialName("tag_name")
    var tagName: String,
    @SerialName("tag_translation")
    var tagTranslation: String = "",
    var type: String // 我不想做enum，这个值只有tag_translation, prefix, romaji
)
