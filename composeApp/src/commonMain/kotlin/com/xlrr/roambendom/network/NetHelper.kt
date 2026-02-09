package com.xlrr.roambendom.network

import com.fleeksoft.ksoup.Ksoup
import com.fleeksoft.ksoup.nodes.Document
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import java.security.KeyStore
import javax.net.ssl.SSLSocketFactory
import javax.net.ssl.TrustManager
import javax.net.ssl.TrustManagerFactory
import javax.net.ssl.X509TrustManager

object NetHelper {
    fun getTrustManagers(): Array<out TrustManager?>? {
        val trustManagerFactory = TrustManagerFactory.getInstance(
            TrustManagerFactory.getDefaultAlgorithm())
        trustManagerFactory.init(null as KeyStore?)
        val mgs = trustManagerFactory.trustManagers
        return mgs
    }

    private fun createClient(): HttpClient = HttpClient(OkHttp) {
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
}