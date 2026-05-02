package com.xlrr.roambendom.gif

import coil3.ImageLoader
import coil3.decode.DecodeResult
import coil3.decode.Decoder
import coil3.decode.ImageSource
import coil3.fetch.SourceFetchResult
import coil3.request.Options
import com.shakster.gifkt.GifDecoder
import com.xlrr.roambendom.utils.isGif
import org.jetbrains.skiko.toImage
import kotlin.time.DurationUnit

class CoilGIFDecoder(
    private val source: ImageSource,
): Decoder {
    override suspend fun decode(): DecodeResult? {
        try {
            //println(source.source().readString(24, Charsets.UTF_8))
            val ba = source.source().readByteArray()
            val gifD = GifDecoder(ba, 0)
            return DecodeResult(
                GifImage(
                    gifD.asList().associateBy(
                        {it.toBufferedImage().toImage()},
                        {it.duration.toInt(DurationUnit.MILLISECONDS)}
                    ),
                    ba.size.toLong(),
                    gifD.loopCount
                ),
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
            return CoilGIFDecoder(result.source)
        }
    }
}