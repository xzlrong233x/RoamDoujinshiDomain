package com.xlrr.roambendom.manager

import androidx.compose.runtime.snapshots.SnapshotStateMap
import com.xlrr.roambendom.data.MessageData
import kotlin.time.Clock
import kotlin.time.Instant

object MessageManager {
    private val messages = SnapshotStateMap<MessageData, Instant>()
    private val ifShown = SnapshotStateMap<MessageData, Boolean>()

    fun addMessage(message: MessageData) {
        messages[message] = Clock.System.now()
        ifShown[message] = false
    }

    fun get(): Map<MessageData, Instant> {
        return messages.toMap()
    }

    fun delectMessage(message: MessageData) {
        messages.remove(message)
        ifShown.remove(message)
    }

    fun ifInit(message: MessageData) : Boolean {
        return ifShown[message] ?: false
    }

    fun tryInit(message: MessageData) : Boolean {
        if (!ifShown.containsKey(message)) return false
        if (!messages.containsKey(message)) return false
        ifShown[message] = true
        return true
    }

    fun deInitialize(message: MessageData) : Boolean {
        if (!ifShown.containsKey(message)) return false
        if (!messages.containsKey(message)) return false
        ifShown[message] = false
        return true
    }
}