package com.xlrr.roambendom

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.tooling.preview.Preview
import com.funny.data_saver.core.DataSaverPreferences
import com.xlrr.roambendom.utils.GlobalData

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        setContent {
            BackHandler {
                GlobalData.nav.defaultBack()
            }
            LaunchedEffect(true) {
                GlobalData.init(cacheDir.path,
                    DataSaverPreferences(applicationContext, false))
            }
            App()
        }
    }
}

@Preview
@Composable
fun AppAndroidPreview() {
    App()
}