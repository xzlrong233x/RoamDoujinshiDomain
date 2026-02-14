package com.xlrr.roambendom.data

data class PixivTestResult(
    var canRequestWebsite: Boolean = false,
    var isUserSigned: Boolean = false,
    var canReadSensitive: Boolean = false,
    var canReadR18: Boolean = false,
    var canReadR18G: Boolean = false,
)
