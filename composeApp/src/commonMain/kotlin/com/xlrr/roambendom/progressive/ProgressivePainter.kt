package com.xlrr.roambendom.progressive

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.decodeToImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.fastRoundToInt
import com.fleeksoft.io.ByteArrayOutputStream

class ProgressivePainter(
    val textMeasurer: TextMeasurer? = null
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

    override fun DrawScope.onDraw() {
        if (_lastImg == null) {
            _lastImg = Pair(ImageBitmap(_w.toInt(), _h.toInt()), 0)
        } else {
            if (_output.size() > (_lastImg?.second ?: 0)) {
                _lastImg = Pair(_output.toByteArray().decodeToImageBitmap(), _output.size())
            }
        }
        val img = _lastImg!!.first
        if (!_sizeSet) {
            _w = img.width.toFloat()
            _h = img.height.toFloat()
        }
        drawImage(
            img,
            dstSize =
                IntSize(
                    this@onDraw.size.width.fastRoundToInt(),
                    this@onDraw.size.height.fastRoundToInt(),
                )
        )
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
        if (!isClosed()) {
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