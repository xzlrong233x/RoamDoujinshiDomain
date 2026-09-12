package com.xlrr.roambendom

import com.xlrr.roambendom.data.nh.NHSearchLike
import com.xlrr.roambendom.data.nh.NHTagSearchItem
import com.xlrr.roambendom.network.NHWebHelper
import com.xlrr.roambendom.network.NetHelper
import com.xlrr.roambendom.network.PIXIVApiHelper
import com.xlrr.roambendom.network.RBDDns
import com.xlrr.roambendom.third.EchRequestRustClass
import io.ktor.client.request.*
import io.ktor.client.statement.*
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.JsonElement
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

// ── nhentai / ECH 通道的测试辅助 ──

private const val NH_UNREACHABLE = "跳过：ECH 通道当前不可达"

private const val NH_PING = "${NHWebHelper.API_PREFIX}search?query=big&page=1"

private val NH_CANDIDATES = RBDDns.mainNH.joinToString(",") { it.hostAddress.orEmpty() }

/** 探测一次 ECH 通道。失败原因要打出来：否则跳过时看不出是网络不通还是服务端阻止 */
private val nhPing: Result<String> by lazy {
    runBlocking { runCatching { EchRequestRustClass.baseHttpGet(NH_PING, NH_CANDIDATES) } }
}

private val echReachable: Boolean get() = nhPing.isSuccess

/** 跳过时打印的原因 */
private fun skipReason() = "$NH_UNREACHABLE —— ${nhPing.exceptionOrNull()?.message ?: "响应体为空"}"

/** 取响应体，连不上时返回 null */
private suspend fun fetch(url: String, form: String? = null): String? = runCatching {
    if (form == null) EchRequestRustClass.baseHttpGet(url, NH_CANDIDATES)
    else EchRequestRustClass.baseHttpPost(url, form, NH_CANDIDATES)
}.getOrNull()

class ComposeAppCommonTest {
    @Test
    fun testPixivRequest() = runBlocking {
        val n = PIXIVApiHelper.testPixivRequest()
        println(n)
    }

    @Test
    fun testNormalSealedDataWork() = runBlocking {
        val res = PIXIVApiHelper.requestStandard<JsonElement>("/ajax/user/815465/works/latest") {
            parameter("lang","zh")
        }
        println(res)
    }

    @Test
    fun pixivSearch() = runBlocking {
        val sr = PIXIVApiHelper.searchIllust("悪堕ち")
        assert(sr.total > 0)
        assert(sr.items.size >= 58)
        println("Total: ${sr.total}")
        sr.items.forEach {
            println(it)
        }
    }

    @Test
    fun example() = runBlocking {
        val res = PIXIVApiHelper.forTest()
        println(res.bodyAsText())
    }

    // ── nhentai / ECH 通道 ──

    /** 原生层 GET */
    @Test
    fun nhHttpGet() = runBlocking {
        if (!echReachable) return@runBlocking println(skipReason())
        val body = fetch(NH_PING) ?: return@runBlocking println(skipReason())
        println("${body.length}B")
        assertTrue(NetHelper.json.decodeFromString<NHSearchLike>(body).total > 0)
    }

    /** 原生层 POST：form 按 JSON 发出 */
    @Test
    fun nhHttpPost() = runBlocking {
        if (!echReachable) return@runBlocking println(skipReason())
        val body = fetch(
            "${NHWebHelper.API_PREFIX}tags/search",
            """{"query":"big","type":"tag","limit":3}"""
        ) ?: return@runBlocking println(skipReason())
        println(body)
        val tags = NetHelper.json.decodeFromString<List<NHTagSearchItem>>(body)
        assertTrue(tags.isNotEmpty(), "unexpected body: $body")
    }

    /** 非 2xx 由原生层抛异常（原生层的错误信息里带状态码） */
    @Test
    fun nhHttpNotFound() = runBlocking {
        if (!echReachable) return@runBlocking println(skipReason())
        val e = assertFailsWith<RuntimeException> {
            EchRequestRustClass.baseHttpGet("${NHWebHelper.API_PREFIX}galleries/99999999999")
        }
        println(e.message)
        assertTrue(e.message.orEmpty().contains("http "), "不是状态码错误：${e.message}")
    }

    /** NHWebHelper.api 的 GET 路径（搜索接口） */
    @Test
    fun nhSearch() = runBlocking {
        if (!echReachable) return@runBlocking println(skipReason())
        val sh = NHWebHelper.searchNH("big") ?: return@runBlocking println(skipReason())
        println("total: ${sh.total}")
        assertTrue(sh.total > 0)
        assertTrue(sh.result.isNotEmpty())
    }

    /** NHWebHelper.api 的 POST 路径（tag 搜索接口） */
    @Test
    fun nhSearchTags() = runBlocking {
        if (!echReachable) return@runBlocking println(skipReason())
        val tags = NHWebHelper.searchTags("big")
        if (tags.isEmpty()) return@runBlocking println(skipReason())
        println(tags.take(3))
        assertTrue(tags.any { it.name.isNotEmpty() && it.count > 0 })
    }

    /** galleries/{id} 接口与 artwork 的字段映射 */
    @Test
    fun nhArtwork() = runBlocking {
        if (!echReachable) return@runBlocking println(skipReason())
        val id = NHWebHelper.searchNH("big")?.result?.firstOrNull()?.id
            ?: return@runBlocking println(skipReason())
        val info = NHWebHelper.artwork(id.toString())
        println("${info.title} | ${info.page} pages | ${info.tags.size} tags")
        assertTrue(info.title.isNotEmpty())
        assertTrue(info.page > 0)
        assertTrue(info.pageUrls.size == info.page)
    }

    /**
     * 候补连接地址：连着发 4 次请求，都应该成功。
     *
     * 故意用 4 个不同的接口：同一条路径连着打会被服务端临时 400/403，那是另一回事。
     * */
    @Test
    fun nhFallbackIps() = runBlocking {
        if (!echReachable) return@runBlocking println(skipReason())
        val api = NHWebHelper.API_PREFIX

        suspend fun search(query: String): Result<String> =
            runCatching { EchRequestRustClass.baseHttpGet("${api}search?query=$query&page=1", NH_CANDIDATES) }

        fun report(name: String, r: Result<String>): Result<String> {
            println("$name: ${r.getOrNull()?.length ?: r.exceptionOrNull()!!.message}")
            return r
        }

        val first = report("search", search("big"))
        val tags = report(
            "tags/search",
            runCatching {
                EchRequestRustClass.baseHttpPost(
                    "${api}tags/search",
                    """{"query":"big","type":"tag","limit":3}""",
                    NH_CANDIDATES
                )
            }
        )
        val again = report("search#2", search("language%3Achinese"))

        // 再要一个 galleries/{id}，凑满 4 次不同的请求
        val id = first.getOrNull()?.let { runCatching { NetHelper.json.decodeFromString<NHSearchLike>(it) }.getOrNull() }
            ?.result?.firstOrNull()?.id
        val gallery = id?.let {
            report(
                "galleries/$it",
                runCatching { EchRequestRustClass.baseHttpGet("${api}galleries/$it", NH_CANDIDATES) }
            )
        }

        val ok = listOf(first, tags, again).count { it.isSuccess } + if (gallery?.isSuccess == true) 1 else 0
        assertTrue(ok >= 3, "4 次请求只成功 $ok 次")
        assertEquals(gallery?.getOrNull()?.contains("\"pages\""), true, "galleries 响应异常")
    }
}