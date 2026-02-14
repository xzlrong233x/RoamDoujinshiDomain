package com.xlrr.roambendom.network

import com.fleeksoft.ksoup.nodes.Element
import com.xlrr.roambendom.data.*
import io.ktor.client.request.*
import io.ktor.http.*
import kotlin.time.Instant

private const val prefix: String = "https://nhentai.net"

private fun unzipNHItem(ele: Element) : SearchItemData {
    val a = ele.child(0)
    val lang = CLanguage.languageDetect(ele.attr("data-tags"))
    val id = a.attr("href").split("/").let { it[it.size - 2] }
    val thumb = a.child(0).attr("data-src")
    val title = a.childElementsList().last().text()
    return SearchItemData(
        id,
        title,
        -1,
        lang,
        dealWithUrl(thumb),
        CSources.NHENTAI
    )
}

private fun dealWithUrl(url: String) : String { // 2026/2/8 nhentai网站好像自2025年中旬开始把他大部分url的https去掉了
    return url.let { if (it.startsWith("//")) "https:$url" else it }
}

object NHWebHelper {
    private fun joinUrl(vararg arg: String): String {
        return arrayListOf(prefix).apply {
            addAll(arg.map { it.removePrefix("/").removeSuffix("/") })
        }.joinToString("/")
    }

    /**
     * 在nhentai上进行搜索，如果key为空则返回首页。
     *
     * @param key 关键字，必须没被url编码
     * @param page 页码
     * */
    suspend fun search(key: String, page: Int = 1): SearchResult {
        fun result(total: Int, lis: List<SearchItemData>): SearchResult {
            return SearchResult(
                total,
                lis,
                key,
                page
            )
        }
        val doc = if (key.isNotEmpty()) NetHelper.getWebDocument(joinUrl("search")) {
            parameter("q", key.encodeURLQueryComponent())
            parameter("page", page)
            defaultHeader()
        }
        else {
            NetHelper.getWebDocument(prefix) {
                parameter("page", page)
            }
        } ?: return result(0, listOf())
        //if (
        //    doc.body().let {
        //        it.select("#content > div.container.index-container > h2").isNotEmpty()
        //                || it.select(".container.error").isNotEmpty()
        //    }
        //) return result(0, listOf())
        val works = doc.body().select(".gallery")
        var tot = -1
        if (key.isNotEmpty()) {
            val totaltxt = doc.body().select("#content > h1")
            tot = totaltxt.text().filter { it.isDigit() }.toInt()
        }
        return result(tot, works.map { unzipNHItem(it) })
    }

    suspend fun artwork(id: String): ArtworkInfo {
        val info = ArtworkInfo()
        val doc = NetHelper.getWebDocument(joinUrl("g", id)) {
            defaultHeader()
        } ?: return info
        info.cover = dealWithUrl(doc.select("#cover > a > img").attr("data-src"))
        info.title = doc.select("#info > h1 > span").joinToString(" ") { it.text() }
        info.altitle = doc.select("#info > h2 > span").joinToString(" ") { it.text() }
        info.page = doc.select("#tags > div:nth-child(8) > span > a > span").text().toIntOrNull() ?: 0
        info.tags = doc.select("#tags > div:nth-child(3) > span > a > span.name").map { it.text() }
        info.groups = doc.select("#tags > div:nth-child(5) > span > a > span.name").map { it.text() }
        val lang = doc.select("#tags > div:nth-child(6) > span > a > span.name").map { it.text() }
        if (lang.size == 2) {
            info.translated = true
        }
        info.language = CLanguage.convert(lang[lang.size-1])
        info.authors = doc.select("#tags > div:nth-child(4) > span > a > span.name").map { it.text() }
        doc.select(".gallerythumb > img").let { y ->
            info.thumbUrls = y.map { dealWithUrl(it.attr("data-src")) }
            info.pageUrls = info.thumbUrls.map {
                it.replace(Regex("t(\\d)")) { x ->
                    "i${x.groups.last()?.value.toString()}"
                }.replace(Regex("(\\d+)t")) {x ->
                    x.groups.last()?.value.toString()
                }.replace(Regex("(\\.[^./]+).webp")) {x ->
                    x.groups.last()?.value.toString()
                }
            }
        }

        info.time = Instant.parse(doc.select("time").attr("datetime")).toEpochMilliseconds()
        return info
    }
}