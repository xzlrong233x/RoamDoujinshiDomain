package com.xlrr.roambendom.ugoira

import android.graphics.Bitmap
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import coil3.Canvas
import coil3.Image
import kotlin.time.Clock
import kotlin.time.Duration.Companion.milliseconds

class MultiImagePackage(
    val map: Map<Bitmap, Int>,
    override val size: Long,
    val loopCount: Int
) : Image {
    override val width: Int = map.keys.sumOf { it.width } / map.size
    override val height: Int = map.keys.sumOf { it.height } / map.size
    override val shareable: Boolean
        get() = false
    private var invalidateTick by mutableIntStateOf(0) // 太伟大了，COMPOSE

    private var startTimestamp = Clock.System.now()
    var isRunning = true
        private set
    private var loop = 0
    private val totalDuration = map.values.sumOf { it }.milliseconds

    override fun draw(canvas: Canvas) {
        var dr = Clock.System.now() - startTimestamp
        var lp = 0
        var newLp = false
        while (dr > totalDuration) {
            dr -= totalDuration
            lp++
        }
        if (lp != loop) {
            loop = lp
            newLp = true
        }
        if (isRunning) {
            val bit = map.firstNotNullOf {
                if (dr <= it.value.milliseconds) {
                    it.key
                } else {
                    dr -= it.value.milliseconds
                    null
                }
            }
            canvas.drawBitmap(bit, 0f, 0f, null)
            invalidateTick++
            if (newLp) {
                if (loopCount == -1 || (loopCount in 1..loop)) {
                    isRunning = false
                }
            }
        }
    }
}