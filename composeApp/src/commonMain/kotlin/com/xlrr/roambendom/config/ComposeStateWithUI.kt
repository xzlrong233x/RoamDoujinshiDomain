package com.xlrr.roambendom.config

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.painterResource
import roambendom.composeapp.generated.resources.Res
import roambendom.composeapp.generated.resources.down_caret

@Composable
fun CalUI(config: StateWithUI<*>, enable: Boolean = true) {
    when (config.uiType) {
        is UIType.SwitchUI -> SwitchComposer(config as StateWithUI<Boolean>, enable)
        is UIType.SingleSegmentedButton -> SingleSegmentedButtonComposer(config as StateWithUI<Int>, enable)
        is UIType.DropStringSelectUI -> DropStringSelectUIComposer(config, enable)
        else -> {}
    }
}

@Composable
fun SwitchComposer(config: StateWithUI<Boolean>, enable: Boolean = true) {
    if (config.uiType !is UIType.SwitchUI) return
    FlowRow(Modifier.fillMaxWidth(),horizontalArrangement = Arrangement.SpaceBetween,
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
    FlowRow(Modifier.fillMaxWidth(),horizontalArrangement = Arrangement.SpaceBetween,
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
fun <T> DropStringSelectUIComposer(config: StateWithUI<T>, enable: Boolean = false) {
    if (config.uiType !is UIType.DropStringSelectUI) {
        return
    }
    var exp by remember { mutableStateOf(false) }
    DisposableEffect(Unit) {
        onDispose {
            exp = false
        }
    }
    FlowRow(Modifier.fillMaxWidth(),horizontalArrangement = Arrangement.SpaceBetween,
        verticalArrangement = Arrangement.Center,
        itemVerticalAlignment = Alignment.CenterVertically) {
        Text(config.uiType.label)
        Box() {
            TextButton({exp = true}, enabled = enable) {
                Text(config.state.value.toString())
                Icon(
                    painterResource(Res.drawable.down_caret),
                    contentDescription = null,
                    modifier = Modifier.offset(0.dp, 1.dp)
                )
            }
            DropdownMenu(
                exp,
                onDismissRequest = {
                    exp = false
                }
            ) {
                config.uiType.choiceList.forEach {
                    DropdownMenuItem({
                        Text(it.toString())
                    }, {
                        config.state.value = it
                        exp = false
                    })
                }
            }
        }
    }
}