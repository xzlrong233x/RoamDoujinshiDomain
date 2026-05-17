package com.xlrr.roambendom

import android.app.ComponentCaller
import android.content.Intent
import android.os.Build
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
import androidx.core.util.Consumer
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.funny.data_saver.core.DataSaverPreferences
import com.xlrr.roambendom.config.LocalPlatformForUI
import com.xlrr.roambendom.config.UIEnablePlatform
import com.xlrr.roambendom.nav.Routes
import com.xlrr.roambendom.utils.GlobalData

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        val wic = window.decorView.windowInsetsController
        installSplashScreen()
        println("Activity create")

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
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    wic?.systemBarsBehavior = WindowInsetsController.BEHAVIOR_DEFAULT
                }
                wic?.show(WindowInsets.Type.statusBars())
            }
        }
        addLifeCycleIntentListener()
    }

    private fun addLifeCycleIntentListener() {
        val intentConsumer = Consumer<Intent> { handleIntent(it) }
        lifecycle.addObserver(object : DefaultLifecycleObserver {

            override fun onCreate(owner: LifecycleOwner) {
                addOnNewIntentListener(intentConsumer)
            }

            override fun onDestroy(owner: LifecycleOwner) {
                removeOnNewIntentListener(intentConsumer)
                lifecycle.removeObserver(this)
            }
        })
    }

    override fun onNewIntent(intent: Intent, caller: ComponentCaller) {
        println("new intent")
        super.onNewIntent(intent, caller)
        println(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent) {
        println(intent)
        val cur = GlobalData.nav.backStack.last() as? Routes.Auth.Choose ?: return
        val dt = intent.data ?: return
        val authCode = dt.getQueryParameter("code")
        if (authCode != null) cur.callback?.invoke(authCode, cur.called)
    }
}

@Preview
@Composable
fun AppAndroidPreview() {
    App()
}