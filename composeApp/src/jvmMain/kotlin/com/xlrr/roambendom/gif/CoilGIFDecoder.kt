package com.xlrr.roambendom.gif

import coil3.ImageLoader
import coil3.decode.DecodeResult
import coil3.decode.Decoder
import coil3.decode.ImageSource
import coil3.fetch.SourceFetchResult
import coil3.request.Options
import com.shakster.gifkt.GifDecoder
import com.xlrr.roambendom.utils.isGif

class CoilGIFDecoder(
    private val source: ImageSource,
    private val options: Options,
    private val enforceMinimumFrameDelay: Boolean = true,
): Decoder {
    override suspend fun decode(): DecodeResult? {
        try {
            //println(source.source().readString(24, Charsets.UTF_8))
            val ba = source.source().readByteArray()
            val gifD = GifDecoder(ba, 0)
            return DecodeResult(
                GifImage(gifD, ba.size.toLong()),
                false
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }

    class Factory(
        val enforceMinimumFrameDelay: Boolean = true,
    ) : Decoder.Factory {

        override fun create(
            result: SourceFetchResult,
            options: Options,
            imageLoader: ImageLoader,
        ): Decoder? {
            if (!isGif(result.source.source())) return null
            return CoilGIFDecoder(result.source, options, enforceMinimumFrameDelay)
        }
    }
}