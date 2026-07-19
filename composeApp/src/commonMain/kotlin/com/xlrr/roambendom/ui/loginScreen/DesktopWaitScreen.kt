package com.xlrr.roambendom.ui.loginScreen

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.xlrr.roambendom.config.ConfigUtil
import com.xlrr.roambendom.network.PIXIVApiHelper
import com.xlrr.roambendom.utils.GlobalData
import com.xlrr.roambendom.utils.PixivTokenUtil
import com.xlrr.roambendom.utils.decryptSP
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import roambendom.composeapp.generated.resources.Res
import roambendom.composeapp.generated.resources.confirm
import roambendom.composeapp.generated.resources.desktop_login_info
import java.awt.TextField

@Composable
fun DesktopWaitScreen() {
    val txt = rememberTextFieldState()
    val ss = rememberCoroutineScope()
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column {
            Text(
                stringResource(Res.string.desktop_login_info),
                Modifier.widthIn(max = 524.dp))
            OutlinedTextField(txt, label = {Text("code")})
            TextButton({
                ss.launch {
                    PixivTokenUtil.handleCode(txt.text.toString())
                    val flag = ConfigUtil.pixivRToken.value.isNotEmpty()
                            && PixivTokenUtil.verifyToken(decryptSP(ConfigUtil.pixivRToken.value))
                    if (flag) {
                        GlobalData.nav.defaultBack()
                    }
                    GlobalData.nav.defaultBack()
                }
            }, enabled = txt.text.isNotEmpty()) {
                Text(stringResource(Res.string.confirm))
            }
        }
    }
}