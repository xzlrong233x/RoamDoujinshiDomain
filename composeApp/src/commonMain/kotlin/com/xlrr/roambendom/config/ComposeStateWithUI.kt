package com.xlrr.roambendom.config

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.selection.DisableSelection
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun CalUI(config: StateWithUI<*>, enable: Boolean = true) {
    when (config.uiType) {
        is UIType.SwitchUI -> SwitchComposer(config as StateWithUI<Boolean>, enable)
        is UIType.SingleSegmentedButton -> SingleSegmentedButtonComposer(config as StateWithUI<Int>, enable)
        is UIType.DropStringSelectUI -> DropStringSelectUIComposer(config as StateWithUI<String>, enable)
        else -> {}
    }
}

@Composable
fun SwitchComposer(config: StateWithUI<Boolean>, enable: Boolean = true) {
    if (config.uiType !is UIType.SwitchUI) return
    FlowRow(horizontalArrangement = Arrangement.SpaceAround,
        verticalArrangement = Arrangement.Center,
        itemVerticalAlignment = Alignment.CenterVertically) {
        Text(config.uiType.label)
        Spacer(Modifier.width(16.dp))
        Switch(
            config.state.value,
            {
                config.state.value = it
                config.uiType.onValueChange(it)
            },
            enabled = enable
        )
    }
}

@Composable
fun SingleSegmentedButtonComposer(config: StateWithUI<Int>, enable: Boolean = true) {
    if (config.uiType !is UIType.SingleSegmentedButton) return
    FlowRow(horizontalArrangement = Arrangement.SpaceAround,
        verticalArrangement = Arrangement.Center,
        itemVerticalAlignment = Alignment.CenterVertically) {
        Text(config.uiType.label)
        SingleChoiceSegmentedButtonRow() {
            config.uiType.choiceList.forEachIndexed { ind, str ->
                SegmentedButton(
                    shape = SegmentedButtonDefaults.itemShape(
                        index = ind,
                        count = config.uiType.choiceList.size
                    ),
                    onClick = { config.state.value = ind },
                    selected = config.state.value == ind,
                    label = { Text(str) },
                    enabled = enable
                )
            }
        }
    }
}

@Composable
fun DropStringSelectUIComposer(config: StateWithUI<String>, enable: Boolean = false) {
    if (config.uiType !is UIType.DropStringSelectUI) {
        return
    }
    var exp by remember { mutableStateOf(false) }
    DisposableEffect(Unit) {
        onDispose {
            exp = false
        }
    }
    FlowRow(horizontalArrangement = Arrangement.SpaceAround,
        verticalArrangement = Arrangement.Center,
        itemVerticalAlignment = Alignment.CenterVertically) {
        Text(config.uiType.label)
        Box() {
            Text(config.state.value)
            DropdownMenu(
                exp,
                onDismissRequest = {
                    exp = false
                }
            ) {
                config.uiType.choiceList.forEach {
                    DropdownMenuItem({
                        Text(it)
                    }, {
                        config.state.value = it
                        exp = false
                    })
                }
            }
        }
    }
}