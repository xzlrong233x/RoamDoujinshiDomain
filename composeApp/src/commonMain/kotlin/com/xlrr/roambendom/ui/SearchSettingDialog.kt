package com.xlrr.roambendom.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.xlrr.roambendom.LocalAnimatedVisibilityScope
import com.xlrr.roambendom.LocalSharedTransitionScope
import com.xlrr.roambendom.config.CalUI
import com.xlrr.roambendom.model.search.SearchParameterModel
import com.xlrr.roambendom.utils.CenterFlowRow
import com.xlrr.roambendom.utils.LocalWindowSize
import com.xlrr.roambendom.utils.TimeUtil
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import roambendom.composeapp.generated.resources.Res
import roambendom.composeapp.generated.resources.cancel
import roambendom.composeapp.generated.resources.click_reload
import roambendom.composeapp.generated.resources.close_icon
import roambendom.composeapp.generated.resources.confirm
import roambendom.composeapp.generated.resources.save
import roambendom.composeapp.generated.resources.select_time
import roambendom.composeapp.generated.resources.select_time_range
import roambendom.composeapp.generated.resources.time_range
import kotlin.math.min

@Composable
fun SearchSettingDialog(onDismiss: () -> Unit, searchParameterModel: SearchParameterModel, cs: CoroutineScope) {
    var par: Pair<Long?, Long?> by remember { mutableStateOf(Pair(null, null)) }
    var ct by remember { mutableIntStateOf(0) }
    Dialog(onDismissRequest = onDismiss) {
        SharedTransitionLayout {
            AnimatedContent(ct) {s ->
                CompositionLocalProvider(
                    LocalAnimatedVisibilityScope provides this,
                    LocalSharedTransitionScope provides this@SharedTransitionLayout
                ) {
                    when (s) {
                        0 -> {
                            NormalSettingDialog(onDismiss, searchParameterModel, cs, { ct = it })
                        }
                        1 -> {
                            DateSettingDialog(onDismiss, searchParameterModel, cs, { ct = it })
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DateSettingDialog(
    onDismiss: () -> Unit,
    searchParameterModel: SearchParameterModel,
    cs: CoroutineScope,
    dialogStateTrans: (Int) -> Unit
) {
    val dateRangePickerState = searchParameterModel.configs.dateRange.value.let {
        rememberDateRangePickerState(
            if (it.first == 0L) null else it.first,
            if (it.second == 0L) null else it.second,
            selectableDates = TimeUtil.DATE_PICK_RANGE
        )
    }
    CardWithScaffoldAnimated(
        Modifier.requiredWidth(360.dp).fillMaxWidth()
            .height((min(LocalWindowSize.current.height.value * 0.85f, 774f)).dp),
        scaffoldModifier = Modifier.padding(2.dp),
        bottomBar = {
            TextButton({
                dialogStateTrans(0)
            }) {
                Text(stringResource(Res.string.cancel))
            }
            TextButton({
                dialogStateTrans(0)
                searchParameterModel.configs.dateRange.value = Pair(
                    dateRangePickerState.selectedStartDateMillis!!,
                    dateRangePickerState.selectedEndDateMillis!!
                )
            }, enabled = dateRangePickerState.selectedStartDateMillis != null && dateRangePickerState.selectedEndDateMillis != null) {
                Text(stringResource(Res.string.save))
            }
        }
    ) {pd ->
        val df = remember { DatePickerDefaults.dateFormatter() }
        DateRangePicker(
            state = dateRangePickerState,
            title = {
                Text(
                    text = stringResource(Res.string.select_time_range) //select_time_range
                )
            },
            showModeToggle = false,
            dateFormatter = df,
            modifier = Modifier
                .fillMaxSize()
                .padding(pd),
            colors = DatePickerDefaults.colors(containerColor = CardDefaults.cardColors().containerColor),
        )
    }
}

@Composable
private fun DateSelectButton(searchParameterModel: SearchParameterModel, dialogStateTrans: (Int) -> Unit) {
    Row {
        val b = searchParameterModel.configs.dateRange.value.let { it.second == 0L }
        if (!b) {
            IconButton({
                searchParameterModel.configs.dateRange.value = Pair(0L, 0L)
            }) {
                Icon(painterResource(Res.drawable.close_icon), null)
            }
        }
        TextButton({dialogStateTrans(1)}) {
            if (b) {
                Text(stringResource(Res.string.select_time)) //select_time
            } else {
                searchParameterModel.configs.dateRange.value.let {
                    Text("${
                        TimeUtil.formatTime(
                            it.first,
                            TimeUtil.PIXIV_PARAM_FORMATTER
                        )
                    }/${
                        TimeUtil.formatTime(
                            it.second,
                            TimeUtil.PIXIV_PARAM_FORMATTER
                        )
                    }")
                }
            }
        }
    }
}

@Composable
private fun NormalSettingDialog(
    onDismiss: () -> Unit,
    searchParameterModel: SearchParameterModel,
    cs: CoroutineScope,
    dialogStateTrans: (Int) -> Unit
) {
    CardWithScaffoldAnimated(
        Modifier.fillMaxWidth()
            .height((min(LocalWindowSize.current.height.value * 0.8f, 627f)).dp)
            .padding(16.dp), scaffoldModifier = Modifier.padding(12.dp),
        bottomBar = {
            Row(
                Modifier.fillMaxWidth().background(CardDefaults.cardColors().containerColor),
                Arrangement.End
            ) {
                TextButton({
                    onDismiss()
                    if (searchParameterModel.configs.searchTarget.isUnchange()) {
                        cs.launch {
                            searchParameterModel.reload()
                        }
                    }
                }) {
                    Text(stringResource(Res.string.confirm))
                }
            }
        }
    ) {pd ->
        Column(Modifier.padding(pd).fillMaxSize()) {
            CalUI(searchParameterModel.configs.searchTarget)
            if (searchParameterModel.configs.searchTarget.value == 1) {
                //CalUI(cfg.pixivSearchRestriction)
                CalUI(searchParameterModel.configs.pixivHideAI)
                CenterFlowRow(Modifier.fillMaxWidth()) {
                    Text(stringResource(Res.string.time_range)) //time_range
                    Spacer(Modifier.width(16.dp))
                    DateSelectButton(searchParameterModel, dialogStateTrans)
                }
            } else {
                CalUI(searchParameterModel.configs.nhSearchSort)
            }
        }
    }
}

@Composable
private fun CardWithScaffoldAnimated(
    modifier: Modifier = Modifier,
    shape: Shape = CardDefaults.shape,
    scaffoldModifier: Modifier,
    bottomBar: @Composable ((RowScope) -> Unit) = {},
    content: @Composable ((PaddingValues) -> Unit)
) {
    with(LocalSharedTransitionScope.current) {
        Card(modifier.sharedBounds(
            rememberSharedContentState("SearchSettingDialogSharedCard"),
            LocalAnimatedVisibilityScope.current
        ), shape = shape) {
            Scaffold(
                scaffoldModifier,
                bottomBar = {
                    Row(Modifier.fillMaxWidth()
                        .background(CardDefaults.cardColors().containerColor),
                        Arrangement.End, content = bottomBar)
                },
                containerColor = CardDefaults.cardColors().containerColor,
                content = content
            )
        }
    }
}