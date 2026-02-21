package com.xlrr.roambendom.data.pixiv

import kotlinx.serialization.Serializable

@Serializable
data class ArtworkPageItem(
    var height: Int,
    var width: Int,
    var urls: ArtworkPageUrls
)
