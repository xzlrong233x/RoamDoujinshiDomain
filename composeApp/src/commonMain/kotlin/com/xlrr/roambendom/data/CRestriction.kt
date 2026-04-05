package com.xlrr.roambendom.data

import androidx.compose.ui.graphics.Color
import com.xlrr.roambendom.data.CRestriction.*
import kotlinx.serialization.Serializable

@Serializable
enum class CRestriction {
    Normal,
    R18,
    R18G;

    companion object {
        val nhNonHTag = listOf(23225, 137289, 154609)
    }
}

fun CRestriction.getColor(): Color {
    return when (this) {
        Normal -> Color(0xFFCECE33)
        R18 -> Color.Red
        R18G -> Color(0xFF8B0000)
    }
}