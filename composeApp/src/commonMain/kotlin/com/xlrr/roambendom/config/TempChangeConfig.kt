package com.xlrr.roambendom.config

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

class TempChangeConfig<T>(
    defaultValue: T,
    uiType: UIType<T>
): StateWithUI<T>(mutableStateOf(defaultValue), uiType) {
    var realValue by mutableStateOf(defaultValue)

    fun applyChange() {
        if (value == realValue) return
        realValue = value
    }

    fun setAll(v: T) {
        realValue = v
        value = v
    }
}