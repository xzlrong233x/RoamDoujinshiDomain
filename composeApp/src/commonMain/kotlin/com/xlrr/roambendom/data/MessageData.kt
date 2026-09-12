package com.xlrr.roambendom.data

import com.xlrr.roambendom.utils.StringOrResource
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

data class MessageData(
    val title: StringOrResource,
    val content: StringOrResource,
    val timeOut: Duration = 10.seconds,
    val type: MessageType = MessageType.RT,
    val click: ((close:() -> Unit) -> Unit)? = null
)

enum class MessageType {
    BOTTOM,
    TOP,
    RT,
    LT,
}