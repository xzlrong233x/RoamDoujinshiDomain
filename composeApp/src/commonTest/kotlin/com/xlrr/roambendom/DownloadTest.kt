package com.xlrr.roambendom

import com.xlrr.roambendom.data.ArtworkInfo
import com.xlrr.roambendom.download.DownloadItem
import com.xlrr.roambendom.download.ImageQuality
import com.xlrr.roambendom.download.downloadItems
import kotlin.test.Test
import kotlin.test.assertEquals

class DownloadTest {
    private val normal = "https://i.pximg.net/c/540x360/img/master/1_p0_master1200.jpg"
    private val original = "https://i.pximg.net/img-original/img/1_p0.jpg"
    private val page = "$normal[w1000h2000]{$original}"
    private val nh = "https://i1.nhentai.net/g/1/1.jpg"

    @Test
    fun itemOfSplitsNameAndExt() {
        assertEquals(DownloadItem(original, "1_p0", "jpg"), DownloadItem.of(original))
        assertEquals(DownloadItem(nh + "?v=1", "1", "jpg"), DownloadItem.of("$nh?v=1"))
        assertEquals(DownloadItem("https://a/b/plain", "plain", "jpg"), DownloadItem.of("https://a/b/plain"))
        assertEquals(DownloadItem("https://a/b/u.zip", "u", "zip"), DownloadItem.of("https://a/b/u.zip"))
    }

    @Test
    fun picksRequestedQuality() {
        val info = ArtworkInfo(page = 1, pageUrls = listOf(page))
        assertEquals(listOf(original), info.downloadItems(listOf(0), ImageQuality.Original).map { it.url })
        assertEquals(listOf(normal), info.downloadItems(listOf(0), ImageQuality.Normal).map { it.url })
        assertEquals(listOf(normal), info.downloadItems(listOf(0)).map { it.url })
    }

    @Test
    fun originalFallsBackWhenMissing() {
        val info = ArtworkInfo(page = 1, pageUrls = listOf("$nh[w100h200]{}"))
        assertEquals(nh, info.downloadItems(listOf(0), ImageQuality.Original).single().url)
        assertEquals(DownloadItem(nh, "1", "jpg"), info.downloadItems(listOf(0)).single())
    }

    @Test
    fun ordersPagesAndSkipsBadOnes() {
        val info = ArtworkInfo(
            page = 2,
            pageUrls = listOf(page, "not-a-formatted-url", "$nh[w1h1]{}")
        )
        assertEquals(listOf("1_p0_master1200", "1"), info.downloadItems(listOf(2, 0, 5, 1)).map { it.name })
    }
}
