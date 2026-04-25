package com.xlrr.roambendom.progressive

import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.ui.text.TextMeasurer
import coil3.PlatformContext
import coil3.SingletonImageLoader
import coil3.memory.MemoryCache
import coil3.request.ImageRequest

object SharedPainterManager {
    val map = HashMap<String, ProgressivePainter>()
    private val keyCount = HashMap<String, Int>()

    fun add(request: ImageRequest, textMeasurer: TextMeasurer? = null): ProgressivePainter {
        if (!exist(request)) {
            val p = ProgressivePainter(textMeasurer)
            map[request.data.toString()] = p
            return p
        }
        return map[request.data.toString()]!!
    }

    fun get(request: ImageRequest): ProgressivePainter? {
        return map[request.data.toString()]
    }

    fun exist(request: ImageRequest): Boolean {
        return map.contains(request.data.toString())
    }

    fun checkDestroyed(platformContext: PlatformContext) {
        map.entries.removeIf {
            val nouse = !it.value.isUsing()
            if (nouse && it.value.isCompleted()) {
                keyCount[it.key] = keyCount.getOrPut(it.key) { 0 } + 1
            }
            val r = nouse && ((it.value.isClosed() && it.value.isDestroyed())
                            || SingletonImageLoader.get(platformContext).memoryCache
                                ?.get(MemoryCache.Key(it.key)) != null
                            || keyCount.getOrDefault(it.key, 0) > 100)
            if (r) keyCount.remove(it.key)
            r
        }
    }

    fun remove(request: ImageRequest): Boolean {
        return map.remove(request.data.toString()) != null
    }
}