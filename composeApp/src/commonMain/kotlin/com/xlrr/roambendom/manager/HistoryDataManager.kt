package com.xlrr.roambendom.manager

import androidx.compose.runtime.snapshots.SnapshotStateMap
import com.xlrr.roambendom.config.ConfigUtil
import com.xlrr.roambendom.data.CSources
import com.xlrr.roambendom.data.SearchItemData
import kotlinx.io.Sink
import kotlinx.io.buffered
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import kotlinx.io.readString
import kotlinx.io.writeString
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json
import kotlin.time.Clock

object HistoryDataManager : LoadFileManager("history.json") {
    @Serializable
    data class HistoryItem(
        var time: Long,
        var count: Int,
        var from: Int = 0
    )
    val map: SnapshotStateMap<String, HistoryItem> = SnapshotStateMap()

    override fun load(p: Path) {
        SystemFileSystem.source(p).buffered().readString().let {
            val r = Json.runCatching { decodeFromString(MapSerializer(String.serializer(), HistoryItem.serializer()), it) }
            if (r.isSuccess) {
                map.clear()
                map.putAll(r.getOrElse { mapOf() })
            } else println("history load error: ${r.exceptionOrNull()}")
        }
    }

    override fun writeTo(buffer: Sink) {
        buffer.writeString(Json.encodeToString(map.toMap()))
    }

    fun addItem(searchItemData: SearchItemData,toRead: Boolean = false) {
        if (ConfigUtil.disableHistoryRecord.value) {
            return
        }
        val it = map.getOrPut(searchItemData.uid()) {
            HistoryItem(
                Clock.System.now().toEpochMilliseconds(),
                0
            )
        }
        it.time = Clock.System.now().toEpochMilliseconds()
        if (toRead) {
            it.count++
        }
        DetailCacheManager.add(searchItemData.toShortInfo())
        save()
    }

    fun changeItemPage(uid: String, now: Int) {
        if (ConfigUtil.disableHistoryRecord.value) {
            return
        }
        val it = map[uid] ?: return
        it.from = now
        save()
    }

    fun remove(uid: String): Boolean {
        val item = map.remove(uid)
        save()
        return item != null
    }

    suspend fun getCacheByUid(uid: String): SearchItemData {
        return DetailCacheManager.mustGet(
            uid.drop(1),
            if (uid.first() == 'n') CSources.NHENTAI else CSources.PIXIV
        ).toSearchItem()
    }

    suspend fun getSearchItem(uid: String): SearchItemData? {
        val item = map[uid]
        return if (item != null) {
            getCacheByUid(uid).copy(time = item.time)
        } else null
    }

    suspend fun getList(): List<SearchItemData> {
        return map.keys.map {
            getCacheByUid(it).copy(time = map[it]?.time ?: 0)
        }.sortedWith { a,b -> (b.time - a.time).toInt() }
    }

    suspend fun search(key: String): List<SearchItemData> {
        return getList().filter {
            it.title.contains(key)
        }
    }

    fun removeAll() {
        map.clear()
        save()
    }
}