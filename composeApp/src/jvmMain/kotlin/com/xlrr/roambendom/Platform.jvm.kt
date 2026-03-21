package com.xlrr.roambendom

import coil3.ComponentRegistry
import com.xlrr.roambendom.gif.CoilGIFDecoder

class JVMPlatform : Platform {
    override val name: String = "Java ${System.getProperty("java.version")}"
}

actual fun getPlatform(): Platform = JVMPlatform()
actual fun getFormatVersionString(): String = "jvm client v${VERSION}"
actual fun ComponentRegistry.Builder.addGifLoader() {
    add(CoilGIFDecoder.Factory())
}