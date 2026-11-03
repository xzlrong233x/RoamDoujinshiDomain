package com.xlrr.roambendom.progressive

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.decodeToImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.fastRoundToInt
import coil3.PlatformContext
import coil3.SingletonImageLoader
import coil3.request.ImageRequest
import coil3.request.ImageResult
import com.fleeksoft.io.ByteArrayOutputStream
import com.xlrr.roambendom.drawSpecial
import com.xlrr.roambendom.gif.MultiImagePlayer
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlin.concurrent.Volatile

@OptIn(ExperimentalCoroutinesApi::class) // Deferred.getCompleted()：拿已完成解码结果（动图）
class ProgressivePainter(
    val textMeasurer: TextMeasurer? = null,
    val animated: Boolean = false,
    val platformContext: PlatformContext,
    /** 注册表键（完整 URL）；动图需要在非绘制阶段重新入队解码时用它构造请求 */
    val sourceKey: String? = null,
) : Painter() {
    private var _w: Float = 1f
    private var _h: Float = 1f
    override val intrinsicSize: Size
        get() = Size(_w,_h)
    private var _sizeSet = false

    /**
     * 保护跨线程访问的字段：HTTP 线程会 [write]，UI 线程会绘制/释放。
     * 锁顺序：SharedPainterManager.lock → ProgressivePainter.lock（painter 内部不回调注册表）。
     */
    private val lock = Any()

    private val _output = ByteArrayOutputStream()
    private var invalidateTick by mutableIntStateOf(0)
    private var _markToDestroy by mutableStateOf(false)
    private var _lastImg: Pair<ImageBitmap, Int>? = null
    private var _totalFileSize = 0
    private var userCount = 0
    private var _error = false

    /**
     * 是否有显示引用（[SharedPainterManager] 在 displayRefs 0→1 / 1→0 时同步）。
     * 动图只有"正在显示"时才允许整帧解码：预取但从未显示的动图不能提前把全部帧解到内存里。
     */
    private var displayed = false

    /** 解码入队进行中：防止多个触发点在 `.job` 赋值前重复入队（release/display/write 可能并发） */
    private var decodeStarting = false

    /** 已彻底释放（_output 已清空，后续 write 丢弃） */
    @Volatile
    private var _closed = false

    /** 缓冲超过 [maxBufferedBytes] 后不再增长；此时也视为"已封闭"，不再做进度重绘 */
    private var _bufferFull = false

    var animatedImage: MultiImagePlayer<*>? = null
        private set
    private var job: Deferred<ImageResult>? = null
        set(value) {
            field?.cancel()
            field = value
        }

    /** 下载结束（写满/出错/销毁）时给出当前字节 */
    val finished: Deferred<ByteArrayOutputStream>
        field = CompletableDeferred<ByteArrayOutputStream>()
    var onSizeKnown: ((Int, Int) -> Unit)? = null

    override fun DrawScope.onDraw() {
        // 无条件读一次，注册 draw 依赖：write()/setSize()（以及正在播放的动图）会在锁外自增它。
        // 本函数自身不再无条件自增，否则静态画面在流式下载期间会永久 60fps 重绘（最小化也不停）。
        @Suppress("UNUSED_VARIABLE")
        val tick = invalidateTick

        // HTTP 线程可能正在 write()，绘制前先在锁内取快照
        var written = 0
        var total = 0
        var currentJob: Deferred<ImageResult>? = null
        var currentPlayer: MultiImagePlayer<*>? = null
        var completedNow = false
        synchronized(lock) {
            written = _output.size()
            total = _totalFileSize
            currentJob = job
            currentPlayer = animatedImage
            completedNow = _totalFileSize > 0 && _output.size() == _totalFileSize
        }

        if (!animated) {
            drawProgressiveFrame()
        } else {
            if (currentPlayer == null && currentJob?.isCompleted != true && total > 0) {
                drawArc(
                    Color.DarkGray,
                    0f,
                    written * 360f / total,
                    false,
                    Offset(size.width / 2, size.height / 2),
                    Size(75f, 75f),
                    1f,
                    Stroke(2f),
                )
            } else if (currentJob?.isCompleted == true) {
                val player = currentPlayer ?: runCatching {
                    currentJob.getCompleted().image as? MultiImagePlayer<*>
                }.getOrNull()?.also { p ->
                    synchronized(lock) {
                        // 解码期间可能已被 destroy()/close()，不要复活
                        if (!_closed && animatedImage == null) animatedImage = p
                    }
                }
                player?.let {
                    // 首帧也要安排下一次绘制，否则解码完成后动图停在第一帧
                    currentPlayer = it
                    drawSpecial(it)
                }
            }
        }
        if (total > 0 && textMeasurer != null && !completedNow) {
            drawText(
                textMeasurer.measure(
                    "${written * 100/total}%",
                    TextStyle(fontSize = 16.sp),
                    density = this
                ),
                color = Color.Gray,
            )
        }
        // 唯一允许在 draw 里自失效的场景：动图正在播放（驱动 60fps）。
        // 播放结束（有限 loop）/非动图/已关闭时不再自增，静止画面不会持续重绘。
        if (animated && currentPlayer?.isRunning == true) {
            invalidateTick++
        }
    }

    /**
     * 非动画的渐进帧：锁内取 [_lastImg] 状态与字节快照，解码与绘制在锁外进行，
     * 避免 HTTP 线程写缓冲时读到半份数据。
     *
     * 解码节流：每来一块字节就整份解码是 O(n²)。只有首次占位、新增字节达到
     * `max(64KB, 已缓冲字节/10)`、或下载已完成时才重新解码。
     */
    private fun DrawScope.drawProgressiveFrame() {
        var bytesToDecode: ByteArray? = null
        var decodedSize = 0
        var image: ImageBitmap? = null
        synchronized(lock) {
            val last = _lastImg
            if (last == null) {
                val placeholder = ImageBitmap(_w.toInt(), _h.toInt())
                _lastImg = Pair(placeholder, 0)
                image = placeholder
            } else {
                val currentSize = _output.size()
                val pending = currentSize - last.second
                if (pending > 0 &&
                    (isCompletedLocked() || pending >= maxOf(64 * 1024, currentSize / 10))
                ) {
                    bytesToDecode = _output.toByteArray()
                    decodedSize = currentSize
                } else {
                    image = last.first
                }
            }
        }
        val bytes = bytesToDecode
        if (bytes != null) {
            val decoded = try {
                bytes.decodeToImageBitmap()
            } catch (_: Exception) {
                // 在安卓平台上部分 webp 数据解码出来的 Bitmap 可能是 null / 抛异常
                null
            }
            if (decoded != null) {
                synchronized(lock) {
                    // 解码期间可能已被 close()，不要往已释放的 painter 里塞位图
                    if (!_closed) _lastImg = Pair(decoded, decodedSize)
                }
                image = decoded
            }
        }
        val img = image ?: return
        try {
            if (!_sizeSet && fileSize() > 0 && img.height > 1 && img.width > 1) {
                _w = img.width.toFloat()
                _h = img.height.toFloat()
                _sizeSet = true
                onSizeKnown?.invoke(img.width, img.height)
            }
            drawImage(
                img,
                dstSize =
                    IntSize(
                        this.size.width.fastRoundToInt(),
                        this.size.height.fastRoundToInt(),
                    )
            )
        } catch (_: Exception) {
            //println(e.message)
        }
    }

    fun write(byteArray: ByteArray) {
        var complete = false
        var changed = false
        synchronized(lock) {
            if (_closed || _bufferFull) return
            if (_output.size() + byteArray.size > maxBufferedBytes) {
                // 单个文件过大时停止缓冲：渐进效果退化为普通加载，避免整份字节常驻内存
                _bufferFull = true
                complete = true
            } else {
                _output.write(byteArray)
                changed = true
                if (_totalFileSize > 0 && _output.size() == _totalFileSize) complete = true
            }
        }
        // 快照自增必须放在锁外：持锁写 SnapshotState 会同步通知观察者，可能反向取到注册表锁
        if (changed) invalidateTick++
        if (complete) {
            finish()
            // 动图：下载完成才允许开始整帧解码（且必须已显示，见 [maybeStartAnimatedDecode]）
            maybeStartAnimatedDecode()
        }
    }

    /**
     * 显示状态变化（由 [SharedPainterManager] 在 displayRefs 0→1 / 1→0 时调用）。
     * 动图解码只能由"显示"触发，预取但未显示的动图不允许提前解出全部帧。
     */
    fun setDisplayed(v: Boolean) {
        synchronized(lock) {
            displayed = v
        }
        if (v) maybeStartAnimatedDecode()
    }

    /**
     * 动图整帧解码的唯一入口。
     *
     * 前置条件：已显示、未关闭/销毁/出错、还没有解码任务或动图、没有下载占用、拥有完整字节。
     * Coil 入队本身放在锁外；回调里再检查一次是否已被 close()/destroy()，避免复活已释放的 painter。
     */
    private fun maybeStartAnimatedDecode() {
        if (!animated) return
        val url = sourceKey ?: return
        synchronized(lock) {
            if (_closed || _markToDestroy || _error || !displayed) return
            if (animatedImage != null || job != null || decodeStarting) return
            if (userCount != 0) return
            if (!isCompletedLocked()) return
            decodeStarting = true
        }
        val enqueued = try {
            SingletonImageLoader.get(platformContext).enqueue(
                ImageRequest.Builder(platformContext).data(url).build()
            ).job
        } catch (e: Exception) {
            e.printStackTrace()
            synchronized(lock) { decodeStarting = false }
            return
        }
        var started = false
        synchronized(lock) {
            decodeStarting = false
            if (_closed || _markToDestroy || job != null || animatedImage != null) {
                enqueued.cancel()
            } else {
                job = enqueued
                started = true
            }
        }
        if (started) {
            // 解码完成（成功或失败）后补一次自失效：draw 只依赖 invalidateTick，
            // 否则解码结果到达时没有任何帧循环来绘制它。
            enqueued.invokeOnCompletion {
                if (!_closed) invalidateTick++
            }
        }
    }

    fun setSize(w: Float, h: Float) {
        _w = w
        _h = h
        invalidateTick++
    }

    /**
     * 彻底释放：取消解码任务、关闭动图帧、完成 [finished]、清空已缓冲字节。
     * 幂等，可被窗口切换 / 引用计数归零 / 重复 dispose 多次调用。
     *
     * 只有注册表确认没有显示/下载引用时才会调用，因此清空 [_output] 不会饿死下载消费者。
     */
    fun close() {
        var image: MultiImagePlayer<*>? = null
        var pendingJob: Deferred<ImageResult>? = null
        synchronized(lock) {
            if (_closed) return
            // 先封闭，阻止 HTTP 线程继续写
            _closed = true
            pendingJob = job
            job?.cancel()
            job = null
            _lastImg = null
            image = animatedImage
            animatedImage = null
        }
        closeAnimated(image)
        // 解码已完成但还没被 draw 取走时，结果仍挂在 job 上；这里一并释放，避免 native 帧等 GC
        completedAnimatedImage(pendingJob)?.takeIf { it !== image }?.let { closeAnimated(it) }
        // 先完成 finished，再清空缓冲
        finish()
        synchronized(lock) {
            _output.reset()
        }
    }

    /**
     * Coil 成功显示时调用：只丢渐进解码出的位图与动图帧、取消解码任务，
     * 保留 [_output] 供下载路径（[bytes]/[finished]）继续使用。
     */
    fun destroy() {
        _markToDestroy = true
        var image: MultiImagePlayer<*>? = null
        var pendingJob: Deferred<ImageResult>? = null
        synchronized(lock) {
            _lastImg = null
            pendingJob = job
            job?.cancel()
            job = null
            image = animatedImage
            animatedImage = null
        }
        closeAnimated(image)
        // 解码完成但 draw 还没来得及取走时，job 上的结果也要释放
        completedAnimatedImage(pendingJob)?.takeIf { it !== image }?.let { closeAnimated(it) }
        finish()
    }

    /** 已完成但尚未被 draw 取走的解码结果（GifImage 等）；失败/取消时返回 null */
    private fun completedAnimatedImage(job: Deferred<ImageResult>?): MultiImagePlayer<*>? {
        if (job == null || !job.isCompleted) return null
        return runCatching { job.getCompleted().image as? MultiImagePlayer<*> }.getOrNull()
    }

    /** 锁外释放动图帧（close 可能较重且不应阻塞 HTTP 线程） */
    private fun closeAnimated(image: MultiImagePlayer<*>?) {
        if (image == null) return
        try {
            image.close()
        } catch (_: Exception) {
            // 帧释放失败不应影响其它资源的释放
        }
    }

    fun error() {
        synchronized(lock) {
            _error = true
        }
        finish()
    }

    /**
     * 完成 [finished]。
     * 必须在锁外调用：`complete` 可能同步恢复等待中的协程，若此时仍持有 painter 锁，
     * 恢复的代码再取注册表锁就会与"注册表锁 → painter 锁"的顺序形成反向死锁。
     */
    private fun finish() {
        finished.complete(_output)
    }

    fun isCompleted(): Boolean = synchronized(lock) { isCompletedLocked() }

    /** 调用方需持有 [lock] */
    private fun isCompletedLocked(): Boolean {
        return _totalFileSize > 0 && _output.size() == _totalFileSize
    }

    fun isDestroyed(): Boolean {
        return _markToDestroy
    }

    fun focus(): Boolean = synchronized(lock) {
        if (userCount > 0) {
            false
        } else {
            userCount++
            true
        }
    }

    fun release(): Boolean {
        var becameIdle = false
        val released = synchronized(lock) {
            if (userCount == 0) {
                false
            } else {
                userCount--
                becameIdle = userCount == 0
                true
            }
        }
        // 流式下载的最后一笔字节是在 focus 持有期间写完的，那时 userCount != 0；
        // 直到拦截器 finally 释放 focus 才真正空闲 —— 这里是"下载完成 + 空闲"的触发点。
        if (becameIdle) maybeStartAnimatedDecode()
        return released
    }

    fun fileSize(): Long = synchronized(lock) { _output.size().toLong() }

    /**
     * 整个文件能否全部缓冲进 [_output]。
     */
    fun canBufferAll(): Boolean = synchronized(lock) {
        _totalFileSize in 1..maxBufferedBytes
    }

    fun setFileSize(s: Long) {
        synchronized(lock) {
            _totalFileSize = s.toInt()
        }
    }

    fun getFileSize(): Long = synchronized(lock) { _totalFileSize.toLong() }

    fun bytes(): ByteArray = synchronized(lock) { _output.toByteArray() }

    fun isUsing(): Boolean = synchronized(lock) { userCount > 0 }

    fun isClosed(): Boolean = synchronized(lock) {
        _closed || _bufferFull || _error || isCompletedLocked() || isDestroyed()
    }

    companion object {
        /** 单个 painter 的缓冲上限（32MB） */
        private const val maxBufferedBytes = 32 * 1024 * 1024
    }
}
