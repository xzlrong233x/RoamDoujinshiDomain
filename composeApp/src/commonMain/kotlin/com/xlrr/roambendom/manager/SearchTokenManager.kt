package com.xlrr.roambendom.manager

import androidx.compose.runtime.snapshots.SnapshotStateList
import kotlinx.io.Sink
import kotlinx.io.buffered
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import kotlinx.io.readString
import kotlinx.io.writeString
import kotlinx.serialization.json.Json

object SearchTokenManager : LoadFileManager("search_token.json") {
    val data = SnapshotStateList<String>()

    override fun load(p: Path) {
        SystemFileSystem.source(p).buffered().readString().let {
            val r = Json.runCatching {
                decodeFromString<List<String>>(it)
            }
            if (r.isSuccess) {
                data.clear()
                data.addAll(r.getOrElse { listOf() })
            } else println("search token loading error: ${r.exceptionOrNull()}")
        }
    }

    override fun writeTo(buffer: Sink) {
        buffer.writeString(Json.encodeToString(data.toList()))
    }

    fun add(k: String) {
        if (data.contains(k)) {
            data.remove(k)
        }
        data.add(k)
        save()
    }

    fun remove(k: String): Boolean {
        return data.remove(k)
    }

    fun requestHistory(key: String): List<String> {
        return data.filter { key in it }.reversed()
    }

    fun clear() {
        data.clear()
        save()
    }

}
