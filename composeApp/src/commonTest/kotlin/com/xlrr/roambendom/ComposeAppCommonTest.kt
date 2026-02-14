package com.xlrr.roambendom

import com.xlrr.roambendom.network.PIXIVApiHelper
import io.ktor.client.request.parameter
import io.ktor.client.statement.bodyAsText
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.JsonElement
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.test.Test
import kotlin.test.assertEquals

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
    fun example() = runBlocking {
        val res = PIXIVApiHelper.forTest()
        println(res.bodyAsText())
    }
}