package com.xlrr.roambendom.data.pixiv

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull

@Serializable
data class NormalSealedData<T>(
    var error: Boolean = true, var message: String = "", var body: JsonElement = JsonNull
) {
    fun realData(serializer: KSerializer<T>): T {
        return Json.decodeFromJsonElement(serializer, body)
    }
}