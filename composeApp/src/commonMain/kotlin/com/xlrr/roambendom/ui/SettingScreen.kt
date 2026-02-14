package com.xlrr.roambendom.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
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

@Composable
fun SettingScreen(modifier: Modifier) {
    Box(modifier.fillMaxSize(), Alignment.Center) {
        Column(Modifier.widthIn(0.dp, 712.dp).fillMaxSize().padding(6.dp, 0.dp),
            Arrangement.spacedBy(6.dp)) {
            Text("设置", style = MaterialTheme.typography.headlineMedium)
            CalUI(ConfigUtil.forceGrid)
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