@file:OptIn(kotlin.concurrent.atomics.ExperimentalAtomicApi::class)

package com.xlrr.roambendom.manager

import androidx.compose.runtime.mutableStateListOf
import com.xlrr.roambendom.data.MessageData
import kotlin.concurrent.atomics.AtomicLong
import kotlin.concurrent.atomics.incrementAndFetch
import kotlin.time.Clock

/**
 * 消息队列中的一条消息。
 *
 * [id] 只用于给 LazyColumn 提供稳定 key 以及精确移除，业务层不可见。
 * [createdAt] 是入队时刻（epoch millis），供 UI 按“墙钟已流逝时间”计算剩余展示时长，
 * 避免条目被 LazyColumn 回收 / 页面重建后重新计时。
 */
class MessageEntry(val id: Long, val data: MessageData, val createdAt: Long)

/**
 * 消息队列（id 驱动）。
 *
 * 设计约束：
 * - 内部是 [androidx.compose.runtime.snapshots.SnapshotStateList]，UI 只读取 [entries]，
 *   不再像旧实现那样每次调用都复制整张 Map；
 * - 新消息插到表头（**最新在前**，与旧 UI 的“新→旧”顺序一致），超过上限从尾部丢弃；
 * - 任何方法都不会在 composition 阶段被调用（移除由条目自身的 LaunchedEffect / 点击回调触发）。
 */
object MessageManager {
    /** 同时最多保留的消息条数，防止无人消费时无限增长。 */
    private const val MAX_MESSAGES = 8

    /** 单调自增 id；addMessage 可能在后台线程（下载完成）与 UI 线程同时被调用，故用原子量。 */
    private val nextId = AtomicLong(0L)

    /** 只读暴露（同一 SnapshotStateList 引用，读取即订阅快照变更）。 */
    val entries: List<MessageEntry>
        field = mutableStateListOf<MessageEntry>()

    /**
     * 新增一条消息。签名保持不变，调用点无需改动。
     *
     * 插到列表头部（index 0），保证 LazyColumn 中“最新在最上面”。
     */
    fun addMessage(message: MessageData) {
        val entry = MessageEntry(
            nextId.incrementAndFetch(),
            message,
            Clock.System.now().toEpochMilliseconds()
        )
        entries.add(0, entry)
        while (entries.size > MAX_MESSAGES) {
            entries.removeAt(entries.lastIndex)
        }
    }

    /** 按 id 移除（幂等）。 */
    fun remove(id: Long) {
        entries.removeAll { it.id == id }
    }
}
