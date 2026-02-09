package com.xlrr.roambendom.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import com.xlrr.roambendom.data.SearchItemData
import com.xlrr.roambendom.network.NHWebHelper
import com.xlrr.roambendom.utils.CenterCircular
import com.xlrr.roambendom.utils.GlobalData
import com.xlrr.roambendom.utils.ItemInfoCard
import com.xlrr.roambendom.utils.LocalWindowSize
import com.xlrr.roambendom.utils.MaxSize
import kotlin.math.max

/*TODO：添加列表以显示nh的主页内容，创建仓库，提交第一版代码，能做到基础可用性验证
* */

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

    suspend fun reload() {
        content.clear()
        page = 0
        requestNext()
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
private fun StaggeredGridContent(modifier: Modifier, viewModel: HomeViewModel) {
    LazyVerticalStaggeredGrid(
        MaxSize(
            216.dp,
            max(6, LocalWindowSize.current.width.value.toInt() / 216 - 2),
            if (LocalWindowSize.current.width < 480.dp) 0.dp else 24.dp
        ),
        modifier,
        horizontalArrangement = Arrangement.spacedBy(2.dp, Alignment.CenterHorizontally),
        verticalItemSpacing = 4.dp,
    ) {
        item(span = StaggeredGridItemSpan.FullLine) {
            Text("这里到时候要添加NH与PIXIV的选择器", style = MaterialTheme.typography.headlineMedium)
        }
        if (viewModel.content.isNotEmpty()) {
            items(viewModel.content) {
                ItemInfoCard(
                    it.thumb,
                    "null",
                    it.title,
                    it.restriction,
                    it.lang.toString().lowercase()
                )
            }
        }
    }
}

@Composable
fun HomeScreen(modifier: Modifier = Modifier, viewModel: HomeViewModel = viewModel { HomeViewModel() }) {
    LaunchedEffect(Unit) {
        viewModel.reload()
    }
    DisposableEffect(Unit) {
        onDispose {
            GlobalData.homeContentSelection = null
        }
    }
    Scaffold(modifier.padding(4.dp).fillMaxSize()) {pd ->
        Box(Modifier.fillMaxSize(), Alignment.TopCenter) {
            StaggeredGridContent(modifier.padding(pd), viewModel)
            if (viewModel.content.isEmpty() && viewModel.loading) {
                CenterCircular()
            }
        }
    }
}