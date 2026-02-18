package com.xlrr.roambendom

import com.xlrr.roambendom.network.PIXIVApiHelper
import io.ktor.client.request.*
import io.ktor.client.statement.*
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.JsonElement
import kotlin.test.Test

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
        val sr = PIXIVApiHelper.search("悪堕ち")
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
}