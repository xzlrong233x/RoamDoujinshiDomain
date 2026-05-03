package com.xlrr.roambendom.ugoira

import android.graphics.Bitmap
import coil3.Canvas
import coil3.Image
import com.xlrr.roambendom.gif.MultiImagePlayer

class MultiImagePackage(
    mp: Map<Bitmap, Int>,
    override val size: Long,
    loopCount: Int
) : Image, MultiImagePlayer<Bitmap>(mp, loopCount) {
    override val width: Int = map.keys.sumOf { it.width } / map.size
    override val height: Int = map.keys.sumOf { it.height } / map.size
    override val shareable: Boolean
        get() = false

    override fun draw(canvas: Canvas) {
        generalDraw {
            canvas.drawBitmap(it, 0f, 0f, null)
        }
        invalidateTick++
    }
}