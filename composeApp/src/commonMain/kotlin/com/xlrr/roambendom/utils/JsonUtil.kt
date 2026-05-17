package com.xlrr.roambendom.utils

import androidx.compose.ui.util.fastJoinToString
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonPrimitive

fun JsonObject.getAsString(k: String) : String {
    return this[k]?.jsonPrimitive?.content ?: ""
}

fun JsonObject.getAsInt(k: String, default: Int = 0) : Int {
    return this[k]?.jsonPrimitive?.intOrNull ?: default
}

fun JsonObject.getAsBoolean(k: String, default: Boolean = false) : Boolean {
    return this[k]?.jsonPrimitive?.booleanOrNull ?: default
}

fun String.camelToSnack(): String = replace("([a-z])([A-Z])".toRegex()) {matchResult ->
    if (matchResult.groupValues.size > 2) {
        "${matchResult.groupValues[1]}_${matchResult.groupValues[2].lowercase()}"
    } else {
        matchResult.value
    }
}.lowercase()