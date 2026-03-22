package com.xlrr.roambendom

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import com.funny.data_saver.core.DataSaverProperties
import com.xlrr.roambendom.config.LocalPlatformForUI
import com.xlrr.roambendom.config.UIEnablePlatform
import com.xlrr.roambendom.utils.GlobalData
import io.github.vinceglb.filekit.FileKit

fun main() {
    FileKit.init("RoamBenDom")

    application {
        Window(
            onCloseRequest = ::exitApplication,
            title = "Roam Doujinshi Domain",
        ) {
            LaunchedEffect(true) {
                GlobalData.init(dataSaverArg = DataSaverProperties("config.properties"))
            }
            CompositionLocalProvider(
                LocalPlatformForUI provides UIEnablePlatform.DESKTOP
            ) {
                App()
            }
        }
    }
}