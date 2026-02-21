package com.xlrr.roambendom.data.pixiv

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ArtworkPageUrls(
    var original: String,
    var regular: String,
    var small: String,
    @SerialName("thumb_mini")
    var thumbMini: String
)