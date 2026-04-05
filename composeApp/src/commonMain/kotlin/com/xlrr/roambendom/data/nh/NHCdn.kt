package com.xlrr.roambendom.data.nh

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class NHCdn(
    @SerialName("image_servers")
    val imageServers: List<String>,
    @SerialName("thumb_servers")
    val thumbServers: List<String>,
)
