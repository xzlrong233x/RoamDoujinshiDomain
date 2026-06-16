package com.xlrr.roambendom.manager

import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.snapshots.SnapshotStateMap
import com.xlrr.roambendom.data.SearchItemData
import kotlinx.io.Sink
import kotlinx.io.buffered
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import kotlinx.io.readString
import kotlinx.io.writeString
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.encoding.CompositeDecoder.Companion.DECODE_DONE
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.encoding.decodeStructure
import kotlinx.serialization.encoding.encodeStructure
import kotlinx.serialization.json.Json
import kotlin.time.Clock

object FavoriteDataManager : LoadFileManager("favorite.json") {
    @Serializable
    data class FavoriteItem(
        var uid: String,
        var time: Long,
        var path: String = "/"
    )

    @Serializable
    data class FavoriteFolderInfo(
        var folders: HashSet<String> = hashSetOf(),
        var name: String
    )

    @Serializable(with = FavoriteContentSerializer::class)
    data class FavoriteContent(
        var favorites: SnapshotStateList<FavoriteItem> = SnapshotStateList<FavoriteItem>(),
        var folderInfo: SnapshotStateMap<String, FavoriteFolderInfo> = SnapshotStateMap<String, FavoriteFolderInfo>().also {
            it["/"] = FavoriteFolderInfo(
                hashSetOf(),
                "root"
            )
        }
    )

    class FavoriteContentSerializer : KSerializer<FavoriteContent> {
        private val listSerializer = ListSerializer(FavoriteItem.serializer())
        private val mapSerializer = MapSerializer(String.serializer(), FavoriteFolderInfo.serializer())
        override val descriptor: SerialDescriptor
            get() = buildClassSerialDescriptor(
                "favoriteContent"
            ) {
                element("favorites", listSerializer.descriptor)
                element("folder_info", mapSerializer.descriptor)
            }

        override fun deserialize(decoder: Decoder): FavoriteContent = decoder.decodeStructure(descriptor) {
            val c = FavoriteContent()
            loop@ while (true) {
                when (val index = decodeElementIndex(descriptor)) {
                    DECODE_DONE -> break@loop
                    0 -> {
                        c.favorites.addAll(
                            decodeSerializableElement(descriptor, index, listSerializer)
                        )
                    }
                    1 -> {
                        c.folderInfo.putAll(decodeSerializableElement(descriptor, index, mapSerializer))
                    }
                    else -> throw SerializationException("Unexpected index $index")
                }
            }
            return@decodeStructure c
        }

        override fun serialize(
            encoder: Encoder,
            value: FavoriteContent
        ) = encoder.encodeStructure(descriptor) {
            encodeSerializableElement(descriptor, 0, listSerializer, value.favorites.toList())
            encodeSerializableElement(descriptor, 1, mapSerializer, value.folderInfo.toMap())
        }
    }

    var content: FavoriteContent = FavoriteContent()
        private set

    override fun load(p: Path) {
        SystemFileSystem.source(p).buffered().readString().let {
            val r = Json.runCatching {
                decodeFromString<FavoriteContent>(it)
            }
            if (r.isSuccess) {
                content = r.getOrElse { FavoriteContent() }
                content.favorites.removeIf { xd -> xd.path !in content.folderInfo.keys }
            } else println("favorite content error: ${r.exceptionOrNull()}")
        }
    }

    override fun writeTo(buffer: Sink) {
        buffer.writeString(Json.encodeToString(content))
    }

    fun addItem(searchItemData: SearchItemData, path: String = "/"): Boolean {
        val rp = if (formatPath(path) in content.folderInfo.keys) formatPath(path) else "/"
        val o = content.favorites.find { it.uid == searchItemData.uid() }
        if (o != null) {
            if (o.path == path) return false
            o.path = path
        } else {
            content.favorites.add(
                FavoriteItem(
                    searchItemData.uid(),
                    Clock.System.now().toEpochMilliseconds(),
                    rp
                )
            )
        }
        save()
        return true
    }

    fun formatPath(path: String): String {
        val l = path.removeSurrounding("/").split("/").filter { it.isNotEmpty() }
        return formatPath(l)
    }

    private fun formatPath(list: List<String>): String {
        return if (list.isEmpty()) {
            "/"
        } else {
            list.joinToString("/", "/", "/")
        }
    }

    private fun splitPath(path: String): Pair<String, String> {
        val l = path.removeSurrounding("/").split("/").filter { it.isNotEmpty() }
        return if (l.isEmpty()) {
            Pair("/", "")
        } else {
            val n = l.dropLast(1)
            Pair(formatPath(n), l.last())
        }
    }

    fun parent(path: String): String {
        return splitPath(formatPath(path)).first
    }

    fun addFolder(folderPath: String,name: String = "", path: String = "/"): Boolean {
        if ("/" in folderPath) return false
        val rp = formatPath(path)
        if (!content.folderInfo.containsKey(rp)) return false
        val tp = formatPath(rp + folderPath)
        content.folderInfo[tp] = FavoriteFolderInfo(name = name.ifEmpty { folderPath })
        content.folderInfo[rp]?.folders?.add(folderPath)
        save()
        return true
    }

    fun loopAddFolder(path: String) {
        val n = splitPath(path)
        if (n.second.isEmpty()) return
        if (n.first !in content.folderInfo.keys) {
            loopAddFolder(n.first)
        }
        addFolder(n.second, n.second, n.first)
    }

    fun addItemWithFolder(searchItemData: SearchItemData, path: String = "/"): Boolean {
        if (path in content.folderInfo.keys) return addItem(searchItemData, path)
        else {
            loopAddFolder(path)
            return addItem(searchItemData, path)
        }
    }

    fun removeItem(uid: String): Boolean {
        val b = content.favorites.removeIf { it.uid == uid }
        save()
        return b
    }

    fun removeFolder(folder: String,path: String = "/", bothItem: Boolean = false): Boolean {
        val rp = formatPath(path)
        val p = formatPath(rp + folder)
        if (p !in content.folderInfo.keys || rp !in content.folderInfo.keys) return false
        content.folderInfo[rp]?.folders?.remove(folder) ?: return false
        val ct = content.folderInfo[p]!!
        if (ct.folders.isNotEmpty()) {
            ct.folders.forEach {
                removeFolder(it, p, bothItem)
            }
        }
        content.folderInfo.remove(p)
        if (bothItem)
            content.favorites.removeIf { it.path == p }
        else {
            content.favorites.forEach {
                if (it.path == p) {
                    it.path = path
                }
            }
        }
        save()
        return true
    }

    fun removeFolder(path: String = "/", bothItem: Boolean = false): Boolean {
        if (path == "/") return false
        val p = splitPath(path)
        return removeFolder(p.second, p.first, bothItem)
    }

    fun getItemsByFolder(path: String): List<FavoriteItem> {
        return content.favorites.filter { it.path == path }
    }

    fun getFoldersByPath(path: String): List<Pair<String, String>> {
        return content.folderInfo[path]?.let {
            it.folders.map {f ->
                val pf = formatPath(path + f)
                Pair(pf, content.folderInfo[pf]?.name ?: "Unknow")
            }
        } ?: listOf()
    }

    fun changeFolder(name: String, path: String = "/") {
        val rp = formatPath(path)
        if (!content.folderInfo.containsKey(rp)) return
        content.folderInfo[rp]?.name = name
    }

    fun moveItem(uid: String, to: String): Boolean {
        val rp = formatPath(to)
        if (!content.folderInfo.containsKey(rp)) return false
        val i = content.favorites.find { it.uid == uid }
        if (i == null) return false
        i.path = to
        save()
        return true
    }

    //TODO: GET
}