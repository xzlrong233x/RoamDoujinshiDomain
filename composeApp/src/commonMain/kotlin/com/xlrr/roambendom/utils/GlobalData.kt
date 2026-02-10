package com.xlrr.roambendom.utils

import androidx.compose.foundation.gestures.ScrollableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.xlrr.roambendom.nav.Navigator
import com.xlrr.roambendom.nav.Routes
import com.xlrr.roambendom.ui.HomeSelection
import okio.Path.Companion.toPath

object GlobalData {
    val nav = Navigator(Routes.Root.Default)
    var homeContentSelection: HomeSelection? by mutableStateOf(null)
    var forListState: ScrollableState? by mutableStateOf(null)

    var cacheDir = "image_cache".toPath()
        private set

    fun init(cachePath: String = "") {
        if (cachePath.isNotEmpty()) {
            cacheDir = cachePath.toPath()
        }
    }
}