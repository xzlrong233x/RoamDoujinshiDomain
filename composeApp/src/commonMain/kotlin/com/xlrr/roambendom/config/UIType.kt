package com.xlrr.roambendom.config

import com.xlrr.roambendom.utils.StringOrResource

sealed class UIType<T>(
    enablePlatform: UIEnablePlatform = UIEnablePlatform.ALL
) {
    var enabledPlatform = enablePlatform
        private set
    data class SwitchUI(
        val label: StringOrResource,
        val onValueChange: (Boolean) -> Unit = {new -> }
    ): UIType<Boolean>()
    data class SingleSegmentedButton(
        val label: StringOrResource,
        val choiceList: List<StringOrResource>
    ): UIType<Int>()
    data class DropStringSelectUI<T>(
        val label: StringOrResource,
        val choiceList: List<T>,
        val strFunc: (T) -> String = {it.toString()}
    ): UIType<T>()
    class NoUI<T> : UIType<T>()
    fun setPlatform(platform: UIEnablePlatform): UIType<T> {
        this.enabledPlatform = platform
        return this
    }
}