package com.xlrr.roambendom.gif

import coil3.Canvas
import coil3.Image

class GifImage(
    mp: Map<org.jetbrains.skia.Image, Int>,
    override val size: Long,
    loopCount: Int
) : Image, MultiImagePlayer<org.jetbrains.skia.Image>(mp, loopCount) {

    override val width: Int = map.keys.sumOf { it.width } / map.size
    override val height: Int = map.keys.sumOf { it.height } / map.size
    override val shareable: Boolean
        get() = false

    override fun draw(canvas: Canvas) {
        generalDraw {
            canvas.drawImage(it, 0f, 0f)
        }
        invalidateTick++
    }
}