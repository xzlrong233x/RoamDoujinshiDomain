package com.xlrr.roambendom.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import com.xlrr.roambendom.LocalSharedTransitionScope
import com.xlrr.roambendom.config.ConfigUtil
import com.xlrr.roambendom.data.CLanguage
import com.xlrr.roambendom.data.PixivTestResult
import com.xlrr.roambendom.data.SearchItemData
import com.xlrr.roambendom.model.detail.asDetail
import com.xlrr.roambendom.model.search.SearchParameterModel
import com.xlrr.roambendom.nav.Routes
import com.xlrr.roambendom.network.PIXIVApiHelper
import com.xlrr.roambendom.utils.*
import kotlin.math.ceil
import kotlin.math.max

enum class HomeSelection {
    NH,
    PIXIV
}

class HomeViewModel() : ViewModel() {
    var local: HomeSelection by mutableStateOf(HomeSelection.NH)
    var error: Throwable? by mutableStateOf(null)
    var loading by mutableStateOf(false)
    var tempLeave by mutableStateOf(false)  //这个变量主要是为了标记是否是暂时离开（即Home被压在栈下）
    val spm = SearchParameterModel("")
    val pixivSPM = SearchParameterModel("").config {
        searchFunction = PIXIVApiHelper.nextUrlSearchFunction { key, offset, viewed ->
            PIXIVApiHelper.illustRecommend(offset, viewed)
        }
    }

    suspend fun reload() {
        if (local == HomeSelection.NH) {
            spm.reload()
        } else {
            pixivSPM.reload()
        }
    }

    fun clear() {
        pixivSPM.clear()
        spm.clear()
    }
}

@Composable
fun ShowSearchItem(it: SearchItemData, orColumn: Boolean = true) {
    Box() {
        ItemInfoCardWithShared(
            it.thumb,
            it.pageCount,
            it.title,
            it.restriction,
            if (it.width <= 0 || it.height <= 0) null else IntSize(it.width, it.height),
            it.lang.let { x ->
                if (x != CLanguage.Unknown) x.toString().lowercase() else it.author.ifEmpty { null }
            },
            {
                if (it.ai) Text("*有AI参与的作品")
                if (it.time > 0) Text(TimeUtil.formatTime(it.time))
            },
            it.isAnimation,
            onClick = {
                GlobalData.nav.push(Routes.Root.Detail(it.asDetail()))
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
                    if (pxResult.isUserSigned) {
                        Text("已登录")
                    } else {
                        Text("您尚未登录，未登录会使大多P站功能不可用，您可以通过下面的按钮选择登录方式")
                        Button({GlobalData.nav.push(Routes.Auth.Choose())}) {
                            Text("选择登录方式")
                        }
                    }
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
    val curSpm = if (viewModel.local == HomeSelection.NH) viewModel.spm else viewModel.pixivSPM
    StandardSearchLikeWithUp(
        curSpm,
        modifier
    ) {
        SearchContent(
            modifier,
            curSpm,
            StaggeredGridCells.Fixed(
                ceil(LocalWindowSize.current.width.value / 216f)
                    .coerceIn(1f, max(6f, LocalWindowSize.current.width.value / 216 - 2)).toInt()
            ),
            small && !ConfigUtil.forceGrid.value && viewModel.local == HomeSelection.NH,
            {
                Column(Modifier.fillMaxWidth()) {
                    SingleChoiceSegmentedButtonRow {
                        HomeSelection.entries.forEachIndexed { index, selection ->
                            SegmentedButton(
                                viewModel.local == selection,
                                {
                                    viewModel.local = selection
                                    if (selection == HomeSelection.PIXIV) {
                                        viewModel.spm.reset()
                                    }
                                },
                                SegmentedButtonDefaults.itemShape(
                                    index = index,
                                    count = 2
                                ),
                            ) {
                                Text(selection.toString())
                            }
                        }
                    }
                    if (viewModel.local == HomeSelection.PIXIV
                        && viewModel.pixivSPM.content.isEmpty()
                        && viewModel.pixivSPM.completed) {
                        Text("您尚未登录，未登录会使大多P站功能不可用，您可以通过下面的按钮选择登录方式")
                        Button({GlobalData.nav.push(Routes.Auth.Choose())}) {
                            Text("选择登录方式")
                        }
                    }
                }
            },
            { spm ->
                if (viewModel.local == HomeSelection.NH) {
                    item("popular") {
                        Text("热门", style = MaterialTheme.typography.headlineSmall)
                    }
                    items(spm.content.distinct().subList(0, 5), { "popular${it.id}" }) {
                        with(LocalSharedTransitionScope.current) {
                            ShowSearchItem(it, false)
                        }
                    }
                    item("lastest") {
                        Text("最新", style = MaterialTheme.typography.headlineSmall)
                    }
                    items(spm.content.distinct().subList(5, spm.content.size), { "lasest${it.id}" }) {
                        with(LocalSharedTransitionScope.current) {
                            ShowSearchItem(it, false)
                        }
                    }
                } else {
                    items(spm.content.distinct(), {it.id}) {
                        with(LocalSharedTransitionScope.current) {
                            ShowSearchItem(it, true)
                        }
                    }
                }
            },
            {spm ->
                if (viewModel.local == HomeSelection.NH) {
                    item("popular", span = StaggeredGridItemSpan.FullLine) {
                        Text("热门", style = MaterialTheme.typography.headlineSmall)
                    }
                    items(spm.content.distinct().subList(0, 5), { "popular${it.id}" }) {
                        with(LocalSharedTransitionScope.current) {
                            ShowSearchItem(it)
                        }
                    }
                    item("lastest", span = StaggeredGridItemSpan.FullLine) {
                        Text("最新", style = MaterialTheme.typography.headlineSmall)
                    }
                    items(spm.content.distinct().subList(5, spm.content.size), { "lasest${it.id}" }) {
                        with(LocalSharedTransitionScope.current) {
                            ShowSearchItem(it)
                        }
                    }
                } else {
                    items(spm.content.distinct(), {it.id}) {
                        with(LocalSharedTransitionScope.current) {
                            ShowSearchItem(it, true)
                        }
                    }
                }
            },
            ss = ss
        )
    }
//    Scaffold(
//        modifier.fillMaxSize(),
//        floatingActionButton = {
//            if (showBtn) { //TODO: 先占位，以后再改
//                FloatingActionButton({ ss.launch { GlobalData.forListState?.scrollBy(-Float.MAX_VALUE) } }) {
//                    Text("UP")
//                }
//            }
//        }) {pd ->
//        CtrlPullToRefreshBox(
//            viewModel.spm.loading && viewModel.spm.refreshing,
//            {ss.launch { viewModel.spm.refresh() }},
//            Modifier.fillMaxSize().padding(pd),
//            contentAlignment = Alignment.TopCenter
//        ) {
//            ChooseContent(
//                small && !ConfigUtil.forceGrid.value, modifier.fillMaxWidth(),
//                viewModel.spm,
//                viewModel.loading || viewModel.spm.loading,
//                viewModel.local,
//                viewModel.pixivResult,
//                {
//                    Box {
//                        SingleChoiceSegmentedButtonRow {
//                            HomeSelection.entries.forEachIndexed { index, selection ->
//                                SegmentedButton(
//                                    viewModel.local == selection,
//                                    {
//                                        viewModel.local = selection
//                                        if (selection == HomeSelection.PIXIV) {
//                                            viewModel.spm.reset()
//                                        }
//                                    },
//                                    SegmentedButtonDefaults.itemShape(
//                                        index = index,
//                                        count = 2
//                                    ),
//                                ) {
//                                    Text(selection.toString())
//                                }
//                            }
//                        }
//                    }
//                }
//            )
//            if (viewModel.loading || viewModel.spm.isFullLoading()) {
//                CenterCircular()
//            }
//            if (viewModel.spm.content.isEmpty() && viewModel.spm.error != null) {
//                viewModel.spm.error?.let {
//                    CenterColumnInfo {
//                        Text("错误：${it.message}")
//                        Button({
//                            ss.launch {
//                                viewModel.reload()
//                            }
//                        }) {
//                            Text("点我重载")
//                        }
//                    }
//                }
//            }
//        }
//    }
}

@Preview
@Composable
fun test() {
    HomeScreen()
}