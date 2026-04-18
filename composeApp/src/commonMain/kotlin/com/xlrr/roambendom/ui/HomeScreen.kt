package com.xlrr.roambendom.ui

import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import com.xlrr.roambendom.LocalSharedTransitionScope
import com.xlrr.roambendom.config.ConfigUtil
import com.xlrr.roambendom.data.CLanguage
import com.xlrr.roambendom.data.PixivTestResult
import com.xlrr.roambendom.data.SearchItemData
import com.xlrr.roambendom.data.search.SearchParameterModel
import com.xlrr.roambendom.nav.Routes
import com.xlrr.roambendom.network.PIXIVApiHelper
import com.xlrr.roambendom.utils.*
import kotlinx.coroutines.launch
import kotlin.math.ceil
import kotlin.math.max

enum class HomeSelection {
    NH,
    PIXIV
}

class HomeViewModel() : ViewModel() {
    var local: HomeSelection by mutableStateOf(HomeSelection.NH)
    var error: Throwable? by mutableStateOf(null)
    private var _loading by mutableStateOf(false)
    var loading by mutableStateOf(false)
    var tempLeave by mutableStateOf(false)  //这个变量主要是为了标记是否是暂时离开（即Home被压在栈下）
    var pixivResult: PixivTestResult = PixivTestResult()
    val spm = SearchParameterModel("")

    suspend fun reload() {
        if (local == HomeSelection.NH) {
            spm.reload()
        } else {
            loading = true
            pixivResult = PIXIVApiHelper.testPixivRequest()
            loading = false
        }
    }

    fun clear() {
        pixivResult = PixivTestResult()
        spm.clear()
    }
}

@Composable
fun ShowSearchItem(it: SearchItemData, orColumn: Boolean = true) {
    Box() {
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
            },
            onclick = {
                GlobalData.nav.push(Routes.Root.Detail(it))
            },
            imgLabel = it.thumb,
            toColumn = orColumn
        )
    }
}

@Composable
private fun ChooseContent(
    k: Boolean, modifier: Modifier, searchParameterModel: SearchParameterModel,
    loading: Boolean, selection: HomeSelection, pxResult: PixivTestResult,
    headItem: @Composable () -> Unit
) {
    if (selection == HomeSelection.PIXIV) {
        LazyColumn(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            item("head") {
                headItem()
            }
            item("explanation") {
                Text("P站插画/主页页面逻辑复杂，程序的屎山叠太高了，以后再做") // TODO: 完善Pixiv类型的主页。
            }
            if (!loading) {
                item("INFOS") {
                    Text("是否可以访问网页: ${pxResult.canRequestWebsite}")
                    Text("账号是否可用: ${pxResult.isUserSigned}")
                    Text("是否显示可能包含敏感内容的作品: ${pxResult.canReadSensitive}")
                    Text("是否显示浏览限制作品（R-18）: ${pxResult.canReadR18}")
                    Text("是否显示猎奇向作品（R-18G）: ${pxResult.canReadR18G}")
                }
            }
        }
    }
    else {
        SearchContent(
            modifier, searchParameterModel, StaggeredGridCells.Fixed(
                ceil(LocalWindowSize.current.width.value / 216f)
                    .coerceIn(1f, max(6f, LocalWindowSize.current.width.value / 216 - 2)).toInt()
            ), k, headItem,
            { spm ->
                item("popular") {
                    Text("热门", style = MaterialTheme.typography.headlineSmall)
                }
                items(spm.content.distinct().subList(0,5), {"popular${it.id}"}) {
                    with(LocalSharedTransitionScope.current) {
                        ShowSearchItem(it, false)
                    }
                }
                item("lastest") {
                    Text("最新", style = MaterialTheme.typography.headlineSmall)
                }
                items(spm.content.distinct().subList(5,spm.content.size), {"lasest${it.id}"}) {
                    with(LocalSharedTransitionScope.current) {
                        ShowSearchItem(it, false)
                    }
                }
            },
            {spm ->
                item("popular",span = StaggeredGridItemSpan.FullLine) {
                    Text("热门", style = MaterialTheme.typography.headlineSmall)
                }
                items(spm.content.distinct().subList(0,5), {"popular${it.id}"}) {
                    with(LocalSharedTransitionScope.current) {
                        ShowSearchItem(it)
                    }
                }
                item("lastest",span = StaggeredGridItemSpan.FullLine) {
                    Text("最新", style = MaterialTheme.typography.headlineSmall)
                }
                items(spm.content.distinct().subList(5,spm.content.size), {"lasest${it.id}"}) {
                    with(LocalSharedTransitionScope.current) {
                        ShowSearchItem(it)
                    }
                }
            }
        )
    }
}

@Composable
fun HomeScreen(modifier: Modifier = Modifier, viewModel: HomeViewModel = viewModel { HomeViewModel() }) {
    val small = LocalWindowSize.current.width < SmallScreenDpLine
    LaunchedEffect(viewModel.local) {
        GlobalData.homeContentSelection = viewModel.local
        if (!viewModel.tempLeave || (viewModel.local == HomeSelection.NH && viewModel.spm.content.isEmpty())) {
            viewModel.reload()
        } else {
            viewModel.tempLeave = false
        }
    }
    DisposableEffect(Unit) {
        onDispose {
            GlobalData.homeContentSelection = null
            if (GlobalData.nav.backStack.any { it is Routes.Root.Home }) {
                viewModel.tempLeave = true
            } else {
                viewModel.clear()
            }
        }
    }
    val showBtn by remember(GlobalData.forListState) {
        derivedStateOf {
            GlobalData.forListState != null
                    && GlobalData.forListState?.scrollIndicatorState?.scrollOffset?.let { it > 0 } == true
                    && viewModel.local == HomeSelection.NH
        }
    }
    val ss = rememberCoroutineScope()
    Scaffold(
        modifier.fillMaxSize(),
        floatingActionButton = {
            if (showBtn) { //TODO: 先占位，以后再改
                FloatingActionButton({ ss.launch { GlobalData.forListState?.scrollBy(-Float.MAX_VALUE) } }) {
                    Text("UP")
                }
            }
        }) {pd ->
        CtrlPullToRefreshBox(
            viewModel.spm.loading && viewModel.spm.refreshing,
            {ss.launch { viewModel.spm.refresh() }},
            Modifier.fillMaxSize().padding(pd),
            contentAlignment = Alignment.TopCenter
        ) {
            ChooseContent(
                small && !ConfigUtil.forceGrid.value, modifier.fillMaxWidth(),
                viewModel.spm,
                viewModel.loading || viewModel.spm.loading,
                viewModel.local,
                viewModel.pixivResult,
                {
                    Box {
                        SingleChoiceSegmentedButtonRow {
                            HomeSelection.entries.forEachIndexed { index, selection ->
                                SegmentedButton(
                                    viewModel.local == selection,
                                    { viewModel.local = selection },
                                    SegmentedButtonDefaults.itemShape(
                                        index = index,
                                        count = 2
                                    ),
                                ) {
                                    Text(selection.toString())
                                }
                            }
                        }
                    }
                }
            )
            if (viewModel.loading || (viewModel.spm.loading && viewModel.spm.content.isEmpty())) {
                CenterCircular()
            }
            if (viewModel.spm.content.isEmpty() && viewModel.spm.error != null) {
                viewModel.spm.error?.let {
                    CenterColumnInfo {
                        Text("错误：${it.message}")
                        Button({
                            ss.launch {
                                viewModel.reload()
                            }
                        }) {
                            Text("点我重载")
                        }
                    }
                }
            }
        }
    }
}

@Preview
@Composable
fun test() {
    HomeScreen()
}