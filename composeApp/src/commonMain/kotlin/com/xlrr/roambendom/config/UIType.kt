package com.xlrr.roambendom.config

sealed class UIType<T>(
    enablePlatform: UIEnablePlatform = UIEnablePlatform.ALL
) {
    var enabledPlatform = enablePlatform
        private set
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
    fun setPlatform(platform: UIEnablePlatform): UIType<T> {
        this.enabledPlatform = platform
        return this
    }
}