package com.xlrr.roambendom.data

data class SuggestionItem(
    val key: String,
    val extra: String = "",
    val addition: Boolean = false,
    val clickType: SuggestionClickType = SuggestionClickType.Default
)

sealed class SuggestionClickType {
    object Default: SuggestionClickType()
    data class Custom(val click: () -> Unit) : SuggestionClickType()
}