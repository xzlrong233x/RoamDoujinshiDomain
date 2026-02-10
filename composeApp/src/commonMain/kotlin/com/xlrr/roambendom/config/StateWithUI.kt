package com.xlrr.roambendom.config

import androidx.compose.runtime.MutableState

data class StateWithUI<T>(
    val value: MutableState<T>,
    val uiType: UIType<T>
)