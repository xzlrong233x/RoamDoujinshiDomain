package com.xlrr.roambendom.utils

import coil3.Extras
import com.xlrr.roambendom.data.pixiv.UgoiraFrameItem
import okio.BufferedSource
import okio.ByteString.Companion.encodeUtf8

val framesKey = Extras.Key<List<UgoiraFrameItem>>(listOf())
private val GIF_HEADER_87A = "GIF87a".encodeUtf8()
private val GIF_HEADER_89A = "GIF89a".encodeUtf8()
private val ZIP_HEADER = "PK\u0003\u0004".encodeUtf8()

fun isGif(source: BufferedSource): Boolean {
    return source.rangeEquals(0, GIF_HEADER_89A) ||
            source.rangeEquals(0, GIF_HEADER_87A)
}

fun isZip(source: BufferedSource): Boolean {
    return source.rangeEquals(0, ZIP_HEADER)
}