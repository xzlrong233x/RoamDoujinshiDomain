package com.xlrr.roambendom.utils

import coil3.Uri
import io.ktor.http.Parameters
import io.ktor.http.URLBuilder
import io.ktor.http.set
import java.net.URL
import java.security.MessageDigest
import java.security.SecureRandom
import kotlin.io.encoding.Base64

expect fun encryptSP(input: String) : String

expect fun decryptSP(input: String) : String

object CryptUtil {
    val CodeToken = Base64.UrlSafe.withPadding(Base64.PaddingOption.ABSENT_OPTIONAL)

    fun generateCodeVerifier(): String {
        val secureRandom = SecureRandom()
        val code = ByteArray(43)
        secureRandom.nextBytes(code)
        return CodeToken.encode(code)
    }

    fun generateCodeChallenge(codeVerifier: String): String {
        val bytes = codeVerifier.toByteArray()
        val messageDigest = MessageDigest.getInstance("SHA-256")
        val digest = messageDigest.digest(bytes)
        return CodeToken.encode(digest)
    }
}