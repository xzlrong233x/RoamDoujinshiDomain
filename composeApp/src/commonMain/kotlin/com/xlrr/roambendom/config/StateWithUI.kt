package com.xlrr.roambendom.config

import androidx.compose.runtime.MutableState

data class StateWithUI<T>(
    val state: MutableState<T>,
    val uiType: UIType<T>
)