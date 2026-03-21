package com.xlrr.roambendom.config

import androidx.compose.runtime.MutableState

open class StateWithUI<T>(
    var state: MutableState<T>,
    val uiType: UIType<T>
) {
    var value: T
        get() = state.value
        set(value) {
            state.value = value
        }
}