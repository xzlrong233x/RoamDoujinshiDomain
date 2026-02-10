package com.xlrr.roambendom.config

import androidx.compose.runtime.Composable

sealed class UIType<T> {
    data class TextFieldWithBtnUI(
        val fieldLabel: String,
        val btnLabel: String,
        var onValueChange: (String) -> Unit = {},
        val supportText: @Composable (String, Boolean, Boolean) -> Unit = { txt, result, show -> },
        val clickEvent: suspend (String) -> Boolean = { txt -> false}
    ) : UIType<String>()
    data class SwitchUI(
        val label: String,
        val onValueChange: (Boolean) -> Unit = {new -> }
    ): UIType<Boolean>()
    data class SingleSegmentedButton(
        val label: String,
        val choiceList: List<String>
    ): UIType<Int>()
    class NoUI<T> : UIType<T>()
}