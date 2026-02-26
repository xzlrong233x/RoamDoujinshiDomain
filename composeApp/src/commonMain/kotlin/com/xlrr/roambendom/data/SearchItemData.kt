package com.xlrr.roambendom.data

import kotlinx.serialization.Serializable

@Serializable
data class SearchItemData(
    var id: String,
    var title: String,
    var pageCount: Int = -1,
    var lang: CLanguage,
    var thumb: String,
    var source: CSources,
    var restriction: CRestriction = CRestriction.R18,
    var ai: Boolean = false,
    var author: String = "",
    var time: Long = 0L
) {
    override fun equals(other: Any?): Boolean {
        if (other is SearchItemData) {
            return uid() == other.uid()
        }
        return super.equals(other)
    }

    override fun hashCode(): Int {
        var result = pageCount
        result = 31 * result + ai.hashCode()
        result = 31 * result + id.hashCode()
        result = 31 * result + title.hashCode()
        result = 31 * result + lang.hashCode()
        //result = 31 * result + thumb.hashCode()
        result = 31 * result + source.hashCode()
        result = 31 * result + restriction.hashCode()
        result = 31 * result + author.hashCode()
        result = 31 * result + time.hashCode()
        return result
    }

    fun uid() : String {
        return when (source) {
            CSources.NHENTAI -> "n$id"
            CSources.PIXIV -> "p$id"
        }
    }
}