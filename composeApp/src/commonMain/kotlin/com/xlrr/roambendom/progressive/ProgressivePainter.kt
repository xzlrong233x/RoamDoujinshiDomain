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
import kotlinx.coroutines.Deferred

class ProgressivePainter(
    val textMeasurer: TextMeasurer? = null,
    val animated: Boolean = false,
    val platformContext: PlatformContext
) : Painter() {
    private var _w: Float = 1f
    private var _h: Float = 1f
    override val intrinsicSize: Size
        get() = Size(_w,_h)
    private var _sizeSet = false
    private val _output = ByteArrayOutputStream()
    private var invalidateTick by mutableIntStateOf(0)
    private var _markToDestroy by mutableStateOf(false)
    private var _lastImg: Pair<ImageBitmap, Int>? = null
    private var _totalFileSize = 0
    private var userCount = 0
    private var _error = false
    var animatedImage: MultiImagePlayer<*>? = null
        private set
    private var job: Deferred<ImageResult>? = null
        set(value) {
            field?.cancel()
            field = value
        }

    override fun DrawScope.onDraw() {
        if (!animated) {
            if (_lastImg == null) {
                _lastImg = Pair(ImageBitmap(_w.toInt(), _h.toInt()), 0)
            } else {
                if (_output.size() > (_lastImg?.second ?: 0)) {
                    _lastImg = Pair(_output.toByteArray().decodeToImageBitmap(), _output.size())
                }
            }
            val img = _lastImg?.first
            img?.let {
                try { // 在安卓平台上部分webp数据解码出来的Bitmap可能是null
                    if (!_sizeSet) {
                        _w = it.width.toFloat()
                        _h = it.height.toFloat()
                    }
                    drawImage(
                        it,
                        dstSize =
                            IntSize(
                                this@onDraw.size.width.fastRoundToInt(),
                                this@onDraw.size.height.fastRoundToInt(),
                            )
                    )
                } catch (_: Exception) {
                    //println(e.message)
                }
            }
        } else {
            if (animatedImage == null && job?.isCompleted != true && _totalFileSize > 0) {
                drawArc(
                    Color.DarkGray,
                    0f,
                    _output.size() * 360f/_totalFileSize,
                    false,
                    Offset(size.width / 2, size.height / 2),
                    Size(75f, 75f),
                    1f,
                    Stroke(2f),
                )
            } else if (job?.isCompleted == true) {
                animatedImage = job?.getCompleted()?.image as? MultiImagePlayer<*>
                animatedImage?.let {
                    drawSpecial(it)
                }
            }
        }
        if (_totalFileSize > 0 && textMeasurer != null && !isCompleted()) {
            drawText(
                textMeasurer.measure(
                    "${_output.size() * 100/_totalFileSize}%",
                    TextStyle(fontSize = 16.sp),
                    density = this
                ),
                color = Color.Gray,
            )
        }
        if (animatedImage == null && job == null && isCompleted() && animated && userCount == 0) {
            job = SingletonImageLoader.get(platformContext).enqueue(
                ImageRequest.Builder(platformContext).data(SharedPainterManager.map.firstNotNullOf {
                    if (it.value != this@ProgressivePainter) null else it.key
                }).build()
            ).job
        }
        if (!isClosed() || animated) {
            invalidateTick++
        }
    }

    fun write(byteArray: ByteArray) {
        _output.write(byteArray)
    }

    fun setSize(w: Float, h: Float) {
        _w = w
        _h = h
        _sizeSet = true
        invalidateTick++
    }

    fun destroy() {
        invalidateTick = 0
        _markToDestroy = true
    }

    fun error() {
        _error = true
    }

    fun isCompleted(): Boolean {
        return _totalFileSize > 0 && _output.size() == _totalFileSize
    }

    fun isDestroyed(): Boolean {
        return _markToDestroy
    }

    fun focus(): Boolean {
        if (userCount > 0) return false
        userCount++
        return true
    }

    fun release(): Boolean {
        if (userCount == 0) return false
        userCount--
        return true
    }

    fun fileSize(): Long {
        return _output.size().toLong()
    }

    fun setFileSize(s: Long) {
        _totalFileSize = s.toInt()
    }

    fun bytes(): ByteArray {
        return _output.toByteArray()
    }

    fun isUsing(): Boolean = userCount > 0

    fun isClosed(): Boolean = _error || isCompleted() || isDestroyed()
}