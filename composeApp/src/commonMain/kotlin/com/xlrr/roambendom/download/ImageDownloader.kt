package com.xlrr.roambendom.download

import coil3.PlatformContext
import coil3.SingletonImageLoader
import coil3.request.ImageRequest
import com.xlrr.roambendom.network.defaultImageRequest
import com.xlrr.roambendom.progressive.SharedPainterManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.time.Duration.Companion.milliseconds

object ImageDownloader {
    private const val WAIT_MS = 300_000L

    /** 缓存命中直接给出原始字节，否则挂 ProgressivePainter 下载并在协程里等待 */
    suspend fun bytes(url: String, context: PlatformContext): ByteArray? {
        val request = defaultImageRequest(url, context)
        val key = request.data.toString()
        cached(request, context)?.let { return it }
        val painter = SharedPainterManager.acquireDownload(request, context)
        try {
            val job = runCatching { SingletonImageLoader.get(context).enqueue(request).job }.getOrNull()
            runCatching { job?.await() }
            cached(request, context)?.let { return it }
            if (!painter.isUsing() && painter.getFileSize() <= 0) return null
            return withTimeoutOrNull(WAIT_MS.milliseconds) {
                painter.finished.await().takeIf { painter.isCompleted() }?.toByteArray()
            } ?: cached(request, context)
        } finally {
            SharedPainterManager.releaseDownload(key)
        }
    }

    /** painter 已完成优先，其次磁盘缓存 */
    private suspend fun cached(request: ImageRequest, context: PlatformContext): ByteArray? {
        SharedPainterManager.find(request)?.takeIf { it.isCompleted() }?.let { return it.bytes() }
        val loader = SingletonImageLoader.get(context)
        val key = request.diskCacheKey ?: return null
        val cache = loader.diskCache ?: return null
        return withContext(Dispatchers.IO) {
            runCatching {
                cache.openSnapshot(key)?.use { cache.fileSystem.read(it.data) { readByteArray() } }
            }.getOrNull()?.takeIf { it.isNotEmpty() }
        }
    }

    suspend fun download(item: DownloadItem, context: PlatformContext): DownloadedImage? {
        return bytes(item.url, context)?.let { DownloadedImage(item, it) }
    }
}
