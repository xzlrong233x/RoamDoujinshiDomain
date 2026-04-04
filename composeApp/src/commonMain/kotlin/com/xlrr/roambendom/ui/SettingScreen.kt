package com.xlrr.roambendom.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.xlrr.roambendom.config.CalUI
import com.xlrr.roambendom.config.ConfigUtil
import com.xlrr.roambendom.getFormatVersionString
import com.xlrr.roambendom.nav.Routes
import com.xlrr.roambendom.utils.CenterFlowRow
import com.xlrr.roambendom.utils.GlobalData

@Composable
fun SettingScreen(modifier: Modifier) {
    Box(modifier.fillMaxSize(), Alignment.Center) {
        Column(Modifier.widthIn(0.dp, 712.dp).fillMaxSize()
            .padding(6.dp, 0.dp).verticalScroll(rememberScrollState()),
            Arrangement.spacedBy(6.dp)) {
            Text("设置", style = MaterialTheme.typography.headlineMedium)
            CalUI(ConfigUtil.forceGrid)
            CalUI(ConfigUtil.useMultithread)
            CalUI(ConfigUtil.streamDisplay)
            CalUI(ConfigUtil.disableHistoryRecord)
            CalUI(ConfigUtil.enableVolumeTurn)
            Spacer(Modifier.height(16.dp))
            CalUI(ConfigUtil.pixivLanguage)
            CenterFlowRow(Modifier.fillMaxWidth()) {
                Text("P站Token")
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Button({ GlobalData.nav.push(Routes.TokenForm)}) {
                        Text("点我填写")
                    }
                }
            }
            Spacer(Modifier.height(18.dp))
            var size by remember {
                mutableStateOf(GlobalData.historyData.requestTokens("").size)
            }
            CenterFlowRow(Modifier.fillMaxWidth()) {
                Text("搜索关键词记录")
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text("已有${size}条", style = MaterialTheme.typography.labelMedium)
                    Button({ GlobalData.historyData.clearSearchToken(); size = 0}, enabled = size > 0) {
                        Text("清除所有")
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            CenterFlowRow(Modifier.fillMaxWidth()) {
                Text("版本")
                Text(getFormatVersionString())
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