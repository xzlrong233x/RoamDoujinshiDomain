package com.xlrr.roambendom.ui

import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.lazy.staggeredgrid.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.xlrr.roambendom.LocalSharedTransitionScope
import com.xlrr.roambendom.config.ConfigUtil
import com.xlrr.roambendom.model.search.SearchParameterModel
import com.xlrr.roambendom.utils.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch
import kotlin.math.ceil
import kotlin.math.max

@Composable
private fun LoadingIndexer(spm: SearchParameterModel, ss: CoroutineScope) {
    Box(Modifier.fillMaxWidth(), Alignment.Center) {
        if (spm.error == null) {
            if (spm.completed) {
                Text("没有更多了，页码${spm.page}")
            } else {
                CircularProgressIndicator()
            }
        } else {
            spm.error?.let {
                CenterColumnInfo {
                    Text("错误：${it.message}")
                    Button({
                        ss.launch {
                            spm.reload()
                        }
                    }) {
                        Text("点我重载")
                    }
                }
            }
        }
    }
}

@Composable
fun InfiniteScrollStaggeredGrid(modifier: Modifier, state: LazyStaggeredGridState, searchParameterModel: SearchParameterModel,
                       header: (@Composable () -> Unit)? = null,
                       main: (LazyStaggeredGridScope.(SearchParameterModel) -> Unit) = { spm ->
                           items(spm.content.toList(), {it.id}) {
                               with(LocalSharedTransitionScope.current) {
                                   ShowSearchItem(it, true)
                               }
                           }
                       }, load: @Composable ((SearchParameterModel, CoroutineScope) -> Unit)? =
                           {p1,p2 -> LoadingIndexer(p1,p2) },
                       ss: CoroutineScope = rememberCoroutineScope(), columns: StaggeredGridCells) {
    val shouldLoadMore = remember {
        derivedStateOf {
            val layoutInfo = state.layoutInfo
            val totalItems = layoutInfo.totalItemsCount
            val lastVisibleItem = layoutInfo.visibleItemsInfo.lastOrNull()

            // 确保列表不为空且不在加载中，且最后一个可见项接近末尾
            !searchParameterModel.loading && !searchParameterModel.completed
                    && lastVisibleItem != null && lastVisibleItem.index >= totalItems - 1
                    && lastVisibleItem.key == "nextLoading"
        }
    }
    LaunchedEffect(Unit) {
        snapshotFlow { shouldLoadMore.value }
            .filter { it }          // 只处理 true 的情况
            .collect {
                searchParameterModel.request()
            }
    }

    Box(Modifier.widthIn(0.dp, 1272.dp)) {
        LazyVerticalStaggeredGrid(
            columns,
            modifier,
            horizontalArrangement = Arrangement.spacedBy(2.dp, Alignment.CenterHorizontally),
            verticalItemSpacing = 4.dp,
            state = state
        ) {
            if (header != null) {
                item("head", span = StaggeredGridItemSpan.FullLine) {
                    header()
                }
            }
            if (searchParameterModel.content.isNotEmpty()) {
                main(searchParameterModel)
                if (load != null) {
                    item(
                        if (searchParameterModel.completed) "bottom" else "nextLoading",
                        span = StaggeredGridItemSpan.FullLine
                    ) {
                        load(searchParameterModel, ss)
                    }
                }
            }
        }
    }
}

@Composable
fun InfiniteScrollList(modifier: Modifier, state: LazyListState, searchParameterModel: SearchParameterModel,
                       header: (@Composable () -> Unit)? = null,
                       main: (LazyListScope.(SearchParameterModel) -> Unit) = {spm ->
                           items(spm.content.toList(), {it.id}) {
                               with(LocalSharedTransitionScope.current) {
                                   ShowSearchItem(it, false)
                               }
                           }
                       }, load: @Composable ((SearchParameterModel, CoroutineScope) -> Unit)? =
                           {p1,p2 -> LoadingIndexer(p1,p2) },
                       ss: CoroutineScope = rememberCoroutineScope()) {
    val shouldLoadMore = remember {
        derivedStateOf {
            val layoutInfo = state.layoutInfo
            val totalItems = layoutInfo.totalItemsCount
            val lastVisibleItem = layoutInfo.visibleItemsInfo.lastOrNull()

            // 确保列表不为空且不在加载中，且最后一个可见项接近末尾
            !searchParameterModel.loading && !searchParameterModel.completed
                    && lastVisibleItem != null && lastVisibleItem.index >= totalItems - 1
                    && lastVisibleItem.key == "nextLoading"
        }
    }
    LaunchedEffect(Unit) {
        snapshotFlow { shouldLoadMore.value }
            .filter { it }          // 只处理 true 的情况
            .collect {
                searchParameterModel.request()
            }
    }

    LazyColumn(modifier, state, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        if (header != null) {
            item(key = "head") {
                header()
            }
        }
        if (searchParameterModel.content.isNotEmpty()) {
            main(searchParameterModel)
            if (load != null){
                item(if (searchParameterModel.completed) "bottom" else "nextLoading") {
                    load(searchParameterModel, ss)
                }
            }
        }
    }
}

@Composable
fun SearchContent(modifier: Modifier,searchParameterModel: SearchParameterModel, columns: StaggeredGridCells,
                  useList: Boolean = true,
                  header: (@Composable () -> Unit)? = null,
                  listMain: (LazyListScope.(SearchParameterModel) -> Unit) = {spm ->
                      items(spm.content.distinct(), {it.id}) {
                          with(LocalSharedTransitionScope.current) {
                              ShowSearchItem(it, false)
                          }
                      }
                  }, gridMain: (LazyStaggeredGridScope.(SearchParameterModel) -> Unit) = {spm ->
                      items(spm.content.distinct(), {it.id}) {
                          with(LocalSharedTransitionScope.current) {
                              ShowSearchItem(it, true)
                          }
                      }
                  },
                  load: @Composable ((SearchParameterModel, CoroutineScope) -> Unit) = {p1,p2 ->
                      LoadingIndexer(p1,p2)
                  },
                  ss: CoroutineScope = rememberCoroutineScope()) {
    if (useList) {
        val lstate = searchParameterModel.scrollState as? LazyListState ?: rememberLazyListState()
        searchParameterModel.scrollState = lstate
        GlobalData.forListState = lstate
        InfiniteScrollList(modifier, lstate, searchParameterModel, header, listMain, load, ss)
    } else {
        val lstate = searchParameterModel.scrollState as? LazyStaggeredGridState ?: rememberLazyStaggeredGridState()
        searchParameterModel.scrollState = lstate
        GlobalData.forListState = lstate
        InfiniteScrollStaggeredGrid(modifier, lstate, searchParameterModel, header, gridMain, load, ss, columns)
    }
}

@Composable
fun StandardSearchLikeWithUp(searchParameterModel: SearchParameterModel,
                         modifier: Modifier, content: @Composable BoxScope.() -> Unit) {
    val ss = rememberCoroutineScope()
    val showBtn by remember(GlobalData.forListState) {
        derivedStateOf {
            GlobalData.forListState != null
                    && GlobalData.forListState?.scrollIndicatorState?.scrollOffset?.let { it > 0 } == true
        }
    }

    LaunchedEffect(Unit) {
        if (searchParameterModel.content.isEmpty() && !searchParameterModel.completed) {
            searchParameterModel.reload()
        }
    }

    Scaffold(
        modifier.fillMaxSize(),
        floatingActionButton = {
            if (showBtn) { //TODO: 先占位，以后再改
                FloatingActionButton({ ss.launch { GlobalData.forListState?.animateScrollBy(-Float.MAX_VALUE) } }) {
                    Text("UP")
                }
            }
        }) {pd ->
        CtrlPullToRefreshBox(
            searchParameterModel.loading && searchParameterModel.refreshing,
            {ss.launch { searchParameterModel.refresh() }},
            Modifier.fillMaxSize().padding(pd),
            contentAlignment =  Alignment.TopCenter
        ) {
            content()
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
//        Box(Modifier.fillMaxSize().padding(pd), Alignment.TopCenter) {
//
//        }
    }
}

@Composable
fun SearchScreen(modifier: Modifier, searchParameterModel: SearchParameterModel) {
    val ss = rememberCoroutineScope()
    val mx = LocalWindowSize.current.width
    StandardSearchLikeWithUp(searchParameterModel, modifier) {
        SearchContent(modifier, searchParameterModel, StaggeredGridCells.Fixed(
            ceil(mx.value / 216f).coerceIn(1f, max(6f, mx.value / 216 - 2)).toInt()
        ), mx < SmallScreenDpLine && !ConfigUtil.forceGrid.value
                && searchParameterModel.configs.searchTarget.realValue == 0,
            ss = ss)
    }
}