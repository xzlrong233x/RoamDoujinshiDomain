package com.xlrr.roambendom.gif

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import coil3.Canvas
import coil3.Image
import java.util.concurrent.atomic.AtomicBoolean

class GifImage(
    mp: Map<org.jetbrains.skia.Image, Int>,
    loopCount: Int
) : Image, MultiImagePlayer<org.jetbrains.skia.Image>(mp, loopCount) {

    override val width: Int = if (map.isEmpty()) 0 else map.keys.sumOf { it.width } / map.size
    override val height: Int = if (map.isEmpty()) 0 else map.keys.sumOf { it.height } / map.size

    /**
     * 图像字节数：以前用"压缩后的 GIF 字节数"，远小于解码后帧的真实占用，
     * 会让任何按 [coil3.Image.size] 记账 / 淘汰的缓存严重低估动图。这里按解码帧的 RGBA 大小累计。
     */
    override val size: Long = map.keys.sumOf { it.width.toLong() * it.height * 4L }

    override val shareable: Boolean
        get() = false

    /** 帧是否已关闭；关闭后不再绘制，避免访问已释放的 native 图像 */
    private val closed = AtomicBoolean(false)
    private val bitmapLock = Any()

    private val bitmaps = HashMap<org.jetbrains.skia.Image, ImageBitmap>()

    fun isClosed(): Boolean = closed.get()

    fun bitmapFor(image: org.jetbrains.skia.Image): ImageBitmap = synchronized(bitmapLock) {
        bitmaps.getOrPut(image) { image.toComposeImageBitmap() }
    }

    override fun draw(canvas: Canvas) {
        if (closed.get()) return
        generalDraw {
            canvas.drawImage(it, 0f, 0f)
        }
        // 只在播放中自失效；播放结束（有限 loop）后不再永久重绘
        if (isRunning) invalidateTick++
    }

    override fun close() {
        if (!closed.compareAndSet(false, true)) return
        isRunning = false
        for (frame in map.keys) {
            try {
                frame.close()
            } catch (_: Exception) {
                // 单帧释放失败不应阻断其它帧
            }
        }
        synchronized(bitmapLock) {
            bitmaps.clear()
        }
    }
}
