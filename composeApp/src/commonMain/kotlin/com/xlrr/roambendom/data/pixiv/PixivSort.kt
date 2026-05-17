package com.xlrr.roambendom.data.pixiv

import com.xlrr.roambendom.utils.camelToSnack

enum class PixivSort {
    DateDesc,
    DateAsc,
    PopularDesc; //这个字段使用频率应该不会太高

    override fun toString(): String {
        return super.toString().camelToSnack()
    }
}