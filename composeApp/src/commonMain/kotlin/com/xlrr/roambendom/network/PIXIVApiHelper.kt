package com.xlrr.roambendom.network

import coil3.network.NetworkHeaders
import com.xlrr.roambendom.config.ConfigUtil
import com.xlrr.roambendom.data.*
import com.xlrr.roambendom.data.pixiv.*
import com.xlrr.roambendom.data.pixiv.search.ArtworkDetailData
import com.xlrr.roambendom.utils.PixivTokenUtil
import com.xlrr.roambendom.utils.PixivTokenUtil.pixivToken
import com.xlrr.roambendom.utils.decryptSP
import com.xlrr.roambendom.utils.getAsInt
import com.xlrr.roambendom.utils.getAsString
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.request.forms.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.*
import kotlinx.serialization.serializer
import kotlin.time.Clock
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant

object PIXIVApiHelper {
    val mainPrefix = "www.pixiv.net"
    val imgPrefix = "i.pximg.net"
    val appApiPrefix = "app-api.pixiv.net"
    val redirectUrl = "https://app-api.pixiv.net/web/v1/users/auth/pixiv/callback"
    val authToken = "https://oauth.secure.pixiv.net/auth/token"

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

    fun HttpRequestBuilder.pixivOSHeader() {
        header("app-os-version", "15.6")
        header("app-os", "ios")
        header("user-agent", "PixivIOSApp/7.13.3 (iOS 14.6; iPhone13,2)")
        pixivToken()
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

    suspend fun apiRequest(
        post: String,
        parameters: Parameters = Parameters.Empty,
        useGet: Boolean = false,
        builder: HttpRequestBuilder.() -> Unit = {}
    ) : Result<JsonObject> {
        PixivTokenUtil.verifyToken(decryptSP(ConfigUtil.pixivRToken.value))
        val url = post.split("/").filter { it.isNotEmpty() }
            .joinToString("/", prefix = "https://$appApiPrefix/")
        try {
            val res = NetHelper.client.submitForm(url,parameters, useGet) {
                builder()
                pixivOSHeader()
            }
            val bd = res.body<JsonObject>()
            if (bd.contains("error")) return Result.failure(Exception(res.bodyAsText()))
            return Result.success(bd)
        } catch (e: Exception) {
            return Result.failure(e)
        }
    }

    fun joToPixivSearchItem(n: JsonObject) : SearchItemData {
        return SearchItemData(
            n.getAsString("id"),
            CSources.PIXIV,
            n.getAsString("title"),
            n.getAsInt("page_count"),
            CLanguage.Unknown,
            n["image_urls"]?.jsonObject?.getAsString("square_medium") ?: "",
            CRestriction.entries[n.getAsInt("x_restrict")],
            n.getAsInt("illust_ai_type") > 1,
            n["user"]?.jsonObject?.let { n ->
                "${n.getAsString("name")}(${n.getAsString("id")})"
            } ?: "",
            Instant.parse(n.getAsString("create_date")).toEpochMilliseconds()
        )
    }

    suspend fun searchIllust(
        key: String,
        page: Int = 0, // offset
        searchTarget: PixivSearchTarget = PixivSearchTarget.PartialMatchForTags,
        searchSort: PixivSort = PixivSort.DateDesc,
        searchDuration: PixivSearchDuration? = null,
    ) : SearchResult {
        val data = apiRequest("/v1/search/illust", Parameters.build {
            append("search_target", searchTarget.toString())
            append("offset", page.toString())
            append("word", key)
            append("sort", searchSort.toString())
            append("filter", "for_ios")
            if (searchDuration != null) append("duration", searchDuration.toString())
        }, true) {
        }
        val sr = SearchResult(-1, listOf(), key, page)
        val change = arrayListOf<SearchItemData>()
        if (data.isFailure) {
            return sr
        }
        val illustManga = data.getOrNull()
        illustManga?.get("illusts")?.let { x ->
            NetHelper.json.decodeFromJsonElement(
                ListSerializer<ArtworkDetailData>(serializer()), x
            ).forEach {
                change.add(
                    SearchItemData(
                        it.id.toString(),
                        CSources.PIXIV,
                        it.title,
                        it.pageCount,
                        CLanguage.Unknown,
                        it.imageUrls.squareMedium,
                        CRestriction.entries[it.xRestrict],
                        it.illustAIType > 1,
                        "${it.user.name}(${it.user.id})",
                        Instant.parse(it.createDate).toEpochMilliseconds()
                    )
                )
            }
//            x.jsonArray.map {
//                it.jsonObject.let { n ->
//                    change.add(
//                        joToPixivSearchItem(n)
//                    )
//                }
//            }
        }
        illustManga?.let {
            it.getAsString("next_url")
        }
        sr.items = change
        return sr
    }

    //TODO_NOTE: page访问，软件api没有pages。
    suspend fun artworkPage(id: String) : List<ArtworkPageItem> {
        val res = requestStandard<List<ArtworkPageItem>>("/ajax/illust/$id/pages")
        if (res.error) {
            return listOf()
        }
        return res.realData(serializer())
    }

    //TODO_NOTE：ugoira信息的获取，通过软件api获取的与UgoiraMetadata定义的不一样，例如说软件api返回的数据里没originalSrc，
    // 不过根据考查，规律不难发现，以下是例子：[
    // src -> "https://i.pximg.net/img-zip-ugoira/img/2020/09/07/23/29/31/84226963_ugoira600x600.zip"
    // ori -> "https://i.pximg.net/img-zip-ugoira/img/2020/09/07/23/29/31/84226963_ugoira1920x1080.zip"
    // ], [
    // src -> "https://i.pximg.net/img-zip-ugoira/img/2026/05/03/17/20/09/144305714_ugoira600x600.zip"
    // originalSrc -> "https://i.pximg.net/img-zip-ugoira/img/2026/05/03/17/20/09/144305714_ugoira1920x1080.zip"
    // ]
    suspend fun ugoiraData(id: String) : UgoiraMetadata? {
        val r = requestStandard<UgoiraMetadata>("/ajax/illust/$id/ugoira_meta")
        return if (r.error) null else r.realData(serializer())
    }

    //TODO_NOTE：获取一个artwork的详细信息，值得注意的是软件api不再提供每张图片的长宽，这影响了一些实现，见PixivDetailComposition
    suspend fun artwork(id: String) : ArtworkInfo {
        val info = ArtworkInfo()
        info.source = CSources.PIXIV
        val bd = requestStandard<JsonObject>("/ajax/illust/$id") {
            pixivNormalSetting()
        }
        if (bd.error) {
            throw Exception("artwork info get error info: ${
                bd.message.ifEmpty { "\nmessage is empty, maybe the artwork is disappeared" } //TODO: Localization
            }")
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
                "${it.urls.regular}[w${it.width}h${it.height}]{${it.urls.original}}" //TODO_NOTE：这个地方。
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

    // TODO_NOTE：网页api中与user work info配合使用，但软件api里会直接返回作品列表，这个变化会涉及RoutesUtil下pushAuthorSearch访问方式的改变
    suspend fun userAllWorks(id: String) : UserAllWorkData {
        val std = requestStandard<JsonObject>("/ajax/user/${id}/profile/all") {
            pixivNormalSetting()
        }
        if (std.error)
            return UserAllWorkData(listOf(),listOf(),listOf())
        return UserAllWorkData(
            std.body.jsonObject.getOrDefault("illusts", JsonElement).let {
                if (it is JsonObject) it.map { t -> t.key.toInt() }
                else listOf()
            },
            std.body.jsonObject.getOrDefault("manga", JsonElement).let {
                if (it is JsonObject) it.map { t -> t.key.toInt() }
                else listOf()
            },
            std.body.jsonObject.getOrDefault("novels", JsonElement).let {
                if (it is JsonObject) it.map { t -> t.key.toInt() }
                else listOf()
            }
        )
    }

    suspend fun userWorkInfo(ids: List<Int>, userId: String, ty: String = "illustManga"): List<SearchItemData> {
        if (ids.isEmpty()) return listOf()
        val std = requestStandard<JsonObject>("/ajax/user/${userId}/profile/illusts") {
            pixivNormalSetting()
            ids.forEach { parameter("ids[]", it) }
            parameter("work_category", ty)
            parameter("is_first_page", 0)
        }
        return std.body.jsonObject["works"]?.jsonObject?.map { joToPixivSearchItem(it.value.jsonObject) } ?: listOf()
    }

    //TODO_NOTE：会与recommendWork混合，但软件api中是直接返回信息列表，同时这里对应的是api的related字段，type是给novel留的坑，但软件api是
    //          都分离的，可以删掉这个形参，专门就访问illust，同时由于返回内容的改变，PIXIVDetailModel下的recommendModel也是要改的，
    //          值得注意的是related返回的next_url是用viewed[]来确定的，虽然很难评，但还是可以通过Pair来向searchFunction提供viewed来实现的
    private suspend fun recommendWorkInfo(ids: List<String>, type: String = "illust"): List<SearchItemData> {
        if (ids.isEmpty()) return listOf()
        val std = requestStandard<JsonObject>("/ajax/${type}/recommend/${type}s") {
            pixivNormalSetting()
            ids.forEach { parameter("${if (type == "illust") "illust_ids" else "novelIds"}[]", it) }
        }
        return std.body.jsonObject["${type}s"]?.jsonArray?.mapNotNull {
            try {
                return@mapNotNull joToPixivSearchItem(it.jsonObject)
            } catch (_: Exception) {
            }
            null
        } ?: listOf()
    }

    suspend fun recommendArtworkInfo(ids: List<String>) = recommendWorkInfo(ids, "illust")

    private suspend fun recommendWork(id: String, type: String, limit: Int = 9): RecommendData {
        val std = requestStandard<JsonObject>("/ajax/${type}/${id}/recommend/init") {
            pixivNormalSetting()
            parameter("limit", limit)
        }
        val recommend = RecommendData()
        if (std.error) return recommend
        std.body.jsonObject["details"]?.let {
            if (it is JsonArray && it.isEmpty()) return@let
            recommend.details = NetHelper.json.decodeFromJsonElement(it)
        }
        std.body.jsonObject["novels"]?.let {
            recommend.novels = it.jsonArray.map {m -> joToPixivSearchItem(m.jsonObject) }
        }
        std.body.jsonObject["illusts"]?.let {
            recommend.illusts = it.jsonArray.map {m -> joToPixivSearchItem(m.jsonObject) }
        }
        std.body.jsonObject["nextIds"]?.let {
            recommend.nextIds = it.jsonArray.map {m -> m.jsonPrimitive.content }
        }
        return recommend
    }

    suspend fun recommendArtwork(id: String, limit: Int = 18) = recommendWork(id, "illust", limit)

    suspend fun requestTokenWithCode(code: String, codeVerifier: String) : Pair<String, String> {
        val res = NetHelper.client.submitForm(authToken, Parameters.build {
            append("client_id", PixivTokenUtil.CLIENT_ID)
            append("client_secret", PixivTokenUtil.CLIENT_SECRET)
            append("code", code)
            append("code_verifier", codeVerifier)
            append("grant_type", "authorization_code")
            append("include_policy", "true")
            append("redirect_uri", redirectUrl)
        }) {
            pixivOSHeader()
        }
        try {
            res.body<JsonObject>().let {
                return Pair(it.getAsString("access_token"),it.getAsString("refresh_token"))
            }
        } catch (e: Exception) {
            return Pair("","")
        }
    }

    suspend fun requestTokenWithToken(refToken: String): Pair<String, Instant> {
        val res = NetHelper.client.submitForm(authToken, Parameters.build {
            append("client_id", PixivTokenUtil.CLIENT_ID)
            append("client_secret", PixivTokenUtil.CLIENT_SECRET)
            append("grant_type", "refresh_token")
            append("refresh_token", refToken)
        }) {
            pixivOSHeader()
        }
        try {
            res.body<JsonObject>().let {
                return Pair(it.getAsString("access_token"), Clock.System.now() + it.getAsInt("expires_in").seconds)
            }
        } catch (e: Exception) {
            return Pair("", Clock.System.now())
        }
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