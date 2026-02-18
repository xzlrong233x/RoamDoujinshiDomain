package com.xlrr.roambendom.utils

import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonPrimitive

fun JsonObject.getAsString(k: String) : String {
    return this[k]?.jsonPrimitive?.content ?: ""
}

fun JsonObject.getAsInt(k: String, default: Int = 0) : Int {
    return this[k]?.jsonPrimitive?.intOrNull ?: default
}