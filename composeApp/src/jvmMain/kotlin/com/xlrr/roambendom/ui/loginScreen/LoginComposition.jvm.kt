package com.xlrr.roambendom.ui.loginScreen

import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import com.xlrr.roambendom.LocalStringResStorage
import com.xlrr.roambendom.nav.Routes
import com.xlrr.roambendom.utils.GlobalData
import com.xlrr.roambendom.utils.PixivTokenUtil
import kotlinx.coroutines.CoroutineScope
import org.jetbrains.compose.resources.LocalResourceReader
import org.jetbrains.compose.resources.stringResource
import java.awt.Desktop
import java.net.URI

@Composable
actual fun AskConfirmButton(
    viewModel: LoginViewModel,
    dismiss: () -> Unit,
    cs: CoroutineScope
) {
    TextButton({
        val n = Desktop.getDesktop()
        if (!n.isSupported(Desktop.Action.BROWSE)) return@TextButton
        PixivTokenUtil.genCodeChallenge()
        n.browse(URI(PixivTokenUtil.genPixivLoginUrl()))
        GlobalData.nav.push(Routes.Auth.Wait)
    }) {
        LocalStringResStorage.current["confirm"]?.let{ Text(stringResource(it)) }
    }
}