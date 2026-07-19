package com.xlrr.roambendom.config

import androidx.compose.runtime.mutableStateOf
import com.funny.data_saver.core.DataSaverInterface
import com.funny.data_saver.core.ITypeConverter
import com.funny.data_saver.core.mutableDataSaverStateOf
import com.xlrr.roambendom.utils.StringOrResource
import com.xlrr.roambendom.utils.decryptSP
import com.xlrr.roambendom.utils.encryptSP
import roambendom.composeapp.generated.resources.Res
import roambendom.composeapp.generated.resources.Res.string
import roambendom.composeapp.generated.resources.disabled_history
import roambendom.composeapp.generated.resources.enable_volume_turn
import roambendom.composeapp.generated.resources.force_grid
import roambendom.composeapp.generated.resources.multithread_load
import roambendom.composeapp.generated.resources.pixiv_language
import roambendom.composeapp.generated.resources.progressive_load

object ConfigUtil {
    val forceGrid = StateWithUI(mutableStateOf(false), UIType.SwitchUI(
        StringOrResource.new(string.force_grid)
    ))

    val pixivLanguage = StateWithUI(mutableStateOf("ja"), UIType.DropStringSelectUI(
        StringOrResource.new(string.pixiv_language),
        listOf("zh","ja","en","kr")
    ))
    val pixivToken = StateWithUI(mutableStateOf(""), UIType.NoUI())
    val pixivRToken = StateWithUI(mutableStateOf(""), UIType.NoUI())
    val useMultithread = StateWithUI(mutableStateOf(false), UIType.SwitchUI(
        StringOrResource.new(string.multithread_load)
    ))
    val streamDisplay = StateWithUI(mutableStateOf(true), UIType.SwitchUI(
        StringOrResource.new(string.progressive_load)
    ))
    val disableHistoryRecord = StateWithUI(mutableStateOf(false), UIType.SwitchUI(
        StringOrResource.new(string.disabled_history)
    ))
    val enableVolumeTurn = StateWithUI(mutableStateOf(false), UIType.SwitchUI(
        StringOrResource.new(string.enable_volume_turn)
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