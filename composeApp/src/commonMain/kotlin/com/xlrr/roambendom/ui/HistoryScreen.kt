package com.xlrr.roambendom.ui

import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.xlrr.roambendom.LocalSharedTransitionScope
import com.xlrr.roambendom.config.ConfigUtil
import com.xlrr.roambendom.data.CLanguage
import com.xlrr.roambendom.data.SearchItemData
import com.xlrr.roambendom.data.search.SearchParameterModel
import com.xlrr.roambendom.nav.Routes
import com.xlrr.roambendom.utils.*
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.painterResource
import roambendom.composeapp.generated.resources.Res
import roambendom.composeapp.generated.resources.delete_icon
import kotlin.math.ceil
import kotlin.math.max

@Composable
private fun ShowHistoryItem(it: SearchItemData, modifier: Modifier,
                            remove: (String) -> Unit, orColumn: Boolean = true) {
    Box(modifier) {
        ItemInfoCardWithShared(
            it.thumb,
            if (it.pageCount > 0) it.pageCount.toString() else "null",
            it.title,
            it.restriction,
            it.lang.let { x ->
                if (x != CLanguage.Unknown) x.toString().lowercase() else it.author.ifEmpty { null }
            },
            {
                if (it.ai) Text("*有AI参与的作品")
                if (it.time > 0) Text(TimeUtil.formatTime(it.time))
                IconButton({
                    remove(it.uid())
                }, Modifier.align(Alignment.End)) {
                    Icon(painterResource(Res.drawable.delete_icon), "delete icon")
                }
            },
            imgLabel = it.thumb,
            onclick = {
                GlobalData.nav.push(Routes.Root.Detail(it))
            },
            toColumn = orColumn
        )
    }
}

@Composable
fun HistoryScreen(modifier: Modifier, searchParameterModel: SearchParameterModel) {
    val ss = rememberCoroutineScope()
    val showBtn by remember(GlobalData.forListState) {
        derivedStateOf {
            GlobalData.forListState != null
                    && GlobalData.forListState?.scrollIndicatorState?.scrollOffset?.let { it > 0 } == true
        }
    }
    val mx = LocalWindowSize.current.width
    val removeList = remember { mutableListOf<String>() }

    LaunchedEffect(Unit) {
        if (searchParameterModel.content.isEmpty() && !searchParameterModel.end) {
            searchParameterModel.reload()
        }
    }

    Scaffold(
        modifier.fillMaxSize(),
        floatingActionButton = {
            if (showBtn) { //TODO: 先占位，以后再改
                FloatingActionButton({ ss.launch { GlobalData.forListState?.scrollBy(-100000f) } }) {
                    Text("UP")
                }
            }
        }) {pd ->
        Box(Modifier.fillMaxSize().padding(pd), Alignment.TopCenter) {
            SearchContent(modifier, searchParameterModel, StaggeredGridCells.Fixed(
                ceil(mx.value / 216f).coerceIn(1f, max(6f, mx.value / 216 - 2)).toInt()
            ), mx < SmallScreenDpLine && !ConfigUtil.forceGrid.state.value,
                header = {
                    Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                        Text("历史", style = MaterialTheme.typography.headlineMedium)
                        Button({
                            GlobalData.historyData.removeAll()
                            ss.launch {
                                searchParameterModel.reload()
                            }
                        }) {
                            Text("清除所有记录")
                        }
                    }
                },
                listMain = {spm ->
                    items(spm.content.distinct(), {it.uid()}) {
                        with(LocalSharedTransitionScope.current) {
                            ShowHistoryItem(it, Modifier.animateItem(),{ s ->
                                spm.content.remove(it)
                                GlobalData.historyData.remove(s)
                            }, false)
                        }
                    }
                },
                gridMain = {spm ->
                    items(spm.content.distinct(), {it.uid()}) {
                        with(LocalSharedTransitionScope.current) {
                            ShowHistoryItem(it, Modifier.animateItem(),{s ->
                                spm.content.remove(it)
                                GlobalData.historyData.remove(s)
                            }, true)
                        }
                    }
                },
                ss = ss)
            if (searchParameterModel.content.isEmpty() && searchParameterModel.loading) {
                CenterCircular()
            }
            if (searchParameterModel.content.isEmpty()) {
                if (searchParameterModel.error != null) {
                    searchParameterModel.error?.let {
                        CenterColumnInfo {
                            Text("错误：${it.message}")
                            Button({
                                ss.launch {
                                    searchParameterModel.reload()
                                }
                            }) {
                                Text("点我重载")
                            }
                        }
                    }
                } else if (!searchParameterModel.loading) {
                    Box(Modifier.fillMaxSize(), Alignment.Center) {
                        Text("空空如也")
                    }
                }
            }
        }
    }
}