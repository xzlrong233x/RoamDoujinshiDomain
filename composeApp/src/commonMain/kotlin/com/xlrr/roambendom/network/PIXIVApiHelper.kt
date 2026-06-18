package com.xlrr.roambendom.network

import coil3.network.NetworkHeaders
import com.xlrr.roambendom.config.ConfigUtil
import com.xlrr.roambendom.data.*
import com.xlrr.roambendom.data.pixiv.*
import com.xlrr.roambendom.data.pixiv.search.ArtworkDetailData
import com.xlrr.roambendom.data.pixiv.search.ArtworkDetailData.Companion.toSearchItem
import com.xlrr.roambendom.data.pixiv.search.PixivSearchDuration
import com.xlrr.roambendom.data.pixiv.search.PixivSearchTarget
import com.xlrr.roambendom.data.pixiv.search.PixivSort
import com.xlrr.roambendom.utils.PixivTokenUtil
import com.xlrr.roambendom.utils.PixivTokenUtil.pixivToken
import com.xlrr.roambendom.utils.TimeUtil
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
import okhttp3.HttpUrl.Companion.toHttpUrl
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
        if (ConfigUtil.pixivRToken.value.isNotEmpty())
            PixivTokenUtil.verifyToken(ConfigUtil.pixivRToken.value)
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
        val detail = NetHelper.json.decodeFromJsonElement<ArtworkDetailData>(n)
        return detail.toSearchItem()
    }

    suspend fun searchIllust(
        key: String,
        page: Int = 0, // offset
        searchTarget: PixivSearchTarget = PixivSearchTarget.PartialMatchForTags,
        searchSort: PixivSort = PixivSort.DateDesc,
        searchDuration: PixivSearchDuration? = null,
        timeRange: Pair<Long,Long>? = null,
        aiType: Int = 1
    ) : SearchResult {
        val data = apiRequest("/v1/search/illust", Parameters.build {
            append("search_target", searchTarget.toString())
            append("offset", page.toString())
            append("word", key)
            append("sort", searchSort.toString())
            append("filter", "for_ios")
            if (searchDuration != null) append("duration", searchDuration.toString())
            if (timeRange != null) {
                append("start_date", TimeUtil.formatTime(timeRange.first, TimeUtil.PIXIV_PARAM_FORMATTER))
                append("end_date", TimeUtil.formatTime(timeRange.second, TimeUtil.PIXIV_PARAM_FORMATTER))
            }
            append("search_ai_type", aiType.toString())
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
                        it.imageUrls.medium,
                        it.width,
                        it.height,
                        CRestriction.entries[it.xRestrict],
                        it.illustAIType > 1,
                        "${it.user.name}(${it.user.id})",
                        Instant.parse(it.createDate).toEpochMilliseconds(),
                        it.type == "ugoira"
                    )
                )
            }
        }
        illustManga?.let {
            "offset=(\\d+)".toRegex().find(it.getAsString("next_url")).let { x->
                if (x == null) {
                    sr.page += 120
                    return@let
                }
                sr.page = x.groupValues.last().toInt()
            }
        }
        sr.items = change
        return sr
    }

    // Deprecated: App API 的 illust/detail 已在 meta_pages 中直接返回页面信息，不再需要单独请求
    @Deprecated("Use artwork() which fetches detail and pages in one App API call")
    suspend fun artworkPage(id: String) : List<ArtworkPageItem> {
        val res = requestStandard<List<ArtworkPageItem>>("/ajax/illust/$id/pages")
        if (res.error) {
            return listOf()
        }
        return res.realData(serializer())
    }

    // 使用 App API /v1/ugoira/metadata 获取 ugoira 信息
    // App API 的 zip_urls 只有 medium，originalSrc 通过 URL 替换推导
    suspend fun ugoiraData(id: String) : UgoiraMetadata? {
        val r = apiRequest("/v1/ugoira/metadata", Parameters.build {
            append("illust_id", id)
        }, true)
        if (r.isFailure) return null
        val body = r.getOrNull() ?: return null
        val meta = body["ugoira_metadata"]?.jsonObject ?: return null
        val zipUrls = meta["zip_urls"]?.jsonObject
        val src = zipUrls?.getAsString("medium") ?: return null
        // 推导 originalSrc: _ugoira600x600 -> _ugoira1920x1080
        val originalSrc = src.replace("_ugoira600x600", "_ugoira1920x1080")
        val frames = meta["frames"]?.jsonArray?.map {
            val f = it.jsonObject
            UgoiraFrameItem(
                f.getAsString("file"),
                f.getAsInt("delay")
            )
        } ?: listOf()
        return UgoiraMetadata(
            frames = frames,
            mimeType = "image/jpeg",
            originalSrc = originalSrc,
            src = src
        )
    }

    // 使用 App API /v1/illust/detail 获取作品详情
    // App API 不再提供单张图片的宽高，meta_pages 中只有 image_urls（有 large/original 但无宽高）
    suspend fun artwork(id: String) : ArtworkInfo {
        val info = ArtworkInfo()
        info.source = CSources.PIXIV
        val r = apiRequest("/v1/illust/detail", Parameters.build {
            append("illust_id", id)
        }, true)
        if (r.isFailure) {
            throw Exception("artwork info get error info: ${r.exceptionOrNull()?.message
                ?: "\nmessage is empty, maybe the artwork is disappeared"}")
        }
        val body = r.getOrNull() ?: throw Exception("artwork info get error: empty response")
        val detail = NetHelper.json.decodeFromJsonElement(
            ArtworkDetailData.serializer(), body["illust"]!!
        )

        info.title = detail.title
        info.tags = detail.tags.map { it.name }
        info.authors = listOf(detail.user.name, detail.user.id.toString())
        info.description = detail.caption
            .split("<\\s*br\\s*/\\s*>".toRegex()).joinToString("\n") {
            NetHelper.handleHTMLString(it)
        }
        info.page = detail.pageCount
        info.likeCount = detail.totalBookmarks
        info.time = Instant.parse(detail.createDate).toEpochMilliseconds()
        info.ai = detail.illustAIType > 1
        info.isAnimation = detail.type == "ugoira"
        info.restriction = CRestriction.entries[detail.xRestrict]

        val illustWidth = detail.width
        val illustHeight = detail.height

        if (detail.type == "ugoira") {
            val meta = ugoiraData(id)
            if (meta != null) {
                meta.width = illustWidth
                meta.height = illustHeight
                info.ugoiraMetadata = meta
                info.thumbUrls = listOf(detail.imageUrls.medium)
            }
        } else {
            if (detail.metaPages.isNotEmpty()) {
                info.pageUrls = detail.metaPages.map { mp ->
                    // App API 无单图宽高，用作品整体宽高作为默认
                    "${mp.imageUrls.large}[w${illustWidth}h${illustHeight}]{${mp.imageUrls.original ?: ""}}"
                }
                info.thumbUrls = detail.metaPages.map { it.imageUrls.medium }
            } else {
                // 单页作品: meta_single_page
                val original = detail.metaSinglePage.originalImageURL
                val large = detail.imageUrls.large.ifEmpty { original }
                if (large.isNotEmpty()) {
                    info.pageUrls = listOf("$large[w${illustWidth}h${illustHeight}]{$original}")
                }
                info.thumbUrls = listOf(detail.imageUrls.medium)
            }
        }
        return info
    }

    // NOTE：不动
    suspend fun keywordSuggestion(keyword: String) : List<KeywordSuggestionItem> {
        return NetHelper.client.get("https://$mainPrefix/rpc/cps.php") {
            pixivNormalSetting()
            parameter("keyword", keyword)
        }.body<JsonObject>()["candidates"]?.let {
             Json.decodeFromJsonElement(it)
        } ?: listOf()
    }

    // 使用 App API /v1/user/illusts 直接获取用户作品列表
    // 替代旧的 userAllWorks + userWorkInfo 两步流程
    suspend fun userIllusts(
        userId: String,
        type: String = "",
        offset: Int = 0
    ): SearchResult {
        val pms = Parameters.build {
            append("user_id", userId)
            append("filter", "for_ios")
            if (type.isNotEmpty()) append("type", type)
            append("offset", offset.toString())
        }
        val r = apiRequest("/v1/user/illusts", pms, true)
        val body = r.getOrNull() ?: return SearchResult(
            -1, listOf(), userId, -1
        )
        val illusts: List<ArtworkDetailData> = body["illusts"]?.runCatching {
            NetHelper.json.decodeFromJsonElement(
                ListSerializer<ArtworkDetailData>(serializer()),
                this
            )
        }?.getOrNull() ?: listOf()
        val nextUrl = body.getAsString("next_url")
        return SearchResult(
            -1,
            illusts.map { it.toSearchItem() },
            userId,
            "offset=(\\d+)".toRegex().find(nextUrl)?.groupValues?.last()?.toInt() ?: -1
        )
    }

    fun nextUrlSearchFunction(
        requestFunction: suspend (key: String, offset: Int, viewed: List<String>)
            -> Pair<List<SearchItemData>, String?>,
    ) : suspend (key: String, page: Int, extra: HashMap<String, Any>) -> SearchResult = {k, p, e ->
        val nextUrl = e["next_url"] as? String
        var view: List<String> = listOf()
        var off = p
        if (nextUrl != null) {
            if (nextUrl.isEmpty()) {
                SearchResult(
                    -1, listOf(), k, p+1
                )
            }
            val url = nextUrl.toHttpUrl()
            off = url.queryParameter("offset")?.toInt() ?: 0
            view = url.queryParameterNames.mapNotNull {
                if (it.matches("viewed\\[\\d+]".toRegex())) {
                    url.queryParameter(it)
                } else null
            }
        }
        val (its , next) = requestFunction(
            k, off, view
        )
        if (next != null)
            e["next_url"] = next
        else if (e["next_url"] != null) {
            e["next_url"] = ""
        }
        SearchResult(
            -1,
            its,
            k,
            off+1
        )
    }

    private fun noOffsetByViewedPair(r: Result<JsonObject>): Pair<List<SearchItemData>, String?> {
        val body = r.getOrNull() ?: return Pair(listOf(), null)
        val illusts = body["illusts"]?.jsonArray?.mapNotNull {
            try {
                joToPixivSearchItem(it.jsonObject)
            } catch (_: Exception) { null }
        } ?: listOf()
        val nextUrl = body.getAsString("next_url").ifEmpty { null }
        return Pair(illusts, nextUrl)
    }

    suspend fun illustRelated(
        illustId: String,
        offset: Int = 0,
        viewed: List<String> = listOf(),
        seedIllustIds: List<String> = listOf()
    ): Pair<List<SearchItemData>, String?> {
        val pms = Parameters.build {
            append("illust_id", illustId)
            append("filter", "for_ios")
            append("offset", offset.toString())
            seedIllustIds.forEach { append("seed_illust_ids[]", it) }
            viewed.forEach { append("viewed[]", it) }
        }
        val r = apiRequest("/v2/illust/related", pms, true)
        return noOffsetByViewedPair(r)
    }

    suspend fun illustRecommend(
        offset: Int = 0,
        viewed: List<String> = listOf()
    ): Pair<List<SearchItemData>, String?> {
        val pms = Parameters.build {
            append("filter", "for_ios")
            append("content_type", "")
            append("offset", offset.toString())
            viewed.forEach { append("viewed[]", it) }
        }
        val r = apiRequest("v1/illust/recommended", pms, true)
        return noOffsetByViewedPair(r)
    }

    @Deprecated("Use userIllusts() which fetches full illust list in one App API call")
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

    @Deprecated("Use userIllusts() which fetches full illust list in one App API call")
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

    @Deprecated("Use illustRelated() which fetches related illusts in one App API call")
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

    @Deprecated("Use illustRelated() which fetches related illusts in one App API call")
    suspend fun recommendArtworkInfo(ids: List<String>) = recommendWorkInfo(ids, "illust")

    @Deprecated("Use illustRelated() which fetches related illusts in one App API call")
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

    @Deprecated("Use illustRelated() which fetches related illusts in one App API call")
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

    // NOTE：保留
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
                result.isUserSigned = apiRequest("/v1/illust/detail", Parameters.build {
                    append("illust_id", "34844544")
                }, true).isSuccess
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