package com.xlrr.roambendom.gif

import coil3.ImageLoader
import coil3.decode.DecodeResult
import coil3.decode.Decoder
import coil3.fetch.SourceFetchResult
import coil3.request.Options
import com.xlrr.roambendom.data.pixiv.UgoiraFrameItem
import com.xlrr.roambendom.utils.framesKey
import com.xlrr.roambendom.utils.isZip
import org.jetbrains.skia.Image
import java.io.ByteArrayOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream

class UgoiraDecoder(
    val zip: ZipInputStream,
    val frames: List<UgoiraFrameItem>
): Decoder {
    override suspend fun decode(): DecodeResult? {
        var ent: ZipEntry? = zip.nextEntry
        val mp = LinkedHashMap<Image, Int>()
        while (ent != null) {
            try {
                val byte = ByteArrayOutputStream()
                zip.copyTo(byte)
                mp[Image.makeFromEncoded(byte.toByteArray())] = (frames.find { it.file == ent.name }?.delay ?: 100)
                byte.close()
            } catch (e: Exception) {
                e.printStackTrace()
            }
            ent = zip.nextEntry
        }
        return DecodeResult(
            GifImage(mp, 0),
            false
        )
    }

    class Factory(
    ) : Decoder.Factory {

        override fun create(
            result: SourceFetchResult,
            options: Options,
            imageLoader: ImageLoader,
        ): Decoder? {
            try {
                if (result.mimeType == "application/zip" || options.extras[framesKey] != null || isZip(result.source.source())) {
                    return UgoiraDecoder(
                        ZipInputStream(result.source.source().inputStream()),
                        options.extras[framesKey] ?: listOf()
                    )
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            return null
        }
    }
}
