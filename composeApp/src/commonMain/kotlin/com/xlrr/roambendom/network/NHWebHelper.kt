package com.xlrr.roambendom.network

import com.fleeksoft.ksoup.nodes.Element
import com.xlrr.roambendom.data.*
import com.xlrr.roambendom.data.nh.NHCdn
import com.xlrr.roambendom.data.nh.NHGallery
import com.xlrr.roambendom.data.nh.NHGalleryTagType
import com.xlrr.roambendom.data.nh.NHSearchLike
import com.xlrr.roambendom.data.nh.NHSearchSortType
import io.ktor.client.call.body
import io.ktor.client.request.*
import io.ktor.client.statement.request
import io.ktor.http.*
import kotlinx.io.IOException
import kotlin.random.Random

private const val prefix: String = "https://nhentai.net"

private fun unzipNHItem(ele: Element) : SearchItemData {
    val a = ele.child(0)
    val lang = CLanguage.languageDetect(ele.className())
    val id = a.attr("href").split("/").let { it[it.size - 2] }
    val thumb = a.getElementsByClass("lazyload").attr("src")
    val title = a.childElementsList().last().text()
    return SearchItemData(
        id,
        CSources.NHENTAI,
        title,
        -1,
        lang,
        dealWithUrl(thumb),
    )
}

private fun dealWithUrl(url: String) : String { // 2026/2/8 nhentai网站好像自2025年中旬开始把他大部分url的https去掉了
    return url.let { if (it.startsWith("//")) "https:$url" else it }
}

object NHWebHelper {
    const val API_PREFIX = "https://nhentai.net/api/v2/"
    private var cdn: NHCdn? = null
    private fun joinUrl(vararg arg: String): String {
        return arrayListOf(prefix).apply {
            addAll(arg.map { it.removePrefix("/").removeSuffix("/") })
        }.joinToString("/")
    }

    private fun String.connectWithCdn(thumb: Boolean = false): String {
        val pres = if (thumb) cdn?.thumbServers else cdn?.imageServers
        val pre = pres?.let {
            it[Random.nextInt(it.size)]
        } ?: "https://${if (thumb) "t" else "i"}1.nhentai.net"
        return "$pre/$this"
    }

    /**
     * refer to [api docs](https://nhentai.net/api/v2/docs#/)
    * */
    suspend inline fun <reified T> api(vararg post: String, block: HttpRequestBuilder.() -> Unit = {}) : Result<T> {
        val resp = NetHelper.client.get {
            url(API_PREFIX)
            url {
                appendPathSegments(*post)
            }
            block()
        }
        return if (resp.status == HttpStatusCode.OK) {
            Result.success(resp.body())
        } else {
            Result.failure(IOException("bad code: ${resp.status} about ${resp.request.url}"))
        }
    }

    private suspend fun checkCdn() {
        if (cdn == null) {
            cdn = api<NHCdn>("cdn").getOrNull()
        }
    }

    suspend fun searchNH(key: String, page: Int = 1, sortType: NHSearchSortType = NHSearchSortType.DATE): NHSearchLike? {
        checkCdn()
        if (key.isEmpty()) {
            return null
        }
        return api<NHSearchLike>("search") {
            parameter("query", key)
            parameter("page", page)
            parameter("sort", sortType)
        }.getOrNull()
    }

    suspend fun galleryNH(id: String): Result<NHGallery> {
        checkCdn()
        return if (id.isNotEmpty()) api<NHGallery>("galleries",id) else Result.failure(Exception("id should be not empty"))
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
        if (key.isNotEmpty()) {
            val sh = searchNH(key, page)
            val total = sh?.total ?: 0
            return result(total, sh?.result?.map {
                SearchItemData(
                    it.id.toString(),
                    CSources.NHENTAI,
                    it.englishTitle,
                    it.numPages,
                    CLanguage.languageDetect(it.tagIds),
                    it.thumbnail.connectWithCdn(true),
                    ai = it.tagIds.contains(145703),
                    restriction = if (it.tagIds.any {n -> n in CRestriction.nhNonHTag})
                        CRestriction.Normal else CRestriction.R18
                )
            } ?: listOf())
        }
        val doc = if (key.isNotEmpty()) NetHelper.getWebDocument(joinUrl("search")) {
            parameter("q", key)
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
        return result(tot, works.map { unzipNHItem(it) })
    }

    suspend fun artwork(id: String): ArtworkInfo {
        val info = ArtworkInfo()
        val s = galleryNH(id).getOrThrow()
        info.cover = s.cover.path.connectWithCdn(true)
        info.title = s.title.english
        info.altitle = s.title.japanese ?: ""
        info.tags = s.tags.filter { it.type == NHGalleryTagType.tag }.map { it.name }
        info.groups = s.tags.filter { it.type == NHGalleryTagType.group }.map { it.name }
        info.authors = s.tags.filter { it.type == NHGalleryTagType.artist }.map { it.name }
        info.page = s.numPages
        s.tags.filter { it.type == NHGalleryTagType.language }.let {
            if (it.size > 1) info.translated = true
            info.language = CLanguage.convert(it.lastOrNull { x -> x.id != 17249 }?.name ?: "unknow")
        }
        info.thumbUrls = s.pages.map { it.thumbnail.connectWithCdn(true) }
        info.pageUrls = s.pages.map { "${it.path.connectWithCdn()}[w${it.width}h${it.height}]{}" }
        info.likeCount = s.numFavorites
        info.time = s.uploadDate * 1000 // 服务器返回的是以秒(s)为单位的时间戳
        info.ai = s.tags.any { it.id == 145703 }
        info.restriction = if (s.tags.any {n -> n.id in CRestriction.nhNonHTag})
            CRestriction.Normal else CRestriction.R18
        return info
    }
}