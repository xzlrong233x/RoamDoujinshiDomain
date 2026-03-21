package com.xlrr.roambendom.gif

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import coil3.Canvas
import coil3.Image
import com.shakster.gifkt.GifDecoder
import org.jetbrains.skiko.toImage
import kotlin.time.Clock

class GifImage(
    val gifDecoder: GifDecoder,
    override val size: Long,
): Image {
    override val shareable: Boolean
        get() = false
    override val width: Int
        get() = gifDecoder.width
    override val height: Int
        get() = gifDecoder.height
    private var invalidateTick by mutableIntStateOf(0) // 太伟大了，COMPOSE

    private var startTimestamp = Clock.System.now()
    private var isRunning = true
    private var loop = 0

    override fun draw(canvas: Canvas) {
        var dr = Clock.System.now() - startTimestamp
        var lp = 0
        var newLp = false
        while (dr > gifDecoder.duration) {
            dr -= gifDecoder.duration
            lp++
        }
        if (lp != loop) {
            loop = lp
            newLp = true
        }
        if (isRunning) {
            canvas.drawImage(gifDecoder[dr].toBufferedImage().toImage(),0f , 0f)
            invalidateTick++
            if (newLp) {
                if (gifDecoder.loopCount == -1 || (gifDecoder.loopCount in 1..loop)) {
                    isRunning = false
                }
            }
        }
    }

    fun restart() {
        isRunning = true
        startTimestamp = Clock.System.now()
        loop = 0
    }

    fun stop() {
        isRunning = false
    }
}