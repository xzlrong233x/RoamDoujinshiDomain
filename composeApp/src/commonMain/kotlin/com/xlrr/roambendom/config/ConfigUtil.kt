package com.xlrr.roambendom.config

import androidx.compose.runtime.mutableStateOf
import com.funny.data_saver.core.DataSaverInterface
import com.funny.data_saver.core.ITypeConverter
import com.funny.data_saver.core.mutableDataSaverStateOf
import com.xlrr.roambendom.utils.decryptSP
import com.xlrr.roambendom.utils.encryptSP

object ConfigUtil {
    val forceGrid = StateWithUI(mutableStateOf(false), UIType.SwitchUI(
        "强制使用网格布局"
    ))

    val pixivLanguage = StateWithUI(mutableStateOf("ja"), UIType.DropStringSelectUI(
        "P站语言",
        listOf("zh","ja","en","kr")
    ))
    val pixivToken = StateWithUI(mutableStateOf(""), UIType.NoUI())
    val pixivRToken = StateWithUI(mutableStateOf(""), UIType.NoUI())
    val useMultithread = StateWithUI(mutableStateOf(false), UIType.SwitchUI(
        "使用多线程加载图片"
    ))
    val streamDisplay = StateWithUI(mutableStateOf(true), UIType.SwitchUI(
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
        pixivRToken.state = mutableDataSaverStateOf(dataSaver, "pixiv_ref_token", "",
            typeConverter = object : ITypeConverter {
                override fun save(data: Any?): String {
                    return encryptSP(data.toString())
                }

                override fun restore(str: String): Any? {
                    return decryptSP(str)
                }

                override fun accept(data: Any?): Boolean {
                    return true
                }

            }
        )
        useMultithread.state = mutableDataSaverStateOf(dataSaver, "use_multithread", false)
        streamDisplay.state = mutableDataSaverStateOf(dataSaver, "stream_display", true)
        disableHistoryRecord.state = mutableDataSaverStateOf(dataSaver, "disable_history_record", false)
        enableVolumeTurn.state = mutableDataSaverStateOf(dataSaver, "enable_volume_turn", false)
    }
}