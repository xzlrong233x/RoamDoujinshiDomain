package com.xlrr.roambendom.utils

object MthUtil {
    fun calWindow(page: Int, per: Int, size: Int, startIndex: Int = 0) : Pair<Int, Int> {
        return Pair(
            ((page-1) * per + startIndex).coerceIn(0, size),
            (page * per + startIndex).coerceIn(0, size)
        )
    }
}