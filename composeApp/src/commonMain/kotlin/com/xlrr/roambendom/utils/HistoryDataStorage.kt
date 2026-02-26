package com.xlrr.roambendom.utils

import com.xlrr.roambendom.config.ConfigUtil
import com.xlrr.roambendom.data.CSources
import com.xlrr.roambendom.data.SearchItemData
import io.github.vinceglb.filekit.utils.div
import kotlinx.io.buffered
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import kotlinx.io.readString
import kotlinx.io.writeString
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlin.time.Clock

class HistoryDataStorage {
    @Serializable
    data class HistoryContent(
        var list: ArrayList<SearchItemData> = arrayListOf(),
        var read: HashMap<String, Int> = hashMapOf()
    )
    private var content = HistoryContent()
    private var _init = false
    private var dataPath = Path("")

    fun init(dp: String) {
        if (_init) return
        if (!SystemFileSystem.exists(Path(dp))) SystemFileSystem.createDirectories(Path(dp))
        val p = Path(dp).div("history.json")
        dataPath = p
        if (SystemFileSystem.exists(p)) {
            load()
        }
        _init = true
    }

    fun getItem(uid: String) : SearchItemData? {
        return if (content.list.any { itemData ->  itemData.uid() == uid }) {
            content.list.first {
                it.uid() == uid
            }
        }
        else null
    }

    fun remove(uid: String) {
        content.list.removeIf {
            it.uid() == uid
        }
        content.read.remove(uid)
        save()
    }

    fun removeAll() {
        content.list.clear()
        content.read.clear()
        save()
    }

    fun search(key: String): List<SearchItemData> {
        return content.list.filter {
            it.title.contains(key)
        }.reversed()
    }

    fun list(): List<SearchItemData> {
        return content.list.toList().reversed()
    }

    fun addItem(searchItemData: SearchItemData,toRead: Boolean = false) {
        if (ConfigUtil.disableHistoryRecord.state.value) {
            return
        }
        val item = searchItemData.copy()
        item.time = Clock.System.now().toEpochMilliseconds()
        content.list.removeIf { it == item }
        content.list.add(item)
        if (toRead) {
            content.read[item.uid()] = 1 + (content.read[item.uid()] ?: 0)
        }
        save()
    }

    fun save() {
        val buf = SystemFileSystem.sink(dataPath).buffered()
        buf.writeString(Json.encodeToString(content))
        buf.flush()
        buf.close()
    }

    private fun load() {
        try {
            content = Json.decodeFromString<HistoryContent>(
                SystemFileSystem.source(dataPath).buffered().readString()
            )
            content.list = ArrayList(content.list.sortedBy { it.time })
        } catch (e : Exception) {
            e.printStackTrace()
            println("History load error")
        }
    }
}