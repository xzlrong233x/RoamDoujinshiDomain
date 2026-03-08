package com.xlrr.roambendom

interface Platform {
    val name: String
}
const val VERSION = "1.0.1"
expect fun getPlatform(): Platform
expect fun getFormatVersionString(): String