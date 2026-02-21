package com.xlrr.roambendom.ui

import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridScope
import androidx.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridState
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.lazy.staggeredgrid.rememberLazyStaggeredGridState
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.xlrr.roambendom.LocalSharedTransitionScope
import com.xlrr.roambendom.config.ConfigUtil
import com.xlrr.roambendom.data.search.SearchParameterModel
import com.xlrr.roambendom.utils.CenterCircular
import com.xlrr.roambendom.utils.CenterColumnInfo
import com.xlrr.roambendom.utils.GlobalData
import com.xlrr.roambendom.utils.LocalWindowSize
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch
import kotlin.math.ceil
import kotlin.math.max

@Composable
private fun LoadingIndexer(spm: SearchParameterModel, ss: CoroutineScope) {
    Box(Modifier.fillMaxWidth(), Alignment.Center) {
        if (spm.error == null) {
            if (spm.end) {
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
                       }, load: @Composable ((SearchParameterModel, CoroutineScope) -> Unit) =
                           {p1,p2 -> LoadingIndexer(p1,p2) },
                       ss: CoroutineScope = rememberCoroutineScope(), columns: StaggeredGridCells) {
    val shouldLoadMore = remember {
        derivedStateOf {
            val layoutInfo = state.layoutInfo
            val totalItems = layoutInfo.totalItemsCount
            val lastVisibleItem = layoutInfo.visibleItemsInfo.lastOrNull()

            // 确保列表不为空且不在加载中，且最后一个可见项接近末尾
            !searchParameterModel.loading && !searchParameterModel.end
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

    LazyVerticalStaggeredGrid(
        columns,
//        MaxSize(
//            216.dp,
//            max(6, mx.value.toInt() / 216 - 2)
//        ),
        modifier,
        horizontalArrangement = Arrangement.spacedBy(2.dp, Alignment.CenterHorizontally),
        verticalItemSpacing = 4.dp,
        state = state
    ) {
        if (header != null) {
            item("head",span = StaggeredGridItemSpan.SingleLane) {
                header()
            }
        }
        if (searchParameterModel.content.isNotEmpty()) {
            main(searchParameterModel)
            item(if (searchParameterModel.end) "bottom" else "nextLoading",span = StaggeredGridItemSpan.FullLine) {
                load(searchParameterModel, ss)
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
                       }, load: @Composable ((SearchParameterModel, CoroutineScope) -> Unit) =
                           {p1,p2 -> LoadingIndexer(p1,p2) },
                       ss: CoroutineScope = rememberCoroutineScope()) {
    val shouldLoadMore = remember {
        derivedStateOf {
            val layoutInfo = state.layoutInfo
            val totalItems = layoutInfo.totalItemsCount
            val lastVisibleItem = layoutInfo.visibleItemsInfo.lastOrNull()

            // 确保列表不为空且不在加载中，且最后一个可见项接近末尾
            !searchParameterModel.loading && !searchParameterModel.end
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
            item(if (searchParameterModel.end) "bottom" else "nextLoading") {
                load(searchParameterModel,ss)
            }
        }
    }
}

@Composable
fun SearchContent(modifier: Modifier,searchParameterModel: SearchParameterModel, columns: StaggeredGridCells,
                  useList: Boolean = true,
                  header: (@Composable () -> Unit)? = null,
                  listMain: (LazyListScope.(SearchParameterModel) -> Unit) = {spm ->
                      items(spm.content.toList(), {it.id}) {
                          with(LocalSharedTransitionScope.current) {
                              ShowSearchItem(it, false)
                          }
                      }
                  }, gridMain: (LazyStaggeredGridScope.(SearchParameterModel) -> Unit) = {spm ->
                      items(spm.content.toList(), {it.id}) {
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
fun SearchScreen(modifier: Modifier, searchParameterModel: SearchParameterModel) {
    val ss = rememberCoroutineScope()
    var showBtn by remember { mutableStateOf(false) }
    val mx = LocalWindowSize.current.width

    DisposableEffect(Unit) {
        onDispose {
            GlobalData.forListState = null
        }
    }
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
            ), mx < SmallScreenDpLine && !ConfigUtil.forceGrid.state.value
                    && searchParameterModel.configs.searchTarget.state.value == 0,
                ss = ss)
            if (searchParameterModel.content.isEmpty() && searchParameterModel.loading) {
                CenterCircular()
            }
            if (searchParameterModel.content.isEmpty() && searchParameterModel.error != null) {
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
            }
        }
    }
}