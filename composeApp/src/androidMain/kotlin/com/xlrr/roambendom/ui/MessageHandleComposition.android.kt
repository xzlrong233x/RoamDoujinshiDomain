package com.xlrr.roambendom.ui

import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import com.xlrr.roambendom.manager.MessageManager

@Composable
actual fun MessageHandleComposition(content: @Composable (() -> Unit)) {
    val context = LocalContext.current
    val entries = MessageManager.entries
    val latest = entries.firstOrNull()
    // getComposeOrString() 是 @Composable，必须在 composition 里取出，不能在 LaunchedEffect 内调用。
    val latestTitle = latest?.data?.title?.getComposeOrString() ?: ""
    LaunchedEffect(latest?.id) {
        if (latest != null) {
            Toast.makeText(context, latestTitle, Toast.LENGTH_LONG).show()
            MessageManager.remove(latest.id)
        }
    }
    content()
}
