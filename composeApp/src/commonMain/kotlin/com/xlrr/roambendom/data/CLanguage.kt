package com.xlrr.roambendom.data

enum class CLanguage {
    Chinese,
    English,
    Japanese,
    Unknown;

    companion object {
        fun languageDetect(code: String) : CLanguage {
            if ("29963" in code) return Chinese
            else if ("6346" in code) return Japanese
            else if ("12227" in code) return English
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

