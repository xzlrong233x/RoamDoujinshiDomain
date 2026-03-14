package com.xlrr.roambendom.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.xlrr.roambendom.config.CalUI
import com.xlrr.roambendom.data.search.SearchParameterModel
import com.xlrr.roambendom.utils.LocalWindowSize
import kotlin.math.min

@Composable
fun SearchSettingDialog(onDismiss: () -> Unit, searchParameterModel: SearchParameterModel) {
    val cfg by rememberUpdatedState(searchParameterModel.configs)
    Dialog(onDismissRequest = onDismiss) {
        Card(
            Modifier.fillMaxWidth()
                .height((min(LocalWindowSize.current.height.value * 0.8f, 627f)).dp)
                .padding(16.dp),
            shape = RoundedCornerShape(12.dp)
        ) {
            Scaffold(
                Modifier.padding(12.dp),
                bottomBar = {
                    Row(Modifier.fillMaxWidth()
                        .background(CardDefaults.cardColors().containerColor), Arrangement.End) {
                        TextButton(onDismiss) {
                            Text("确定")
                        }
                    }
                },
                containerColor = CardDefaults.cardColors().containerColor
            ) { pd ->
                Column(Modifier.padding(pd).fillMaxSize()) {
                    CalUI(cfg.searchTarget)
                    if (cfg.searchTarget.value == 1) {
                        CalUI(cfg.pixivSearchRestriction)
                    }
                }
            }
        }
    }
}