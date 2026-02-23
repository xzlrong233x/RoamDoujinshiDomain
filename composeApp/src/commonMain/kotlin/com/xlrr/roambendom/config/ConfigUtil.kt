package com.xlrr.roambendom.config

import androidx.compose.runtime.mutableStateOf
import com.funny.data_saver.core.DataSaverInterface
import com.funny.data_saver.core.mutableDataSaverStateOf

object ConfigUtil {
    val forceGrid = StateWithUI(mutableStateOf(false), UIType.SwitchUI(
        "强制使用网格布局"
    ))

    val pixivLanguage = StateWithUI(mutableStateOf("ja"), UIType.DropStringSelectUI(
        "P站语言",
        listOf("zh","ja","en","kr")
    ))
    val pixivToken = StateWithUI(mutableStateOf(""), UIType.NoUI())

    fun init(dataSaver: DataSaverInterface) {
        forceGrid.state = mutableDataSaverStateOf(dataSaver, "force_grid", false)
        pixivLanguage.state = mutableDataSaverStateOf(dataSaver, "pixiv_language", "ja")
        pixivToken.state = mutableDataSaverStateOf(dataSaver, "pixiv_token", "")

    }
}