package com.xlrr.roambendom

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowState
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.funny.data_saver.core.DataSaverProperties
import com.xlrr.roambendom.config.AppVisibility
import com.xlrr.roambendom.config.LocalPlatformForUI
import com.xlrr.roambendom.config.UIEnablePlatform
import com.xlrr.roambendom.utils.GlobalData
import io.github.vinceglb.filekit.FileKit

@Composable
private fun SyncAppVisibility(windowState: WindowState) {
    // WindowState.isMinimized 在 Compose Multiplatform 1.11 里由 mutableStateOf 承载，
    // 且 SwingWindow 注册的 WindowStateListener 会把真实窗口状态写回该属性。
    // 放在 LaunchedEffect 里同步，避免在 composition 阶段写状态，也不扩大整棵 UI 的重组范围。
    LaunchedEffect(windowState.isMinimized) {
        AppVisibility.visible = !windowState.isMinimized
    }
}

fun main() {
    FileKit.init("RoamBenDom")
    application {
        val windowState = rememberWindowState()
        Window(
            onCloseRequest = ::exitApplication,
            state = windowState,
            title = "Roam Doujinshi Domain",
        ) {
            SyncAppVisibility(windowState)
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