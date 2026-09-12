package com.xlrr.roambendom.ui

import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import com.xlrr.roambendom.manager.MessageManager

@Composable
actual fun MessageHandleComposition(content: @Composable (() -> Unit)) {
    val map = MessageManager.get()
    val context = LocalContext.current
    val titles = map.keys.map { it.title.getComposeOrString() }
    LaunchedEffect(map) {
        if (map.isNotEmpty()) {
            val m = map.entries.last()
            Toast.makeText(context, titles.last(), m.value.toEpochMilliseconds().toInt()).show()
            MessageManager.delectMessage(m.key)
        }
    }
    content()
}