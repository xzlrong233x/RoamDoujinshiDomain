package com.xlrr.roambendom

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform