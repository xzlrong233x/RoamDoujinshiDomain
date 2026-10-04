package com.xlrr.roambendom.download

import coil3.PlatformContext
import io.github.vinceglb.filekit.FileKit
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.div
import io.github.vinceglb.filekit.dialogs.FileKitDialogSettings
import io.github.vinceglb.filekit.dialogs.openDirectoryPicker
import io.github.vinceglb.filekit.dialogs.openFileSaver
import io.github.vinceglb.filekit.write

actual suspend fun saveImage(context: PlatformContext, bytes: ByteArray, name: String, ext: String): Boolean {
    val file = FileKit.openFileSaver(
        suggestedName = name,
        defaultExtension = ext,
        dialogSettings = FileKitDialogSettings(title = "保存图片")
    ) ?: return false
    return runCatching { file.write(bytes) }.isSuccess
}

private class DirectoryTarget(private val dir: PlatformFile) : SaveTarget {
    override suspend fun save(images: List<DownloadItem>, context: PlatformContext): Int =
        images.count { item ->
            runCatching {
                var n = ImageDownloader.download(item, context)
                checkNotNull(n).let {
                    (dir / "${it.item.name}.${it.item.ext}").write(it.bytes)
                }
                n = null
            }.isSuccess
        }
}

actual suspend fun prepareDownloadTarget(context: PlatformContext): SaveTarget? =
    FileKit.openDirectoryPicker()?.let { DirectoryTarget(it) }
