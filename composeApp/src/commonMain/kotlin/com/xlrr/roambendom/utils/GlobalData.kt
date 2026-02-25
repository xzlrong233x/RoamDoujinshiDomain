package com.xlrr.roambendom.utils

import androidx.compose.foundation.gestures.ScrollableState
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.funny.data_saver.core.DataSaverInterface
import com.xlrr.roambendom.config.ConfigUtil
import com.xlrr.roambendom.nav.Navigator
import com.xlrr.roambendom.nav.Routes
import com.xlrr.roambendom.ui.HomeSelection
import okio.Path.Companion.toPath

object GlobalData {
    val nav = Navigator(Routes.Root.Default)
    var homeContentSelection: HomeSelection? by mutableStateOf(null)
    var forListState: ScrollableState? by mutableStateOf(null)
    var rootSearchQuery: TextFieldState = TextFieldState()

    var hideStatusBar by mutableStateOf(false) // 是否不显示状态栏，对桌面端无效
    var rootSearchBarExpanded by mutableStateOf(false) // 保留，说不定什么时候就用上了

    var cacheDir = "image_cache".toPath()
        private set

    var dataDir = "data".toPath()
        private set

    var dataSaver: DataSaverInterface? = null

    fun init(cachePath: String = "",dataPath: String = "", dataSaverArg: DataSaverInterface) {
        if (cachePath.isNotEmpty()) {
            cacheDir = cachePath.toPath()
        }
        if (dataPath.isNotEmpty()) {
            dataDir = dataPath.toPath()
        }
        dataSaver = dataSaverArg
        ConfigUtil.init(dataSaverArg)
        PixivTokenUtil.reload()
    }
}