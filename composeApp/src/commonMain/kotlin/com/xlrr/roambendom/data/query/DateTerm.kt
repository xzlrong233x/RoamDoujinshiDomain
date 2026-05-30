package com.xlrr.roambendom.data.query

import kotlinx.datetime.DateTimeUnit

class DateTerm(
    v: Int,
    val unit: DateTimeUnit,
    exclude: Boolean,
    operator: TermOperator
) : OpTerm(
    "uploaded",
    v,
    operator,
    exclude
) {
    override fun serializeKeyValue(): String {
        return super.serializeKeyValue() + timeUnit()
    }

    fun timeUnit() = when (unit) {
        DateTimeUnit.DAY -> "d"
        DateTimeUnit.WEEK -> "w"
        DateTimeUnit.MONTH -> "m"
        DateTimeUnit.YEAR -> "y"
        else -> "h"
    }
}