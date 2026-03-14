package com.xlrr.roambendom.config

import androidx.compose.runtime.MutableState

data class StateWithUI<T>(
    var state: MutableState<T>,
    val uiType: UIType<T>
) {
    var value: T
        get() = state.value
        set(value) {
            state.value = value
        }
}