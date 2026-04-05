package com.xlrr.roambendom.data.nh

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class NHSearchLike(
    val result: List<NHSearchLikeResultItem> = listOf(),
    @SerialName("num_pages")
    val numPages: Int,
    @SerialName("per_page")
    val perPage: Int,
    val total: Int,
)

@Serializable
data class NHSearchLikeResultItem(
    val id: Int,
    @SerialName("media_id")
    val mediaId: String,
    @SerialName("english_title")
    val englishTitle: String,
    @SerialName("japanese_title")
    val japaneseTitle: String?,
    val thumbnail: String,
    @SerialName("thumbnail_width")
    val thumbnailWidth: Int,
    @SerialName("thumbnail_height")
    val thumbnailHeight: Int,
    @SerialName("num_pages")
    val numPages: Int,
    @SerialName("tag_ids")
    val tagIds: List<Int>,
    val blacklisted: Boolean,
)
