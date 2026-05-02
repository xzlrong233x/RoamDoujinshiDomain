package com.xlrr.roambendom.ui.detailScreen

import androidx.compose.animation.expandIn
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import com.xlrr.roambendom.LocalAnimatedVisibilityScope
import com.xlrr.roambendom.LocalSharedTransitionScope
import com.xlrr.roambendom.config.LocalPlatformForUI
import com.xlrr.roambendom.config.UIEnablePlatform
import com.xlrr.roambendom.data.ArtworkInfo
import com.xlrr.roambendom.model.detail.BaseDetailModel
import com.xlrr.roambendom.model.detail.NHDetailModel
import com.xlrr.roambendom.model.detail.PIXIVDetailModel
import com.xlrr.roambendom.nav.Routes
import com.xlrr.roambendom.network.UrlWithSize
import com.xlrr.roambendom.utils.*
import kotlinx.coroutines.launch

class DetailViewModel() : ViewModel() {
    var content: ArtworkInfo? by mutableStateOf(null)
}

// 我使用SelectionContainer的时候，曾遇到过一个关于select range的报错，不过我难以复现。


@Composable
fun NHDetail(details: NHDetailModel) {
    with(LocalSharedTransitionScope.current) {
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally) {
            DetailBox() {
                Row(Modifier.fillMaxWidth()) {
                    val url = details.searchItemData.thumb.ifEmpty { details.content?.thumbUrls?.first() }
                    if (url != null && url.isNotEmpty()) {
                        DefaultErrorHandleImage(
                            url,
                            Modifier.sharedBounds(
                                rememberSharedContentState(url),
                                LocalAnimatedVisibilityScope.current
                            ).width((LocalWindowSize.current.width.value * 0.382).dp.coerceIn(98.dp, 256.dp))
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                    SelectionContainer {
                        Column {
                            Text(details.searchItemData.title.ifEmpty { details.content?.title ?: "" },
                                style = typography.titleLarge,
                                maxLines = 3, overflow = TextOverflow.Ellipsis)
                            CtrlAnimatedVisibility(
                                details.isSuccessful(),
                                enter = fadeIn(),
                                exit = fadeOut(),
                                label = "smallTitle"
                            ) {
                                details.content?.let {
                                    Column {
                                        if (it.altitle.isNotBlank()) Text(
                                            it.altitle,
                                            color = Color(0.5f, 0.5f, 0.5f),
                                            style = typography.titleSmall,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            "#${details.searchItemData.id}",
                                            Modifier.padding(0.dp, 2.dp),
                                            style = typography.bodyLarge
                                        )
                                        if (it.page > 0) Text(
                                            "页数：${it.page}",
                                            style = typography.titleMedium
                                        )
                                        Text(
                                            "语言：${it.language.toString().lowercase()}",
                                            style = typography.titleMedium
                                        )
                                        Text(
                                            "喜好数量：${it.likeCount.toString().lowercase()}",
                                            style = typography.titleMedium
                                        )
                                        if (it.time > 0) {
                                            Text(
                                                TimeUtil.formatTime(it.time),
                                                style = typography.titleMedium
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                CtrlAnimatedVisibility(
                    details.isSuccessful(),
                    modifier = Modifier.fillMaxWidth(),
                    enter = fadeIn() + expandIn(expandFrom = Alignment.TopCenter),
                    exit = fadeOut(),
                    label = "DetailToolBar"
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Button({
                            if (details.content == null) {
                                return@Button
                            }
                            GlobalData.historyData.addItem(
                                details.searchItemData.fillSelf(details.content!!),
                                true
                            )
                            GlobalData.nav.push(Routes.Artwork(details.content!!))
                        }, shape = RoundedCornerShape(20),
                            modifier = Modifier.padding(top = 12.dp).width(128.dp).height(36.dp),
                            enabled = details.isSuccessful()) {
                            Text("阅读")
                        }
//                    IconButton({loved = !loved}, ) {
//                        Icon(
//                            painterResource(if (loved) Res.drawable.loved_btn_icon else Res.drawable.love_btn_icon),
//                            contentDescription = null
//                        )
//                    } 类似收藏的功能，还没想好怎么做。
                    }
                }
            }
            CtrlAnimatedVisibility(
                details.isSuccessful(),
                modifier = Modifier.fillMaxWidth(),
                enter = fadeIn() + expandIn(expandFrom = Alignment.TopCenter),
                exit = fadeOut(),
                label = "otherInfo"
            ) {
                Column(Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally) {
                    MultiCardTagBox("标签", details.content?.tags.orEmpty()) //TODO: 以后做固定搜索页，以实现标签搜索
                    MultiCardTagBox("作者", details.content?.authors.orEmpty())
                    MultiCardTagBox("团体", details.content?.groups.orEmpty())
                }
            }
        }
    }
}

@Composable
fun DetailScreen(detailModel: BaseDetailModel, modifier: Modifier = Modifier, details: DetailViewModel = viewModel { DetailViewModel() }) {
    LaunchedEffect(Unit) {
        if (!detailModel.searchItemData.isDefective()) GlobalData.historyData.addItem(detailModel.searchItemData)
        detailModel.reloadIfEmpty()
    }
    val co = rememberCoroutineScope()
    Scaffold(modifier.fillMaxSize()) { pd ->
        CtrlPullToRefreshBox(
            detailModel.loading,
            {co.launch { detailModel.refresh() }},
            Modifier.fillMaxSize().padding(pd),
            contentAlignment =  Alignment.TopCenter
        ) {
            if (detailModel.error != null) {
                detailModel.error?.let {
                    Box(Modifier.fillMaxSize(), Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("错误：${it.message}")
                            Button({
                                co.launch {
                                    detailModel.reloadIfEmpty()
                                }
                            }) {
                                Text("点我重载")
                            }
                        }
                    }
                }
            } else {
                when (detailModel) {
                    is NHDetailModel -> NHDetail(detailModel)
                    is PIXIVDetailModel -> PIXIVDetail(detailModel)
                }
            }
            if (detailModel.loading && LocalPlatformForUI.current == UIEnablePlatform.DESKTOP) {
                CenterCircular()
            }
        }
    }
}

@Composable
private fun MultiCardTagBox(head: String,list: List<String>) {
    if (list.isNotEmpty()) {
        DetailBox {
            Text(head,
                style = typography.headlineMedium)
            Spacer(Modifier.height(12.dp))
            SelectionContainer {
                FlowRow(Modifier.fillMaxWidth()) {
                    for (x in list) {
                        CardLabel(
                            x,
                            shape = RoundedCornerShape(15),
                            sufColor = Color(142, 142, 142, 255),
                            fontColor = Color.Black,
                            fontStyle = typography.bodyLarge,
                            modifier = Modifier.padding(4.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun DetailBox(color: Color = MaterialTheme.colorScheme.surfaceContainerLow, content: @Composable () -> Unit) {
    Surface(Modifier.padding(12.dp).fillMaxWidth(0.96f),
        color = color, shape = RoundedCornerShape(12.dp)) {
        Column(
            Modifier.fillMaxSize()
                .padding(8.dp)
        ) {
            content()
        }
    }
}

@Composable
@Preview
fun DTTest() {
    Surface(Modifier.fillMaxSize()) {
        Box(Modifier) {
            Surface(Modifier.widthIn(0.dp, 312.dp).padding(12.dp).align(Alignment.TopCenter), color = MaterialTheme.colorScheme.surfaceContainerLow,
                shape = RoundedCornerShape(12.dp)) {
                LazyColumn(Modifier) {
                    item {
                        val s = UrlWithSize.parse("ht[w1h2]")
                        Text(s.toString())
                    }
                    item {
                        Text("12222")
                    }
                    item {
                        Text("ffddddd")
                    }
                }
            }
        }
    }
}