package com.xlrr.roambendom.utils

import com.xlrr.roambendom.config.ConfigUtil

object PixivTokenUtil {
    val map: HashMap<String, String> = hashMapOf()
    fun reload() {
        ConfigUtil.pixivToken.state.value.let {
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
        ConfigUtil.pixivToken.state.value = encryptSP(listOf(a,b,c).joinToString("|"))
        reload()
    }
}