package com.xlrr.roambendom.data.pixiv

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class UgoiraFrameItem(
    var file: String,
    var delay: Int
)

@Serializable
data class UgoiraMetadata(
    var frames: List<UgoiraFrameItem>,
    @SerialName("mime_type")
    var mimeType: String,
    var originalSrc: String,
    var src: String,
    var width: Int = 0,
    var height: Int = 0
)
