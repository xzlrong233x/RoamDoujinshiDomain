package com.xlrr.roambendom.model

class NormalRequestModel<T>(default: () -> T, val requestFunction: suspend () -> Unit) : RequestRefreshModel<T>(default) {
    override suspend fun reload() {
        super.reload()
        if (_loading) return
        error = null
        _loading = true
        try {
            requestFunction()
        } catch (e: Exception) {
            error = e
        }
        _loading = false
    }

    suspend fun reloadIfNull() {
        if (content == null && !_loading) {
            reload()
        }
    }
}