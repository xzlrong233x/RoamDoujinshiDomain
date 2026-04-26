package com.xlrr.roambendom.network

import coil3.PlatformContext
import coil3.network.httpHeaders
import coil3.request.ImageRequest
import coil3.size.Size
import com.fleeksoft.ksoup.Ksoup
import com.fleeksoft.ksoup.nodes.Document
import io.ktor.client.*
import io.ktor.client.engine.okhttp.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json
import java.security.KeyStore
import javax.net.ssl.SSLSocketFactory
import javax.net.ssl.TrustManager
import javax.net.ssl.TrustManagerFactory
import javax.net.ssl.X509TrustManager

fun HttpRequestBuilder.defaultHeader() {
    header("user-agent","Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/144.0.0.0 Safari/537.36 Edg/144.0.0.0")
}

fun defaultImageRequest(url: String, context: PlatformContext, originalSize: Boolean = true) : ImageRequest {
    return Regex("https://[it]\\d.nhentai.net")
        .replace(url, "").let {
            ImageRequest.Builder(context)
                .diskCacheKey(it)
                .memoryCacheKey(it)
                .data(url)
                .run {
                    if (originalSize) size(Size.ORIGINAL) else this
                }
                .run {
                    if (url.contains("pximg") || url.contains("pixiv")) {
                        httpHeaders(PIXIVApiHelper.pixivCoilHeader)
                    }
                    else this
                }
                .build()
        }
}

object NetHelper {
    fun getTrustManagers(): Array<out TrustManager?>? {
        val trustManagerFactory = TrustManagerFactory.getInstance(
            TrustManagerFactory.getDefaultAlgorithm())
        trustManagerFactory.init(null as KeyStore?)
        val mgs = trustManagerFactory.trustManagers
        return mgs
    }

    private fun createClient(): HttpClient = HttpClient(OkHttp) {
        install(ContentNegotiation) {
            json(Json {
                encodeDefaults = true
                isLenient = true
                allowSpecialFloatingPointValues = true
                allowStructuredMapKeys = true
                prettyPrint = false
                useArrayPolymorphism = false
                ignoreUnknownKeys = true
            })
        }
        engine {
            config {
                val ssls = RBDSocketFactory(SSLSocketFactory.getDefault() as SSLSocketFactory)
                val mgs = getTrustManagers()
                if (mgs?.isNotEmpty() == true && mgs[0] is X509TrustManager) {
                    sslSocketFactory(
                        ssls ,
                        mgs[0] as X509TrustManager
                    )
                }
                dns(RBDDns)
            }
        }
    }

    val client = createClient()

    suspend fun getWebDocument(url: String, block : HttpRequestBuilder.() -> Unit = {}) : Document? {
        val res = client.get(url, block)
        if (res.status != HttpStatusCode.OK) {
            return null
        }
        return Ksoup.parse(res.bodyAsText(), url)
    }


    fun handleHTMLString(str: String) : String {
        return Ksoup.parseBodyFragment("<p>$str</p>").body().child(0).text()
    }
}