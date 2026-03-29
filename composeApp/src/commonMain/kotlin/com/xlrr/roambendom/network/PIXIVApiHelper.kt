package com.xlrr.roambendom.network

import coil3.network.NetworkHeaders
import com.xlrr.roambendom.config.ConfigUtil
import com.xlrr.roambendom.data.*
import com.xlrr.roambendom.data.pixiv.*
import com.xlrr.roambendom.utils.PixivTokenUtil
import com.xlrr.roambendom.utils.getAsBoolean
import com.xlrr.roambendom.utils.getAsInt
import com.xlrr.roambendom.utils.getAsString
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.serialization.json.*
import kotlinx.serialization.serializer
import kotlin.time.Instant

data class UrlWithSize(
    val url: String,
    val w: Int,
    val h: Int,
    val oriUrl: String
) {
    companion object {
        val EMPTY = UrlWithSize("", 0, 0, "")

        fun parse(str: String) : UrlWithSize {
            return Regex("(.+?)\\[w(\\d+)h(\\d+)]\\{(.+?)}").find(str)?.let {
                UrlWithSize(
                    it.groups[1]?.value.toString(),
                    it.groups[2]?.value?.toIntOrNull() ?: 0,
                    it.groups[3]?.value?.toIntOrNull() ?: 0,
                    it.groups[4]?.value.toString()
                )
            } ?: EMPTY
        }
    }
}

object PIXIVApiHelper {
    val mainPrefix = "www.pixiv.net"
    val imgPrefix = "i.pximg.net"

    fun HttpRequestBuilder.pixivNormalSetting(useLang: Boolean = true) {
        defaultHeader()
        header("referer", "https://$mainPrefix")
        cookie("yuid_b", PixivTokenUtil.map.getOrDefault("a", ""))
        cookie("PHPSESSID", PixivTokenUtil.map.getOrDefault("b", ""))
        cookie("device_token", PixivTokenUtil.map.getOrDefault("c", ""))
        if (useLang) {
            parameter("lang", ConfigUtil.pixivLanguage.value)
        }
        userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/145.0.0.0 Safari/537.36 Edg/145.0.0.0")
    }

    val pixivCoilHeader = NetworkHeaders.Builder()
        .set("Referer", "https://$mainPrefix")
        .build()

    suspend inline fun <reified T> requestStandard(post: String, builder: HttpRequestBuilder.() -> Unit = {}) : NormalSealedData<T> {
        val url = post.split("/").filter { it.isNotEmpty() }
            .joinToString("/", prefix = "https://$mainPrefix/")
        var result = NormalSealedData<T>()
        try {
            val res = NetHelper.client.get(url) {
                pixivNormalSetting()
                builder()
            }
            result = res.body()
        } catch (e : Exception) {
            e.printStackTrace()
        }
        return result
    }

    suspend fun search(key: String, page: Int = 1,
                       mode: PixivSearchRestriction = PixivSearchRestriction.All) : SearchResult {
        val encoded = key.encodeURLQueryComponent()
        val data = requestStandard<JsonObject>("/ajax/search/artworks/${encoded}") {
            parameter("mode", mode.lowerStr())
            parameter("p", page)
            parameter("word", key) //我不知道p站的api为什么会有这个参数
            // TODO：实现更多参数
        }
        val sr = SearchResult(-1, listOf(), key, page)
        val change = arrayListOf<SearchItemData>()
        if (data.error) {
            return sr
        }
        val illustManga = data.body.jsonObject["illustManga"]?.jsonObject
        illustManga?.let { x ->
            x["data"]?.jsonArray?.map {
                it.jsonObject.let { n ->
                    if (n.getAsBoolean("isAdContainer")) return@let
                    change.add(
                        SearchItemData(
                            n.getAsString("id"),
                            CSources.PIXIV,
                            n.getAsString("title"),
                            n.getAsInt("pageCount"),
                            CLanguage.Unknown,
                            n.getAsString("url"),
                            CRestriction.entries[n.getAsInt("xRestrict")],
                            n.getAsInt("aiType") > 1,
                            "${n.getAsString("userName")}(${n.getAsString("userId")})",
                            Instant.parse(n.getAsString("updateDate")).toEpochMilliseconds()
                        )
                    )
                }
            }
            sr.total = x.getAsInt("total")
        }
        sr.items = change
        return sr
    }

    suspend fun artworkPage(id: String) : List<ArtworkPageItem> {
        val res = requestStandard<List<ArtworkPageItem>>("/ajax/illust/$id/pages")
        if (res.error) {
            return listOf()
        }
        return res.realData(serializer())
    }

    suspend fun ugoiraData(id: String) : UgoiraMetadata? {
        val r = requestStandard<UgoiraMetadata>("/ajax/illust/$id/ugoira_meta")
        return if (r.error) null else r.realData(serializer())
    }

    suspend fun artwork(id: String) : ArtworkInfo {
        val info = ArtworkInfo()
        info.source = CSources.PIXIV
        val bd = requestStandard<JsonObject>("/ajax/illust/$id") {
            pixivNormalSetting()
        }
        if (bd.error) {
            return info
        }
        val jo = bd.realData(JsonObject.serializer())
        info.title = jo.getAsString("illustTitle")
        info.tags = jo["tags"]?.jsonObject["tags"]?.jsonArray?.map { x ->
            x.jsonObject.getAsString("tag")
        } ?: listOf()
        info.authors = listOf(jo.getAsString("userName"),jo.getAsString("userId"))
        info.description = jo.getAsString("description")
            .split("<\\s*br\\s*/\\s*>".toRegex()).joinToString("\n") {
            NetHelper.handleHTMLString(it)
        }
        info.page = jo.getAsInt("pageCount")
        info.likeCount = jo.getAsInt("bookmarkCount")
        info.time = Instant.parse(jo.getAsString("uploadDate")).toEpochMilliseconds()
        info.ai = jo.getAsInt("aiType") > 1
        info.restriction = CRestriction.entries[jo.getAsInt("xRestrict")]
        val meta = ugoiraData(id)
        if (meta == null) {
            val page = artworkPage(id)
            info.pageUrls = page.map {
                "${it.urls.regular}[w${it.width}h${it.height}]{${it.urls.original}}"
            }
            info.thumbUrls = page.map {
                it.urls.thumbMini
            }
        } else {
            meta.width = jo.getAsInt("width")
            meta.height = jo.getAsInt("height")
            info.ugoiraMetadata = meta
            info.thumbUrls = listOf(
                jo["urls"]?.jsonObject?.getAsString("thumb") ?: ""
            )
        }
        return info
    }

    suspend fun keywordSuggestion(keyword: String) : List<KeywordSuggestionItem> {
        return NetHelper.client.get("https://$mainPrefix/rpc/cps.php") {
            pixivNormalSetting()
            parameter("keyword", keyword)
        }.body<JsonObject>()["candidates"]?.let {
             Json.decodeFromJsonElement(it)
        } ?: listOf()
    }

    suspend fun testPixivRequest(): PixivTestResult {
        val result = PixivTestResult()
        try {
            val website = forTest()
            val accountEx = NetHelper.client.get("https://www.pixiv.net/ajax/user/extra") {
                pixivNormalSetting()
            }
            val viewSetting = NetHelper.getWebDocument("https://www.pixiv.net/settings/viewing") {
                pixivNormalSetting()
            }
            result.canRequestWebsite = website.status == HttpStatusCode.OK
            if (result.canRequestWebsite) {
                result.isUserSigned = accountEx.body<JsonObject>()["error"]?.jsonPrimitive?.booleanOrNull == false
                if (result.isUserSigned && viewSetting != null) {
                    result.canReadSensitive = viewSetting.select("*[name=\"sensitive_view_setting\"]")
                        .hasAttr("checked")
                    result.canReadR18 = viewSetting.select("*[name=\"r18\"]")
                        .hasAttr("checked")
                    result.canReadR18G = viewSetting.select("*[name=\"r18g\"]")
                        .hasAttr("checked")
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return result
    }

    suspend fun forTest(): HttpResponse {
        val res = NetHelper.client.get("https://www.pixiv.net/illustration") {
            pixivNormalSetting()
        }
        return res
    }
}