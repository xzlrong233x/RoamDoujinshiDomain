package com.xlrr.roambendom

import android.os.Bundle
import android.view.WindowInsets
import android.view.WindowInsetsController
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.funny.data_saver.core.DataSaverPreferences
import com.xlrr.roambendom.config.LocalPlatformForUI
import com.xlrr.roambendom.config.UIEnablePlatform
import com.xlrr.roambendom.utils.GlobalData

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        val wic = window.decorView.windowInsetsController
        installSplashScreen()

        setContent {
            BackHandler {
                GlobalData.nav.defaultBack()
            }
            LaunchedEffect(true) {
                GlobalData.init(cacheDir.path, dataDir.path,
                    DataSaverPreferences(applicationContext, false))
            }
            CompositionLocalProvider(LocalPlatformForUI provides UIEnablePlatform.ANDROID) {
                App()
            }
            val sh = GlobalData.hideStatusBar.collectAsState().value
            if (sh) {
                wic?.systemBarsBehavior = WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                wic?.hide(WindowInsets.Type.statusBars())
            } else {
                wic?.systemBarsBehavior = WindowInsetsController.BEHAVIOR_DEFAULT
                wic?.show(WindowInsets.Type.statusBars())
            }
        }
    }
}

@Preview
@Composable
fun AppAndroidPreview() {
    App()
}