package com.xlrr.roambendom.download

import com.xlrr.roambendom.data.ArtworkInfo
import com.xlrr.roambendom.network.UrlWithSize

enum class ImageQuality { Normal, Original }

data class DownloadItem(val url: String, val name: String, val ext: String) {
    companion object {
        fun of(url: String): DownloadItem {
            val path = url.substringBefore('?').substringAfterLast('/')
            val ext = path.substringAfterLast('.', "").lowercase()
            return DownloadItem(url, path.removeSuffix(".$ext").ifEmpty { path }, ext.ifEmpty { "jpg" })
        }
    }
}

data class DownloadedImage(val item: DownloadItem, val bytes: ByteArray)

/** pages 为 [ArtworkInfo.pageUrls] 下标；原图缺失时回退普通图 */
fun ArtworkInfo.downloadItems(
    pages: Collection<Int>,
    quality: ImageQuality = ImageQuality.Normal
): List<DownloadItem> = pages.sorted().mapNotNull { i ->
    UrlWithSize.parse(pageUrls.getOrNull(i) ?: return@mapNotNull null)
        .takeIf { it.url.isNotEmpty() }
        ?.let { uws ->
            DownloadItem.of(
                if (quality == ImageQuality.Original && uws.oriUrl.isNotEmpty()) uws.oriUrl else uws.url
            )
        }
}
