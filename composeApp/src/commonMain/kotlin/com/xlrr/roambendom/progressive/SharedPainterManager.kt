package com.xlrr.roambendom.progressive

import androidx.compose.ui.text.TextMeasurer
import coil3.PlatformContext
import coil3.request.ImageRequest

/**
 * 生命周期：
 * - [prefetch] / [acquireDisplay] / [acquireDownload] 负责创建或复用 painter；
 * - 显示/下载引用计数归零、且不在任何预取窗口内时 `close()` 并移出注册表；
 * - 阅读页通过 [setWindow] / [clearWindow] 声明"预取窗口"，窗口外的空闲 painter 立即释放；
 * - 注册表字节总量超过 [MAX_TOTAL_BYTES] 时，按 [Entry.lastAccess] 升序释放空闲 painter。
 *
 * 线程约定：注册表内部由 [lock] 保护，调用方无需关心线程 —— 显示/窗口在主线程，
 * 下载路径在 `Dispatchers.Default`；HTTP 线程只调用 [ProgressivePainter.write]，不触碰注册表。
 */
object SharedPainterManager {
    /** 注册表内 painter 缓冲字节总量上限，超限时淘汰最久未访问的空闲条目 */
    private const val MAX_TOTAL_BYTES = 64L * 1024 * 1024

    data class Stats(
        val entries: Int,
        val totalBytes: Long,
        val displayed: Int,
        val windowed: Int,
        val streaming: Int,
    )

    private class Entry(
        val key: String,
        val painter: ProgressivePainter,
    ) {
        /** 是否位于某个 owner 的预取窗口内；窗口内的 painter 即使没有引用也不淘汰 */
        var windowed: Boolean = false
        var displayRefs: Int = 0
        var downloadRefs: Int = 0
        /** 单调递增的访问序号，用于字节预算淘汰排序 */
        var lastAccess: Long = 0L
    }

    /** 保护以下所有可变状态；锁顺序：本锁 → ProgressivePainter.lock */
    private val lock = Any()

    private val entries = LinkedHashMap<String, Entry>()

    /** owner（阅读页的 Preload 实例）→ 该 owner 当前声明的预取窗口 key 集合 */
    private val windowKeys = LinkedHashMap<Any, Set<String>>()
    private var accessTick = 0L

    /** 预取：保证 painter 存在并标记为窗口内，不增加显示引用 */
    fun prefetch(
        request: ImageRequest,
        context: PlatformContext,
        textMeasurer: TextMeasurer? = null,
        animated: Boolean = false,
    ): ProgressivePainter = synchronized(lock) {
        val entry = getOrCreate(request, context, textMeasurer, animated)
        entry.windowed = true
        entry.painter
    }

    /** 显示：复用或创建 painter，displayRefs++；必须与 [releaseDisplay] 成对 */
    fun acquireDisplay(
        request: ImageRequest,
        context: PlatformContext,
        textMeasurer: TextMeasurer? = null,
        animated: Boolean = false,
    ): ProgressivePainter {
        var becameDisplayed: ProgressivePainter? = null
        val painter = synchronized(lock) {
            val entry = getOrCreate(request, context, textMeasurer, animated)
            if (entry.displayRefs == 0) becameDisplayed = entry.painter
            entry.displayRefs++
            entry.painter
        }
        // 只在 0→1 时通知 painter；且必须放在注册表锁外：
        // setDisplayed(true) 可能触发动图解码入队 Coil，不能持有注册表锁。
        becameDisplayed?.setDisplayed(true)
        return painter
    }

    fun releaseDisplay(key: String) {
        var becameHidden: ProgressivePainter? = null
        synchronized(lock) {
            val entry = entries[key] ?: return
            if (entry.displayRefs > 0) {
                entry.displayRefs--
                if (entry.displayRefs == 0) becameHidden = entry.painter
            }
            touch(entry)
            closeIfIdle(key, entry)
        }
        // 1→0 时通知 painter；锁外调用，避免注册表锁 → painter 锁之外再嵌套 Coil 回调
        becameHidden?.setDisplayed(false)
    }

    /** 下载：复用同一 URL 的 painter，downloadRefs++；必须与 [releaseDownload] 成对 */
    fun acquireDownload(request: ImageRequest, context: PlatformContext): ProgressivePainter =
        synchronized(lock) {
            val entry = getOrCreate(request, context, textMeasurer = null, animated = false)
            entry.downloadRefs++
            entry.painter
        }

    /** 下载引用释放；幂等 */
    fun releaseDownload(key: String) {
        synchronized(lock) {
            val entry = entries[key] ?: return
            if (entry.downloadRefs > 0) entry.downloadRefs--
            touch(entry)
            closeIfIdle(key, entry)
        }
    }

    /** 阅读页调用：替换该 owner 的预取窗口；窗口外的空闲 painter 立即 close */
    fun setWindow(owner: Any, keys: Collection<String>) {
        synchronized(lock) {
            if (keys.isEmpty()) {
                windowKeys.remove(owner)
            } else {
                windowKeys[owner] = LinkedHashSet(keys)
            }
            applyWindow()
        }
    }

    /** 阅读页离开时调用：清空该 owner 的窗口并释放所有空闲 painter */
    fun clearWindow(owner: Any) {
        synchronized(lock) {
            windowKeys.remove(owner)
            applyWindow()
        }
    }

    fun findForUrl(url: String): ProgressivePainter? = synchronized(lock) {
        val entry = entries[url] ?: return@synchronized null
        touch(entry)
        entry.painter
    }

    fun find(request: ImageRequest): ProgressivePainter? = findForUrl(request.data.toString())

    /** 从注册表移除；没有显示/下载引用时一并 close */
    fun remove(request: ImageRequest): Boolean = synchronized(lock) {
        val entry = entries.remove(request.data.toString()) ?: return@synchronized false
        if (entry.displayRefs == 0 && entry.downloadRefs == 0) entry.painter.close()
        true
    }

    fun snapshot(): Map<String, ProgressivePainter> = synchronized(lock) {
        if (entries.isEmpty()) return@synchronized emptyMap()
        val copy = LinkedHashMap<String, ProgressivePainter>(entries.size)
        for ((key, entry) in entries) copy[key] = entry.painter
        copy
    }

    fun stats(): Stats = synchronized(lock) {
        var totalBytes = 0L
        var displayed = 0
        var windowed = 0
        var streaming = 0
        for (entry in entries.values) {
            totalBytes += entry.painter.fileSize()
            if (entry.displayRefs > 0) displayed++
            if (entry.windowed) windowed++
            if (entry.painter.isUsing()) streaming++
        }
        Stats(
            entries = entries.size,
            totalBytes = totalBytes,
            displayed = displayed,
            windowed = windowed,
            streaming = streaming,
        )
    }

    /** 调用方需持有 [lock] */
    private fun getOrCreate(
        request: ImageRequest,
        context: PlatformContext,
        textMeasurer: TextMeasurer?,
        animated: Boolean,
    ): Entry {
        val key = request.data.toString()
        val existing = entries[key]
        if (existing != null) {
            touch(existing)
            return existing
        }
        val entry = Entry(key, ProgressivePainter(textMeasurer, animated, context, key))
        touch(entry)
        entries[key] = entry
        return entry
    }

    /** 调用方需持有 [lock] */
    private fun touch(entry: Entry) {
        entry.lastAccess = ++accessTick
    }

    /** 空闲（不在窗口、无显示/下载引用）则释放并从注册表移除；调用方需持有 [lock] */
    private fun closeIfIdle(key: String, entry: Entry) {
        if (!entry.windowed && entry.displayRefs == 0 && entry.downloadRefs == 0) {
            entries.remove(key)
            entry.painter.close()
        }
    }

    /** 依据当前所有 owner 的窗口刷新 windowed 标记，然后淘汰空闲条目并做字节预算修剪；需持有 [lock] */
    private fun applyWindow() {
        for (entry in entries.values) entry.windowed = false
        for (keys in windowKeys.values) {
            for (key in keys) entries[key]?.windowed = true
        }
        evictIdle()
        trimToBudget()
    }

    /** 调用方需持有 [lock] */
    private fun evictIdle() {
        val iterator = entries.values.iterator()
        while (iterator.hasNext()) {
            val entry = iterator.next()
            if (!entry.windowed && entry.displayRefs == 0 && entry.downloadRefs == 0) {
                iterator.remove()
                entry.painter.close()
            }
        }
    }

    /** 字节预算：按 lastAccess 升序关闭空闲条目，直到总量回到上限内；调用方需持有 [lock] */
    private fun trimToBudget() {
        var total = entries.values.sumOf { it.painter.fileSize() }
        if (total <= MAX_TOTAL_BYTES) return
        val idle = entries.values
            .filter { it.displayRefs == 0 && it.downloadRefs == 0 }
            .sortedBy { it.lastAccess }
        for (entry in idle) {
            if (total <= MAX_TOTAL_BYTES) break
            total -= entry.painter.fileSize()
            entries.remove(entry.key)
            entry.painter.close()
        }
    }
}
