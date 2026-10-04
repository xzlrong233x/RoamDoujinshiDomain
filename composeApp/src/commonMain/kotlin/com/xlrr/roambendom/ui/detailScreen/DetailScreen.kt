package com.xlrr.roambendom.ui.detailScreen

import androidx.compose.animation.expandIn
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import coil3.compose.LocalPlatformContext
import com.xlrr.roambendom.LocalAnimatedVisibilityScope
import com.xlrr.roambendom.LocalSharedTransitionScope
import com.xlrr.roambendom.config.LocalPlatformForUI
import com.xlrr.roambendom.config.UIEnablePlatform
import com.xlrr.roambendom.data.ArtworkInfo
import com.xlrr.roambendom.data.SearchItemData
import com.xlrr.roambendom.download.DownloadManager
import com.xlrr.roambendom.download.ImageQuality
import com.xlrr.roambendom.download.downloadItems
import com.xlrr.roambendom.download.prepareDownloadTarget
import com.xlrr.roambendom.manager.FavoriteDataManager
import com.xlrr.roambendom.manager.HistoryDataManager
import com.xlrr.roambendom.model.detail.BaseDetailModel
import com.xlrr.roambendom.model.detail.NHDetailModel
import com.xlrr.roambendom.model.detail.PIXIVDetailModel
import com.xlrr.roambendom.nav.Routes
import com.xlrr.roambendom.network.UrlWithSize
import com.xlrr.roambendom.utils.*
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import roambendom.composeapp.generated.resources.Res
import roambendom.composeapp.generated.resources.artists
import roambendom.composeapp.generated.resources.cancel
import roambendom.composeapp.generated.resources.click_reload
import roambendom.composeapp.generated.resources.download_icon
import roambendom.composeapp.generated.resources.download_label
import roambendom.composeapp.generated.resources.download_normal
import roambendom.composeapp.generated.resources.download_original
import roambendom.composeapp.generated.resources.error_info
import roambendom.composeapp.generated.resources.groups
import roambendom.composeapp.generated.resources.img_download_icon
import roambendom.composeapp.generated.resources.language
import roambendom.composeapp.generated.resources.love_btn_icon
import roambendom.composeapp.generated.resources.love_count
import roambendom.composeapp.generated.resources.loved_btn_icon
import roambendom.composeapp.generated.resources.page
import roambendom.composeapp.generated.resources.read
import roambendom.composeapp.generated.resources.read_from
import roambendom.composeapp.generated.resources.tags

class DetailViewModel() : ViewModel() {
    var content: ArtworkInfo? by mutableStateOf(null)
}

// 我使用SelectionContainer的时候，曾遇到过一个关于select range的报错，不过我难以复现。

@Composable
fun LoveButton(
    searchItemData: SearchItemData,
    artworkInfo: ArtworkInfo?,
    modifier: Modifier = Modifier
) {
    val loved by remember {
        derivedStateOf {
            FavoriteDataManager.content.favorites.find { it.uid == searchItemData.uid() } != null
        }
    }
    IconButton({
        if (!loved) FavoriteDataManager.addItem(
           if (artworkInfo == null)
               searchItemData
           else
               searchItemData.fillSelf(artworkInfo)
        ) else FavoriteDataManager.removeItem(searchItemData.uid())
    }, modifier) {
        Icon(
            painterResource(if (loved) Res.drawable.loved_btn_icon else Res.drawable.love_btn_icon),
            null,
        )
    }
}

@Composable
fun MultiDownloadButton(details: BaseDetailModel, modifier: Modifier = Modifier) {
    var opened by remember { mutableStateOf(false) }
    Box {
        IconButton({ opened = true }, modifier, enabled = details.content?.ugoiraMetadata == null && details.content != null) {
            Icon(painterResource(Res.drawable.download_icon), null)
        }
        MultiSelectDialog(details, opened) { opened = false }
    }
}

@Composable
fun MultiSelectDialog(details: BaseDetailModel, open: Boolean, onDismiss: () -> Unit) {
    if (!open) return
    val content = details.content
    val context = LocalPlatformContext.current
    val scope = rememberCoroutineScope()
    val thumbs = content?.thumbUrls.orEmpty()
    val selected = remember { mutableStateListOf<Int>().apply { addAll(thumbs.indices) } }
    var quality by remember { mutableStateOf(ImageQuality.Normal) }
    var busy by remember { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = { if (!busy) onDismiss() },
        modifier = Modifier.padding(0.dp, 24.dp),
        confirmButton = {
            Button({
                val items = content?.downloadItems(selected, quality).orEmpty()
                if (items.isEmpty()) return@Button
                busy = true
                scope.launch {
                    val target = prepareDownloadTarget(context)
                    if (target == null) {
                        busy = false
                    } else {
                        onDismiss()
                        DownloadManager.submit(context, items, target)
                    }
                }
            }, enabled = !busy && selected.isNotEmpty()) {
                if (busy) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                else Text(stringResource(Res.string.download_label))
            }
        },
        dismissButton = {
            TextButton({ if (!busy) onDismiss() }) { Text(stringResource(Res.string.cancel)) }
        },
        title = {
            if (details is PIXIVDetailModel) {
                var exp by remember { mutableStateOf(false) }
                Box {
                    TextButton({ exp = true }) {
                        Text(stringResource(quality.label()))
                    }
                    DropdownMenu(expanded = exp, onDismissRequest = { exp = false }) {
                        ImageQuality.entries.forEach { q ->
                            DropdownMenuItem({
                                Text(stringResource(q.label()))
                            }, { quality = q; exp = false }, enabled = q != quality)
                        }
                    }
                }
            }
        },
        text = {
            if (content == null) {
                CenterCircular()
            } else {
                LazyVerticalStaggeredGrid(
                    StaggeredGridCells.Adaptive(96.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
                    modifier = Modifier.fillMaxSize().padding(8.dp),
                    verticalItemSpacing = 4.dp
                ) {
                    itemsIndexed(thumbs) { i, uws ->
                        val on = selected.contains(i)
                        Box(Modifier.clip(RoundedCornerShape(8.dp))
                            .clickable { if (on) selected.remove(i) else selected.add(i) }
                            .run {
                                if (on) border(2.dp, Color.Gray, RoundedCornerShape(8.dp)) else this
                            }
                        ) {
                            DefaultErrorHandleImage(uws, Modifier.fillMaxSize())
                            CardLabel(i.toString())
                        }
                    }
                }
            }
        }
    )
}

private fun ImageQuality.label() = when (this) {
    ImageQuality.Normal -> Res.string.download_normal
    ImageQuality.Original -> Res.string.download_original
}

@Composable
fun NHDetail(details: NHDetailModel) {
    with(LocalSharedTransitionScope.current) {
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally) {
            DetailBox() {
                Row(Modifier.fillMaxWidth()) {
                    val url = details.searchItemData.thumb.ifEmpty { details.content?.thumbUrls?.first() }
                    if (!url.isNullOrEmpty()) {
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
                                            stringResource(Res.string.page).format(it.page),
                                            style = typography.titleMedium
                                        )
                                        Text(
                                            stringResource(Res.string.language).format(it.language.toString().lowercase()),
                                            style = typography.titleMedium
                                        )
                                        Text(
                                            stringResource(Res.string.love_count).format(it.likeCount),
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
                            HistoryDataManager.addItem(
                                details.searchItemData.fillSelf(details.content!!),
                                true
                            )
                            GlobalData.nav.push(Routes.Artwork(details.content!!) { i ->
                                HistoryDataManager.changeItemPage(
                                    details.searchItemData.uid(),
                                    i
                                )
                            })
                        }, shape = RoundedCornerShape(20),
                            modifier = Modifier.padding(top = 12.dp).widthIn(128.dp).height(36.dp),
                            enabled = details.isSuccessful()) {
                            Text(
                                HistoryDataManager.map[details.searchItemData.uid()]?.from?.takeIf { it > 0 }
                                    ?.let { stringResource(Res.string.read_from).format(it + 1) }
                                    ?: stringResource(Res.string.read)
                            )
                        }
                        LoveButton(
                            details.searchItemData,
                            details.content,
                            Modifier.padding(top = 12.dp)
                        )
                        MultiDownloadButton(details, Modifier.padding(top = 12.dp))
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
                    MultiCardTagBox(stringResource(Res.string.tags), details.content?.tags.orEmpty()) //TODO: 实现标签搜索
                    MultiCardTagBox(stringResource(Res.string.artists), details.content?.authors.orEmpty())
                    MultiCardTagBox(stringResource(Res.string.groups), details.content?.groups.orEmpty())
                }
            }
        }
    }
}

@Composable
fun DetailScreen(detailModel: BaseDetailModel, modifier: Modifier = Modifier) {
    LaunchedEffect(Unit) {
        if (!detailModel.searchItemData.isDefective()) HistoryDataManager.addItem(detailModel.searchItemData)
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
                            Text(stringResource(Res.string.error_info).format(it.message))
                            Button({
                                co.launch {
                                    detailModel.reloadIfEmpty()
                                }
                            }) {
                                Text(stringResource(Res.string.click_reload))
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