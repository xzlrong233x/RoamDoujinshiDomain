package com.xlrr.roambendom.network

import com.xlrr.roambendom.data.*
import com.xlrr.roambendom.data.pixiv.NormalSealedData
import com.xlrr.roambendom.data.pixiv.PixivSearchRestriction
import com.xlrr.roambendom.data.pixiv.lowerStr
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.serialization.json.*


object PIXIVApiHelper {
    val mainPrefix = "www.pixiv.net"
    val imgPrefix = "i.pximg.net"

    fun HttpRequestBuilder.pixivNormalSetting() {
        defaultHeader()
        header("referer", "https://$mainPrefix")
        cookie("yuid_b","") // TODO：见设置 吧
        cookie("PHPSESSID", "")
        cookie("device_token", "")
    }

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
            parameter("word", encoded) //我不知道p站的api为什么会有这个参数
            // TODO：实现更多参数
        }
        val sr = SearchResult(-1, listOf(), key, page)
        val change = arrayListOf<SearchItemData>()
        if (data.error) {
            return sr
        }
        data.body.jsonObject["illustManga"]?.jsonObject?.get("data")?.jsonArray?.map {
            change.add(
                SearchItemData(
                    it.jsonObject["id"]?.jsonPrimitive.toString(),
                    it.jsonObject["title"]?.jsonPrimitive.toString(),
                    it.jsonObject["pageCount"]?.jsonPrimitive?.intOrNull ?: 0,
                    CLanguage.Unknown,
                    it.jsonObject["url"]?.jsonPrimitive.toString(),
                    CSources.PIXIV,
                    CRestriction.entries[it.jsonObject["xRestrict"]?.jsonPrimitive?.intOrNull ?: 0],
                    (it.jsonObject["aiType"]?.jsonPrimitive?.intOrNull ?: 0) > 1
                )
            )
        }
        sr.items = change
        return sr
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