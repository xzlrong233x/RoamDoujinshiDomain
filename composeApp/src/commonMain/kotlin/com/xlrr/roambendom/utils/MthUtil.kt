package com.xlrr.roambendom.utils

import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Stable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Density
import kotlin.math.floor

object MthUtil {
    fun calWindow(
        page: Int,
        per: Int,
        size: Int,
        startIndex: Int = 0,
    ): Pair<Int, Int> =
        Pair(
            ((page - 1) * per + startIndex).coerceIn(0, size),
            (page * per + startIndex).coerceIn(0, size),
        )

    data class HSV(
        val h: Float,
        val s: Float,
        val v: Float,
    )

    fun rgbToHsv(color: Color): HSV = rgbToHsv((color.red * 255).toInt(), (color.green * 255).toInt(), (color.blue * 255).toInt())

    fun rgbToHsv(
        r: Int,
        g: Int,
        b: Int,
    ): HSV {
        val rf = r / 255f
        val gf = g / 255f
        val bf = b / 255f

        val max = maxOf(rf, gf, bf)
        val min = minOf(rf, gf, bf)
        val delta = max - min

        // 色相 H
        val h =
            when {
                delta == 0f -> 0f
                max == rf -> 60f * (((gf - bf) / delta) % 6f)
                max == gf -> 60f * (((bf - rf) / delta) + 2f)
                else -> 60f * (((rf - gf) / delta) + 4f)
            }.let { if (it < 0) it + 360f else it }

        // 饱和度 S
        val s = if (max == 0f) 0f else delta / max

        // 明度 V
        val v = max

        return HSV(h, s, v)
    }

    fun Color.Companion.hsv(hsv: HSV): Color = Color.hsv(hsv.h, hsv.s, hsv.v)

    @Stable
    fun Modifier.adaptiveSize(
        windowWidth: Float,
        windowHeight: Float,
        w: Float,
        h: Float,
        density: Density,
        noLong: Boolean = false,
    ): Modifier {
        val testH = h * windowWidth / w
        return if (testH < windowHeight) {
            width(with(density) { windowWidth.toDp() })
                .heightIn(min = with(density) { testH.toDp() })
        } else {
            height(
                with(density) {
                    (
                        if (noLong) {
                            1f
                        } else {
                            floor(testH / windowHeight).coerceIn(
                                1f,
                                null,
                            ) * windowHeight
                        }
                    ).toDp()
                },
            )
        }
    }
}
