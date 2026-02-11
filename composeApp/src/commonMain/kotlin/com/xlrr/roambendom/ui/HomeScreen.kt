package com.xlrr.roambendom.ui

import androidx.compose.foundation.gestures.ScrollableState
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.staggeredgrid.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import com.xlrr.roambendom.LocalSharedTransitionScope
import com.xlrr.roambendom.data.SearchItemData
import com.xlrr.roambendom.nav.Routes
import com.xlrr.roambendom.network.NHWebHelper
import com.xlrr.roambendom.utils.*
import kotlinx.coroutines.launch
import kotlin.math.max

enum class HomeSelection {
    NH,
    PIXIV
}

class HomeViewModel() : ViewModel() {
    var local: HomeSelection by mutableStateOf(HomeSelection.NH)
    val content = mutableStateListOf<SearchItemData>()
    var page by mutableIntStateOf(0)
    var isEnd by mutableStateOf(false)
    var loading by mutableStateOf(false)
    var scrollState : ScrollableState? = null
    var tempLeave by mutableStateOf(false)

    suspend fun reload() {
        clear()
        requestNext()
    }

    fun clear() {
        content.clear()
        page = 0
    }

    suspend fun requestNext() {
        page += 1
        loading = true
        val result = NHWebHelper.search("", page)
        if (result.items.isNotEmpty()) {
            content.addAll(result.items)
        }
        else isEnd = true
        loading = false
    }
}

@Composable
private fun StaggeredGridContent(modifier: Modifier,
                                 scrollState: ScrollableState?, changeSc: (ScrollableState) -> Unit,
                                 loading: Boolean, isEnd: Boolean, req: suspend () -> Unit,
                                 content: List<SearchItemData>, page: Int) {
    val lstate = scrollState as? LazyStaggeredGridState ?: rememberLazyStaggeredGridState()
    var found by remember {
        mutableStateOf(false)
    }
    DisposableEffect(Unit) {
        GlobalData.forListState = lstate
        changeSc(lstate)
        onDispose {
            GlobalData.forListState = null
        }
    }
    LaunchedEffect(lstate.layoutInfo.visibleItemsInfo) { // 这都什么跟什么啊
        if (lstate.layoutInfo.visibleItemsInfo.any {it.key == "nextLoading"}
            && !loading && !isEnd) {
            found = true
        }
        else if (!loading) {
            found = false
        }
    }
    LaunchedEffect(isEnd) {
        if (!isEnd) {
            found = false
        }
    }
    LaunchedEffect(found) {
        if (!loading && found) {
            req()
        }
    }
    LazyVerticalStaggeredGrid(
        MaxSize(
            216.dp,
            max(6, LocalWindowSize.current.width.value.toInt() / 216 - 2),
            if (LocalWindowSize.current.width < SmallScreenDpLine) 0.dp else 32.dp
        ),
        modifier,
        horizontalArrangement = Arrangement.spacedBy(2.dp, Alignment.CenterHorizontally),
        verticalItemSpacing = 4.dp,
        state = lstate
    ) {
        item("head",span = StaggeredGridItemSpan.FullLine) {
            Text("这里到时候要添加NH与PIXIV的选择器", style = MaterialTheme.typography.headlineMedium)
        }
        if (content.isNotEmpty()) {
            items(content, {it.id}) {
                with(LocalSharedTransitionScope.current) {
                    Box() {
                        ItemInfoCardWithShared(
                            it.thumb,
                            "null",
                            it.title,
                            it.restriction,
                            it.lang.toString().lowercase(),
                            onclick = {
                                GlobalData.nav.push(Routes.Root.Detail(it))
                            },
                            imgLabel = it.thumb
                        )
                    }
                }
            }
            item(if (isEnd) "bottom" else "nextLoading",span = StaggeredGridItemSpan.FullLine) {
                Box(Modifier.fillMaxWidth(), Alignment.Center) {
//                    Button({ss.launch { viewModel.requestNext() }}, enabled = !viewModel.loading) {
//                        Text("加载更多")
//                    }
                    if (isEnd) {
                        Text("没有更多了，页码${page}")
                    } else {
                        CircularProgressIndicator()
                    }
                }
            }
        }
    }
}

@Composable
fun HomeScreen(modifier: Modifier = Modifier, viewModel: HomeViewModel = viewModel { HomeViewModel() }) {
    LaunchedEffect(Unit) {
        if (!viewModel.tempLeave) {
            viewModel.reload()
        } else {
            viewModel.tempLeave = false
        }
    }
    LaunchedEffect(viewModel.local) {
        GlobalData.homeContentSelection = viewModel.local
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
        }
    }
    val ss = rememberCoroutineScope()
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
            StaggeredGridContent(modifier, viewModel.scrollState, { viewModel.scrollState = it },
                viewModel.loading, viewModel.isEnd, { viewModel.requestNext() },
                viewModel.content, viewModel.page)
            if (viewModel.content.isEmpty() && viewModel.loading) {
                CenterCircular()
            }
        }
    }
}

@Preview
@Composable
fun test() {
    HomeScreen()
}