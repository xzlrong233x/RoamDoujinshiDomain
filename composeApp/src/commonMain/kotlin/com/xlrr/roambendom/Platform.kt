package com.xlrr.roambendom

import coil3.ComponentRegistry

interface Platform {
    val name: String
}
const val VERSION = "1.1.1" //修改此处后需修改build.gradle
expect fun getPlatform(): Platform
expect fun getFormatVersionString(): String

expect fun ComponentRegistry.Builder.addGifLoader()