package com.xlrr.roambendom

import coil3.ComponentRegistry

interface Platform {
    val name: String
}
const val VERSION = "1.0.1"
expect fun getPlatform(): Platform
expect fun getFormatVersionString(): String

expect fun ComponentRegistry.Builder.addGifLoader()