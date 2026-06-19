package com.xlrr.roambendom

import androidx.compose.ui.graphics.drawscope.DrawScope
import coil3.ComponentRegistry
import com.xlrr.roambendom.gif.MultiImagePlayer

interface Platform {
    val name: String
}
const val VERSION = "1.2.0" //修改此处后需修改build.gradle
expect fun getPlatform(): Platform
expect fun getFormatVersionString(): String

expect fun ComponentRegistry.Builder.addGifLoader()
expect fun DrawScope.drawSpecial(img: MultiImagePlayer<*>)