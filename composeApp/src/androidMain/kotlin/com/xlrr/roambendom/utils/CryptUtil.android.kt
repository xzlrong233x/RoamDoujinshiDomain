package com.xlrr.roambendom.utils

import javax.crypto.Cipher
import javax.crypto.spec.SecretKeySpec
import kotlin.io.encoding.Base64
import kotlin.random.Random

actual fun encryptSP(input: String): String { // TODO: 用上KeyGenerated
    val cipher = Cipher.getInstance("AES")
    var password = ByteArray(24) { Random.nextInt(21, 127).toByte() }.toString(Charsets.UTF_8)

    val keySpec = SecretKeySpec(password.toByteArray(),"AES")
    cipher.init(Cipher.ENCRYPT_MODE,keySpec)
    val encrypt = cipher.doFinal(input.toByteArray())

    val result = Base64.encode(encrypt)
    password = " " + Base64.encode(password.toByteArray())
    return StringBuilder().apply {
        var n = result.length % 32
        var p = 9
        result.forEachIndexed { i, c ->
            if (i == n && password.isNotEmpty()) {
                password.first().let { s ->
                    if (!s.isWhitespace()) {
                        append(s)
                    }
                    p = (if (!s.isWhitespace()) s else c).code % (p + 1) + 1
                    n += p
                }
                password = password.drop(1)
                if (n >= result.length) {
                    append(password)
                }
            }
            append(c)
        }
    }.toString()
}

actual fun decryptSP(input: String): String {
    try {
        val sb = StringBuilder()
        val bp = StringBuilder().apply {
            var n = input.length % 32
            var p = 9
            var f = false
            input.forEachIndexed { i, c ->
                if (i == n && length < 32) {
                    p = c.code % (p + 1) + 1
                    if (n != input.length % 32) {
                        append(c)
                        n += 1
                    } else sb.append(c)
                    if (p >= input.length - i - 33 + length) f = true
                    n += if (f) 0 else p
                } else {
                    sb.append(c)
                }
            }
        }.toString()

        val cipher = Cipher.getInstance("AES")
        val keySpec = SecretKeySpec(Base64.decode(bp),"AES")
        cipher.init(Cipher.DECRYPT_MODE,keySpec)

        val result = cipher.doFinal(Base64.decode(sb))
        return String(result, Charsets.UTF_8)
    } catch (e: Exception) {
        e.printStackTrace()
        return ""
    }
}