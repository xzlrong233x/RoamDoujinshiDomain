package com.xlrr.roambendom.progressive

import androidx.compose.ui.text.TextMeasurer
import coil3.request.ImageRequest

object SharedPainterManager {
    val map = HashMap<String, ProgressivePainter>()

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

    fun checkDestroyed() {
        map.values.removeIf { it.isClosed() && !it.isUsing() }
    }

    fun remove(request: ImageRequest): Boolean {
        return map.remove(request.data.toString()) != null
    }
}