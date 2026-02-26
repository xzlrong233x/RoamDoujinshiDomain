package com.xlrr.roambendom.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.xlrr.roambendom.config.CalUI
import com.xlrr.roambendom.config.ConfigUtil
import com.xlrr.roambendom.nav.Routes
import com.xlrr.roambendom.utils.GlobalData

@Composable
fun SettingScreen(modifier: Modifier) {
    Box(modifier.fillMaxSize(), Alignment.Center) {
        Column(Modifier.widthIn(0.dp, 712.dp).fillMaxSize().padding(6.dp, 0.dp),
            Arrangement.spacedBy(6.dp)) {
            Text("设置", style = MaterialTheme.typography.headlineMedium)
            CalUI(ConfigUtil.forceGrid)
            CalUI(ConfigUtil.useMultithread)
            CalUI(ConfigUtil.disableHistoryRecord)
            Spacer(Modifier.height(16.dp))
            CalUI(ConfigUtil.pixivLanguage)
            FlowRow(Modifier.fillMaxWidth(),horizontalArrangement = Arrangement.SpaceBetween,
                verticalArrangement = Arrangement.Center,
                itemVerticalAlignment = Alignment.CenterVertically) {
                Text("P站Token")
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Button({ GlobalData.nav.push(Routes.TokenForm)}) {
                        Text("点我填写")
                    }
                }
            }
        }
    }
}

@Composable
@Preview
fun SetTest() {
    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            FlowRow(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                Text("233")
                Button({}) {Text("456")}
            }
        }
    }
}