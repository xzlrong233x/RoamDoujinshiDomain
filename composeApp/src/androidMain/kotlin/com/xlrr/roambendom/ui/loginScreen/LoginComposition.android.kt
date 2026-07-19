package com.xlrr.roambendom.ui.loginScreen

import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import com.xlrr.roambendom.LocalStringResStorage
import com.xlrr.roambendom.nav.Routes
import com.xlrr.roambendom.utils.GlobalData
import com.xlrr.roambendom.utils.PixivTokenUtil
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource

@Composable
actual fun AskConfirmButton(viewModel: LoginViewModel, dismiss: () -> Unit, cs: CoroutineScope) {
    val pt = LocalContext.current
    TextButton({
        dismiss()
        val cur = GlobalData.nav.backStack.last()
        if (cur is Routes.Auth.Choose) {
            cur.callback = { s, mb ->
                mb.value = true
                cs.launch {
                    PixivTokenUtil.handleCode(s)
                    GlobalData.nav.defaultBack()
                }
            }
            viewModel.launchBrowser(pt)
        }
    }) {
        LocalStringResStorage.current["i_know"]?.let{ Text(stringResource(it)) }
    }
}