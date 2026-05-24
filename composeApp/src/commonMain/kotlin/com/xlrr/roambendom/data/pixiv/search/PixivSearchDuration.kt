package com.xlrr.roambendom.data.pixiv.search

import com.xlrr.roambendom.utils.camelToSnack

enum class PixivSearchDuration {
    WithinLastDay,
    WithinLastWeek,
    WithinLastMonth;

    override fun toString(): String {
        return super.toString().camelToSnack()
    }
}