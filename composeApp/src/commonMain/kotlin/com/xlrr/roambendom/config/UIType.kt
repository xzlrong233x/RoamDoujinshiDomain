package com.xlrr.roambendom.config

import androidx.compose.runtime.Composable

sealed class UIType<T> {
    data class SwitchUI(
        val label: String,
        val onValueChange: (Boolean) -> Unit = {new -> }
    ): UIType<Boolean>()
    data class SingleSegmentedButton(
        val label: String,
        val choiceList: List<String>
    ): UIType<Int>()
    data class DropStringSelectUI<T>(
        val label: String,
        val choiceList: List<T>,
        val strFunc: (T) -> String = {it.toString()}
    ): UIType<T>()
    class NoUI<T> : UIType<T>()
}