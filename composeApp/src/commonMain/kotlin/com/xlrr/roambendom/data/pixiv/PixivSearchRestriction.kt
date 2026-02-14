package com.xlrr.roambendom.data.pixiv

enum class PixivSearchRestriction {
    All,
    SAFE,
    R18;

}

fun PixivSearchRestriction.lowerStr(): String {
    return this.toString().lowercase()
}