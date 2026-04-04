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
    val useMultithread = StateWithUI(mutableStateOf(false), UIType.SwitchUI(
        "使用多线程加载图片"
    ))
    val streamDisplay = StateWithUI(mutableStateOf(false), UIType.SwitchUI(
        "使用流式显示"
    ))
    val disableHistoryRecord = StateWithUI(mutableStateOf(false), UIType.SwitchUI(
        "禁用历史记录"
    ))
    val enableVolumeTurn = StateWithUI(mutableStateOf(false), UIType.SwitchUI(
        "启用音量键翻页"
    ).setPlatform(UIEnablePlatform.ANDROID))

    fun init(dataSaver: DataSaverInterface) {
        forceGrid.state = mutableDataSaverStateOf(dataSaver, "force_grid", false)
        pixivLanguage.state = mutableDataSaverStateOf(dataSaver, "pixiv_language", "ja")
        pixivToken.state = mutableDataSaverStateOf(dataSaver, "pixiv_token", "")
        useMultithread.state = mutableDataSaverStateOf(dataSaver, "use_multithread", false)
        streamDisplay.state = mutableDataSaverStateOf(dataSaver, "stream_display", false)
        disableHistoryRecord.state = mutableDataSaverStateOf(dataSaver, "disable_history_record", false)
        enableVolumeTurn.state = mutableDataSaverStateOf(dataSaver, "enable_volume_turn", false)
    }
}