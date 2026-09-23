package com.xlrr.roambendom.utils

import com.xlrr.roambendom.config.ConfigUtil
import com.xlrr.roambendom.network.PIXIVApiHelper
import io.ktor.client.request.*
import io.ktor.http.*
import kotlin.time.Clock
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant

object PixivTokenUtil {
    val map: HashMap<String, String> = hashMapOf()
    fun reload() {
        ConfigUtil.pixivToken.value.let {
            if (it.isNotEmpty()) {
                val s = decryptSP(it)
                s.split("|").let { l ->
                    map["a"] = l.getOrNull(0) ?: ""
                    map["b"] = l.getOrNull(1) ?: ""
                    map["c"] = l.getOrNull(2) ?: ""
                }
            }
        }
    }

    fun setToken(a: String, b: String, c: String) {
        ConfigUtil.pixivToken.value = encryptSP(listOf(a,b,c).joinToString("|"))
        reload()
    }

    private var codeVerifier: String = ""
    private var codeChallenge: String =""
    const val CLIENT_ID = "MOBrBDS8blbauoSck0ZfDbtuzpyT"
    const val CLIENT_SECRET = "lsACyCD94FhDUtGTXi3QzcFE2uU1hqtDaKeqrdwj"
    var accessToken = ""
        private set
    private var passTime: Instant = Clock.System.now()

    fun genCodeChallenge() : String {
        codeVerifier = CryptUtil.generateCodeVerifier()
        codeChallenge = CryptUtil.generateCodeChallenge(codeVerifier)
        return codeChallenge
    }

    suspend fun handleCode(code: String) {
        if (codeVerifier.isEmpty() || codeChallenge.isEmpty()) return
        val r = PIXIVApiHelper.requestTokenWithCode(code, codeVerifier)
        accessToken = r.first
        passTime = Clock.System.now() + 3600.seconds
        ConfigUtil.pixivRToken.value = r.second
        codeVerifier = ""
        codeChallenge = ""
    }

    suspend fun verifyToken(rToken: String) : Boolean {
        if (accessToken == "" || Clock.System.now() >= passTime) {
            val r = PIXIVApiHelper.requestTokenWithToken(rToken)
            if (r.first.isEmpty()) {
                return false
            }
            accessToken = r.first
            passTime = r.second
        }
        return true
    }

    fun HttpRequestBuilder.pixivToken() : Boolean {
        if (accessToken == "" || Clock.System.now() >= passTime) {
            return false
        }
        header("Authorization", "Bearer $accessToken")
        return true
    }

    fun genPixivLoginUrl() : String {
        return URLBuilder(URLProtocol.HTTPS, "app-api.pixiv.net", pathSegments = listOf("web","v1","login"), parameters = Parameters.build {
            append("code_challenge", codeChallenge)
            append("code_challenge_method", "S256")
            append("client", "pixiv-android")
        }).buildString()
    }
}