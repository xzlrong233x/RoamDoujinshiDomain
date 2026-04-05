package com.xlrr.roambendom.data.nh

enum class NHSearchSortType {
    DATE,
    POPULAR,
    POPULAR_TODAY,
    POPULAR_WEEK,
    POPULAR_MONTH;

    override fun toString(): String {
        return super.toString().lowercase()
    }
}