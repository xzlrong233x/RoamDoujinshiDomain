package com.xlrr.roambendom

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import com.xlrr.roambendom.utils.GlobalData

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "RoamBenDom",
    ) {
        LaunchedEffect(true) {
            GlobalData.init()
        }
        App()
    }
}