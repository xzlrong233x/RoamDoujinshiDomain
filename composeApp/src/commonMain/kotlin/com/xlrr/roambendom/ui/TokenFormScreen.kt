package com.xlrr.roambendom.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material3.Button
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.xlrr.roambendom.utils.GlobalData
import com.xlrr.roambendom.utils.PixivTokenUtil

@Composable
fun TokenFormScreen(modifier: Modifier) {
    val a = rememberTextFieldState()
    val b = rememberTextFieldState()
    val c = rememberTextFieldState()
    Column(modifier.fillMaxSize()) {
        Surface(Modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.secondaryContainer) {
            Row(Modifier.fillMaxWidth().statusBarsPadding().height(RootBarHeight),
                verticalAlignment = Alignment.CenterVertically,) {
                IconButton({ GlobalData.nav.defaultBack()}) {
                    Text("返")
                }
            }
        }
        Box(Modifier.fillMaxSize()) {
            Column(Modifier.align(Alignment.Center), Arrangement.spacedBy(6.dp)) {
                Text("请自行登录P站后，在cookie中寻找到以下值并填入", style = MaterialTheme.typography.titleSmall)
                OutlinedTextField(a, Modifier.align(Alignment.CenterHorizontally).width(292.dp),
                    lineLimits = TextFieldLineLimits.SingleLine, placeholder = {Text("yuid_b")})
                OutlinedTextField(b, Modifier.align(Alignment.CenterHorizontally).width(292.dp),
                    lineLimits = TextFieldLineLimits.SingleLine, placeholder = {Text("PHPSESSID")})
                OutlinedTextField(c, Modifier.align(Alignment.CenterHorizontally).width(292.dp),
                    lineLimits = TextFieldLineLimits.SingleLine, placeholder = {Text("device_token")})
                Text("*本人实力不足，暂时做不到仿照登录界面及系统", Modifier.align(Alignment.CenterHorizontally),
                    color = Color.Gray, style = MaterialTheme.typography.labelSmall)
                Spacer(Modifier.height(16.dp))
                Button({
                    PixivTokenUtil.setToken(a.text.toString(), b.text.toString(), c.text.toString())
                    GlobalData.nav.defaultBack()
                }, Modifier.width(96.dp).align(Alignment.End), enabled = listOf(a,b,c).all { it.text.isNotEmpty() },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("确定")
                }
            }
        }
    }
}

@Composable
@Preview
fun TXTEST() {
    MaterialTheme {
        TokenFormScreen(Modifier)
    }
}