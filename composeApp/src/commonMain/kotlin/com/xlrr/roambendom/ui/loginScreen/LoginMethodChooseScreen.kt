package com.xlrr.roambendom.ui.loginScreen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.xlrr.roambendom.config.ConfigUtil
import com.xlrr.roambendom.nav.Routes
import com.xlrr.roambendom.network.PIXIVApiHelper
import com.xlrr.roambendom.ui.RootBarHeight
import com.xlrr.roambendom.utils.GlobalData
import com.xlrr.roambendom.utils.PixivTokenUtil
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@Composable
fun ProxyAskAlert(enable: Boolean, cs: CoroutineScope, viewModel: LoginViewModel, dismiss: () -> Unit) {
    if (enable) {
        AlertDialog(dismiss, {
            AskConfirmButton(viewModel, dismiss, cs)
        }, Modifier, {
            TextButton(dismiss) {
                Text("取消")
            }
        }, title = {Text("需要代理", style = MaterialTheme.typography.titleMedium)},
            text = {Text("因技术问题，该方式需要您开启代理以访问pixiv")})
    }
}

@Composable
fun RefreshTokenAsk(enable: Boolean, dismiss: () -> Unit) {
    val txt = rememberTextFieldState()
    val cs = rememberCoroutineScope()
    var loading by remember { mutableStateOf(false) }
    var err by remember { mutableStateOf(false) }
    if (enable) {
        AlertDialog(dismiss, {
            TextButton({
                if (txt.text.isNotEmpty()) {
                    cs.launch {
                        loading = true
                        if (PixivTokenUtil.verifyToken(txt.text.toString())) {
                            ConfigUtil.pixivRToken.value = txt.text.toString()
                            dismiss()
                            GlobalData.nav.defaultBack()
                            return@launch
                        }
                        err = true
                        loading = false
                    }
                }
            }, enabled = !loading) {
                Text("确定")
            }
        }, Modifier, {
            TextButton(dismiss, enabled = !loading) {
                Text("取消")
            }
        }, text = {
            OutlinedTextField(
                txt,
                enabled = !loading,
                placeholder = {Text("refresh_token")},
                isError = err,
            ) },title = {Text("输入Token")})
    }
}

@Composable
fun LoginMethodChooseScreen(modifier: Modifier, cs: CoroutineScope = rememberCoroutineScope(),
                            viewModel: LoginViewModel = viewModel { LoginViewModel() }
) {
    var pa by remember { mutableStateOf(false) }
    var pn by remember { mutableStateOf(false) }
    val cur = GlobalData.nav.backStack.last() as? Routes.Auth.Choose
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(verticalArrangement = Arrangement.spacedBy(24.dp)) {
            Button({
                pa = true
            }, Modifier.widthIn(Dp.Unspecified,256.dp).fillMaxWidth(0.85f).height(52.dp),
                shape = RoundedCornerShape(16.dp)) {
                Text("账号登录", style = MaterialTheme.typography.headlineSmall, modifier = Modifier)
            }
            OutlinedButton({
                pn = true
            }, Modifier.widthIn(Dp.Unspecified,256.dp).fillMaxWidth(0.85f).height(52.dp),
                shape = RoundedCornerShape(16.dp)) {
                Text("Token", style = MaterialTheme.typography.headlineSmall, modifier = Modifier)
            }
        }
    }
    ProxyAskAlert(pa, cs, viewModel) { pa = false }
    RefreshTokenAsk(pn) { pn = false }
}

@Composable
fun TotalAuthScreen(modifier: Modifier) {
    val scope = rememberCoroutineScope()
    Column(modifier.fillMaxSize()) {
        Surface(Modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.secondaryContainer) {
            Row(
                Modifier.fillMaxWidth().statusBarsPadding().height(RootBarHeight),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton({ GlobalData.nav.defaultBack() }) {
                    Text("返")
                }
            }
        }
        when (val cur = GlobalData.nav.backStack.last()) {
            is Routes.Auth.Choose -> LoginMethodChooseScreen(modifier, scope)
        }
    }
}

@Preview
@Composable
fun LMCTest() {
    LoginMethodChooseScreen(Modifier)
}