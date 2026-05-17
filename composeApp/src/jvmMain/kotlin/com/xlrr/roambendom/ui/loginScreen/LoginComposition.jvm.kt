package com.xlrr.roambendom.ui.loginScreen

import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import kotlinx.coroutines.CoroutineScope

@Composable
actual fun AskConfirmButton(
    viewModel: LoginViewModel,
    dismiss: () -> Unit,
    cs: CoroutineScope
) {
    TextButton({}, enabled = false) {
        Text("暂不支持桌面端")
    }
}