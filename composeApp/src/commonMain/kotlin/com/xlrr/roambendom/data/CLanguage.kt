package com.xlrr.roambendom.data

import kotlinx.serialization.Serializable

@Serializable
enum class CLanguage {
    Chinese,
    English,
    Japanese,
    Unknown;

    companion object {
        fun languageDetect(code: String) : CLanguage {
            if ("lang-cn" in code) return Chinese
            else if ("lang-jp" in code) return Japanese
            else if ("lang-gb" in code) return English
            return Unknown
        }

        fun convert(lang: String): CLanguage {
            return when(lang.trim().lowercase()) {
                "chinese" -> Chinese
                "english" -> English
                "japanese" -> Japanese
                else -> Unknown
            }
        }
    }
}

