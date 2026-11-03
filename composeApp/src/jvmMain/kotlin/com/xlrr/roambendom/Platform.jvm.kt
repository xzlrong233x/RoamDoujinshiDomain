package com.xlrr.roambendom

import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.toComposeImageBitmap
import coil3.ComponentRegistry
import com.xlrr.roambendom.gif.CoilGIFDecoder
import com.xlrr.roambendom.gif.GifImage
import com.xlrr.roambendom.gif.MultiImagePlayer
import com.xlrr.roambendom.gif.UgoiraDecoder
import org.jetbrains.skia.Image

class JVMPlatform : Platform {
    override val name: String = "Java ${System.getProperty("java.version")}"
}

actual fun getPlatform(): Platform = JVMPlatform()
actual fun getFormatVersionString(): String = "JVM;v${VERSION}"
actual fun ComponentRegistry.Builder.addGifLoader() {
    add(CoilGIFDecoder.Factory())
    add(UgoiraDecoder.Factory())
}

actual fun DrawScope.drawSpecial(img: MultiImagePlayer<*>) {
    val gif = img as? GifImage
    if (gif?.isClosed() == true) return
    img.generalDraw {
        if (it is Image) {
            // GifImage 内部缓存帧的 Compose 包装，避免每帧新建包装对象
            drawImage(gif?.bitmapFor(it) ?: it.toComposeImageBitmap())
        }
    }
}
