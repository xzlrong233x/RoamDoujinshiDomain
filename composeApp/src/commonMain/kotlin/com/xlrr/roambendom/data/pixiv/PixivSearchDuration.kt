package com.xlrr.roambendom.data.pixiv

import com.xlrr.roambendom.utils.camelToSnack

enum class PixivSearchDuration {
    WithinLastDay,
    WithinLastWeek,
    WithinLastMonth;

    override fun toString(): String {
        return super.toString().camelToSnack()
    }
}