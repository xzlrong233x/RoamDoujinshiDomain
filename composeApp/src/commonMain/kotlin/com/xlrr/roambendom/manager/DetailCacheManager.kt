package com.xlrr.roambendom.manager

import androidx.compose.runtime.snapshots.SnapshotStateList
import com.xlrr.roambendom.data.CSources
import com.xlrr.roambendom.data.storage.ShortInfoItem
import com.xlrr.roambendom.network.NHWebHelper
import com.xlrr.roambendom.network.PIXIVApiHelper
import kotlinx.io.Sink
import kotlinx.io.buffered
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import kotlinx.io.readString
import kotlinx.io.writeString
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json

object DetailCacheManager : LoadFileManager("detail_cache.json") {
    val list = SnapshotStateList<ShortInfoItem>()

    override fun load(p: Path) {
        SystemFileSystem.source(p).buffered().readString().let {
            list.clear()
            try {
                list.addAll(
                    Json.decodeFromString(
                        ListSerializer(ShortInfoItem.serializer()), it
                    )
                )
            } catch (e: Exception) {
                println("detail_cache load error: $e")
            }
        }
    }

    fun get(id: String, source: CSources): ShortInfoItem? {
        return list.runCatching { first { it.id == id && source == it.source } }.getOrNull()
    }

    suspend fun mustGet(id: String, source: CSources): ShortInfoItem {
        val i = get(id, source)
        if (i != null) return i
        val n = when (source) {
            CSources.PIXIV -> PIXIVApiHelper.artwork(id).toShortInfo(id)
            CSources.NHENTAI -> NHWebHelper.artwork(id).toShortInfo(id)
        }
        add(n, true)
        return n
    }

    fun add(shortInfoItem: ShortInfoItem, replace: Boolean = false): Boolean {
        val i = get(shortInfoItem.id, shortInfoItem.source)
        return if (i != null) {
            if (replace || i.pageCount < 0) {
                list.remove(i)
                list.add(shortInfoItem)
                true
            }
            else false
        } else {
            list.add(shortInfoItem)
        }.also { save() }
    }

    fun remove(id: String, source: CSources): Boolean {
        return list.runCatching { removeIf { it.id == id && it.source == source } }.isSuccess.also { save() }
    }

    override fun writeTo(buffer: Sink) {
        buffer.writeString(Json.encodeToString(list.toList()))
    }
}