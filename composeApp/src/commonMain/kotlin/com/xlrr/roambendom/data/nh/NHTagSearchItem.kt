package com.xlrr.roambendom.data.nh

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class NHTagSearchItem(
    val id: Int,
    val type: String,
    val name: String,
    val slug: String,
    val url: String,
    val count: Int,
    val description: String?,
    @SerialName("is_community")
    val isCommunity: Boolean?,
    @SerialName("pending_describe_id")
    val pendingDescribeId: String?
)
