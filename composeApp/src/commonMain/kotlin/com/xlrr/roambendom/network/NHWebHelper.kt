package com.xlrr.roambendom.network

import com.xlrr.roambendom.data.*
import com.xlrr.roambendom.data.nh.*
import com.xlrr.roambendom.network.NHWebHelper.API_PREFIX
import com.xlrr.roambendom.network.NHWebHelper.api
import com.xlrr.roambendom.third.EchRequestRustClass
import io.ktor.http.*
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlin.random.Random

private const val prefix: String = "https://nhentai.net"

private fun dealWithUrl(url: String) : String { // 2026/2/8 nhentai网站好像自2025年中旬开始把他大部分url的https去掉了
    return url.let { if (it.startsWith("//")) "https:$url" else it }
}

object NHWebHelper {
    const val API_PREFIX = "https://nhentai.net/api/v2/"

    /**
     * ECH 连接地址候补。[RBDDns] 本来就给 nhentai 钉了 Cloudflare IP，直接拿来用：
     * cloudflare-ech.com 的 A 记录成功用过一次就会被封锁 90~155 秒，
     * 有候补就不至于在这段时间里一直失败。
     *
     * [api] 是 public inline，只能访问 @PublishedApi 的成员。
     * */
    @PublishedApi
    internal val echCandidates = RBDDns.mainNH.joinToString(",") { it.hostAddress.orEmpty() }

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
     *
     * 请求统一走 Rust 的 ECH 通道，原生层对非 2xx 会抛异常。
     *
     * @param path 路径片段，会拼在 [API_PREFIX] 后面
     * @param params 查询参数
     * @param form 为 null 时发 GET，否则把它当成 JSON 请求体发 POST
     * */
    suspend inline fun <reified T> api(
        vararg path: String,
        params: Map<String, Any> = emptyMap(),
        form: JsonObject? = null
    ) : Result<T> = runCatching {
        val url = buildString {
            append(API_PREFIX)
            append(path.joinToString("/"))
            if (params.isNotEmpty()) append(params.entries.joinToString("&", "?") { (k, v) ->
                "${k.encodeURLParameter()}=${v.toString().encodeURLParameter()}"
            })
        }
        val text = if (form == null) EchRequestRustClass.baseHttpGet(url, echCandidates)
            else EchRequestRustClass.baseHttpPost(url, form.toString(), echCandidates)
        NetHelper.json.decodeFromString<T>(text)
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
        return api<NHSearchLike>(
            "search",
            params = mapOf("query" to key, "page" to page, "sort" to sortType)
        ).getOrNull()
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
    suspend fun search(key: String, page: Int = 1, sortType: NHSearchSortType = NHSearchSortType.DATE): SearchResult {
        fun result(total: Int, lis: List<SearchItemData>): SearchResult {
            return SearchResult(
                total,
                lis,
                key,
                page + 1
            )
        }
        fun toS(it: NHSearchLikeResultItem): SearchItemData {
            return SearchItemData(
                it.id.toString(),
                CSources.NHENTAI,
                it.englishTitle,
                it.numPages,
                CLanguage.languageDetect(it.tagIds),
                it.thumbnail.connectWithCdn(true),
                it.thumbnailWidth,
                it.thumbnailHeight,
                ai = it.tagIds.contains(145703),
                restriction = if (it.tagIds.any {n -> n in CRestriction.nhNonHTag})
                    CRestriction.Normal else CRestriction.R18
            )
        }
        if (key.isNotEmpty()) {
            val sh = searchNH(key, page, sortType)
            val total = sh?.total ?: 0
            return result(total, sh?.result?.map {
                toS(it)
            } ?: listOf())
        } else {
            val list = ArrayList<SearchItemData>()
            if (page == 1) {
                list.addAll(api<List<NHSearchLikeResultItem>>("galleries","popular").getOrNull()?.map { toS(it) } ?: listOf())
            }
            list.addAll(
                api<NHSearchLike>("galleries", params =  mapOf("page" to page))
                    .getOrNull()?.result
                    ?.map { toS(it) } ?: listOf()
            )
            return result(-1, list)
        }
    }

    suspend fun searchTags(query: String, type: String = "tag", limit: Int = 10): List<NHTagSearchItem> {
        val res = api<List<NHTagSearchItem>>("tags", "search", form = buildJsonObject {
            put("query", query)
            put("type", type)
            put("limit", limit)
        })
        return res.getOrNull() ?: listOf()
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