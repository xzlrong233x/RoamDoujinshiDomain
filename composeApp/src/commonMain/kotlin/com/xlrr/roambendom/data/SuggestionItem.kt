package com.xlrr.roambendom.data

import androidx.compose.ui.text.TextRange

data class SuggestionItem(
    val key: String,
    val extra: String = "",
    val trailing: String = "",
    val addition: Boolean = false,
    val clickType: SuggestionClickType = SuggestionClickType.Default
)

sealed class SuggestionClickType {
    object Default: SuggestionClickType()
    data class Replacement(
        val text: String,
        val start: Int,
        val end: Int,
        val afterSelection: TextRange? = null
    ): SuggestionClickType()
    data class Custom(val click: () -> Unit) : SuggestionClickType()
}