package com.xlrr.roambendom.download

import android.content.ContentValues
import android.provider.MediaStore
import coil3.PlatformContext

private const val IMG_PATH = "Pictures/ROAMBENDOM"

private fun mimeOf(ext: String) = when (ext.lowercase()) {
    "jpg", "jpeg" -> "image/jpeg"
    "gif" -> "image/gif"
    "webp" -> "image/webp"
    "png" -> "image/png"
    else -> "image/*"
}

actual suspend fun saveImage(context: PlatformContext, bytes: ByteArray, name: String, ext: String): Boolean {
    val resolver = context.contentResolver
    val values = ContentValues().apply {
        put(MediaStore.Images.Media.DISPLAY_NAME, "$name.$ext")
        put(MediaStore.Images.Media.MIME_TYPE, mimeOf(ext))
        put(MediaStore.Images.Media.RELATIVE_PATH, IMG_PATH)
    }
    val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values) ?: return false
    return try {
        resolver.openOutputStream(uri)?.use { it.write(bytes) }
        true
    } catch (e: Exception) {
        e.printStackTrace()
        resolver.delete(uri, null, null)
        false
    }
}

private class GalleryTarget(private val context: PlatformContext) : SaveTarget {
    override suspend fun save(images: List<DownloadItem>, context: PlatformContext): Int =
        images.count { item ->
            ImageDownloader.download(item, context).let {
                it != null && saveImage(context, it.bytes, it.item.name, it.item.ext)
            }
        }
}

actual suspend fun prepareDownloadTarget(context: PlatformContext): SaveTarget? = GalleryTarget(context)
