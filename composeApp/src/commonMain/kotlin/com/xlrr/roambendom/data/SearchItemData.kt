package com.xlrr.roambendom.data

data class SearchItemData(
    var id: String,
    var title: String,
    var pageCount: Int = -1,
    var lang: CLanguage,
    var thumb: String,
    var source: CSources,
    var restriction: CRestriction = CRestriction.R18,
    var ai: Boolean = false
) {
    override fun equals(other: Any?): Boolean {
        if (other is SearchItemData) {
            return id == other.id
        }
        return super.equals(other)
    }

    override fun hashCode(): Int {
        var result = pageCount
        result = 31 * result + id.hashCode()
        result = 31 * result + title.hashCode()
        result = 31 * result + lang.hashCode()
        result = 31 * result + thumb.hashCode()
        result = 31 * result + source.hashCode()
        result = 31 * result + restriction.hashCode()
        return result
    }
}