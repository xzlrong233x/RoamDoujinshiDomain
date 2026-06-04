package com.xlrr.roambendom.painter

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.drawText

class TextPlaceholderPainter(
    override val intrinsicSize: Size,
    val text: String,
    val textMeasurer: TextMeasurer
) : Painter() {
    override fun DrawScope.onDraw() {
        val res = textMeasurer.measure(text)
        drawText(
            res,
            topLeft = Offset(
                (this.size.width - res.size.width) / 2f,
                (this.size.height - res.size.height) / 2f
            )
        )
    }
}