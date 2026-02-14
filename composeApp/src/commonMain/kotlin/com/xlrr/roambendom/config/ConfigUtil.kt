package com.xlrr.roambendom.config

import androidx.compose.runtime.mutableStateOf
import com.funny.data_saver.core.DataSaverInterface
import com.funny.data_saver.core.mutableDataSaverStateOf

object ConfigUtil {
    val forceGrid = StateWithUI(mutableStateOf(false), UIType.SwitchUI(
        "强制使用网格布局"
    ))

    fun init(dataSaver: DataSaverInterface) {
        forceGrid.state = mutableDataSaverStateOf(dataSaver, "force_grid", false)
    }
}