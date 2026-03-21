package com.xlrr.roambendom.config

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.xlrr.roambendom.utils.CenterFlowRow
import org.jetbrains.compose.resources.painterResource
import roambendom.composeapp.generated.resources.Res
import roambendom.composeapp.generated.resources.down_caret

val LocalPlatformForUI = compositionLocalOf { UIEnablePlatform.ALL }

@Composable
fun CalUI(config: StateWithUI<*>, enable: Boolean = true) {
    if (config.uiType.enabledPlatform != UIEnablePlatform.ALL
        && config.uiType.enabledPlatform != LocalPlatformForUI.current) return
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
    CenterFlowRow(Modifier.fillMaxWidth()) {
        Text(config.uiType.label)
        Spacer(Modifier.width(16.dp))
        Switch(
            config.value,
            {
                config.value = it
                config.uiType.onValueChange(it)
            },
            enabled = enable
        )
    }
}

@Composable
fun SingleSegmentedButtonComposer(config: StateWithUI<Int>, enable: Boolean = true) {
    if (config.uiType !is UIType.SingleSegmentedButton) return
    CenterFlowRow(Modifier.fillMaxWidth()) {
        Text(config.uiType.label)
        SingleChoiceSegmentedButtonRow() {
            config.uiType.choiceList.forEachIndexed { ind, str ->
                SegmentedButton(
                    shape = SegmentedButtonDefaults.itemShape(
                        index = ind,
                        count = config.uiType.choiceList.size
                    ),
                    onClick = { config.value = ind },
                    selected = config.value == ind,
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
    CenterFlowRow(Modifier.fillMaxWidth()) {
        Text(config.uiType.label)
        Box() {
            TextButton({exp = true}, enabled = enable) {
                Text(config.uiType.strFunc(config.value))
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
                        Text(config.uiType.strFunc(it))
                    }, {
                        config.value = it
                        exp = false
                    })
                }
            }
        }
    }
}