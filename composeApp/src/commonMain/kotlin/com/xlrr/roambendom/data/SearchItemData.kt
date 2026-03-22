package com.xlrr.roambendom.data

import kotlinx.serialization.Serializable

@Serializable
data class SearchItemData(
    var id: String,
    var source: CSources,
    var title: String = "",
    var pageCount: Int = -1,
    var lang: CLanguage = CLanguage.Unknown,
    var thumb: String = "",
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

    fun isDefective(): Boolean {
        return title.isEmpty() || thumb.isEmpty()
    }

    fun fillSelf(art: ArtworkInfo): SearchItemData {
        val cp = this.copy()
        cp.title = art.title
        cp.thumb = cp.thumb.ifEmpty { art.thumbUrls.firstOrNull() ?: "" }
        cp.pageCount = art.page
        cp.lang = art.language
        cp.author = art.authors.firstOrNull() ?: ""
        cp.restriction = art.restriction
        cp.ai = art.ai
        return cp
    }

    fun fillSelfIfDefective(art: ArtworkInfo): SearchItemData {
        if (isDefective()) {
            return fillSelf(art)
        }
        return copy()
    }

    fun uid() : String {
        return when (source) {
            CSources.NHENTAI -> "n$id"
            CSources.PIXIV -> "p$id"
        }
    }
}