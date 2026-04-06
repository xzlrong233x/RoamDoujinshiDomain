package com.xlrr.roambendom

import android.os.Build
import coil3.ComponentRegistry
import coil3.gif.AnimatedImageDecoder
import com.xlrr.roambendom.ugoira.UgoiraDecoder

class AndroidPlatform : Platform {
    override val name: String = "Android ${Build.VERSION.SDK_INT}"
}

actual fun getPlatform(): Platform = AndroidPlatform()
actual fun getFormatVersionString(): String = "Android;v$VERSION"
actual fun ComponentRegistry.Builder.addGifLoader() {
    add(AnimatedImageDecoder.Factory())
    add(UgoiraDecoder.Factory())
}