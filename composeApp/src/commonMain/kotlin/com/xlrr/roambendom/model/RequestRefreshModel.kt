package com.xlrr.roambendom.model

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

open class RequestRefreshModel<T>(default: () -> T) {
    protected var _loading by mutableStateOf(false)
    protected var _refreshing by mutableStateOf(false)
    var error: Throwable? = null
        protected set

    var content: T = default()
        protected set

    val loading
        get() = _loading
    val refreshing
        get() = _refreshing

    open suspend fun reload() {}
    open suspend fun refresh() {}
}