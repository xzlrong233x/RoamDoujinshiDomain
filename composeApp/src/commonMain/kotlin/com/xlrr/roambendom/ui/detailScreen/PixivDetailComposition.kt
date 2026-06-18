package com.xlrr.roambendom.ui.detailScreen

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.*
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.LocalPlatformContext
import com.xlrr.roambendom.LocalSharedTransitionScope
import com.xlrr.roambendom.config.ConfigUtil
import com.xlrr.roambendom.data.ArtworkInfo
import com.xlrr.roambendom.data.CRestriction
import com.xlrr.roambendom.data.getColor
import com.xlrr.roambendom.data.pixiv.UgoiraMetadata
import com.xlrr.roambendom.manager.HistoryDataManager
import com.xlrr.roambendom.model.detail.PIXIVDetailModel
import com.xlrr.roambendom.nav.Routes
import com.xlrr.roambendom.network.UrlWithSize
import com.xlrr.roambendom.network.defaultImageRequest
import com.xlrr.roambendom.ui.Preload
import com.xlrr.roambendom.ui.ThumbDialog
import com.xlrr.roambendom.utils.*
import com.xlrr.roambendom.utils.MthUtil.adaptiveSize
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch
import net.engawapg.lib.zoomable.ZoomState
import net.engawapg.lib.zoomable.rememberZoomState
import net.engawapg.lib.zoomable.zoomable
import org.jetbrains.compose.resources.painterResource
import roambendom.composeapp.generated.resources.Res
import roambendom.composeapp.generated.resources.book
import roambendom.composeapp.generated.resources.img_download_icon

@Composable
fun ImageDialog(
    urlWithSize: UrlWithSize?,
    onDismiss: () -> Unit,
    zm: ZoomState,
    ugoiraMetadata: UgoiraMetadata? = null,
    realLoad: SnapshotStateList<String>
) {
    val screenWidth = with(LocalDensity.current) {
        LocalWindowSize.current.width.toPx()
    }
    val screenHeight = with(LocalDensity.current) { LocalWindowSize.current.height.toPx()}
    val fcq = remember { FocusRequester() }

    with(LocalSharedTransitionScope.current) {
        AnimatedContent(urlWithSize, transitionSpec = {
            fadeIn() togetherWith fadeOut()
        }, label = "image-dialog") { uws ->
            Box(
                Modifier.fillMaxSize().onPreviewKeyEvent {
                    if (it.key == Key.Escape && it.type == KeyEventType.KeyUp) {
                        onDismiss()
                        return@onPreviewKeyEvent true
                    }
                    false
                }
            ) {
                if (uws != null) {
                    DisposableEffect(Unit) {
                        GlobalData.requestToHideRailBtn()
                        onDispose {
                            GlobalData.requestToShowRailBtn()
                        }
                    }
                    val n by remember(realLoad) {
                        derivedStateOf {
                            realLoad.contains(uws.url)
                        }
                    }
                    Box(Modifier.fillMaxSize().focusRequester(fcq).focusable().clickable {
                        onDismiss()
                    }.background(Color.Black.copy(alpha = 0.75f)), Alignment.Center) {
                        SharedImage(uws, Modifier.sharedElement(
                            rememberSharedContentState(uws.url.split("/").last()),
                            this@AnimatedContent,
                        ).run {
                            if (screenWidth > screenHeight) fillMaxHeight()
                            else fillMaxWidth()
                        }.zoomable(
                            zm,
                            onTap = {
                                onDismiss()
                            }
                        ), n, ugoiraMetadata)
                        Row(Modifier.align(Alignment.BottomEnd).padding(14.dp)) {
                            Coil3SaveImageButton(
                                defaultImageRequest(uws.url, LocalPlatformContext.current),
                                painterResource(Res.drawable.img_download_icon)
                            )
                        }
                        if (!n) {
                            Row(Modifier.align(Alignment.BottomStart).padding(14.dp)) {
                                Button({ realLoad.add(uws.url) },
                                    colors = ButtonDefaults.buttonColors().copy(
                                        Color.Gray.copy(alpha = 0.75f)
                                    )) {
                                    Text("加载原图", color = Color.White)
                                }
                            }
                        }
                    }
                    SideEffect {
                        fcq.requestFocus()
                    }
                }
            }
        }
    }
}

@Composable
private fun SharedImage(
    uws: UrlWithSize,
    modifier: Modifier,
    ori: Boolean = false,
    ugoiraMetadata: UgoiraMetadata? = null,
    onSizeKnown: ((Int, Int) -> Unit)? = null
) {
    val ur = if (!ori || uws.oriUrl.isEmpty()) uws.url else uws.oriUrl
    ProgressiveImage(
        defaultImageRequest(
            ur,
            LocalPlatformContext.current
        ).run {
            ugoiraMetadata?.let {
                return@run newBuilder().also { ib->ib.extras[framesKey] = it.frames }.build()
            }
            return@run this
        },
        modifier,
        width = uws.w.toFloat(),
        height = uws.h.toFloat(),
        onSizeKnown = onSizeKnown
    )
}

private fun LazyListScope.imagesOrAnimatedImage(
    info: ArtworkInfo,
    pageSizes: SnapshotStateList<UrlWithSize>,
    screenWidth: Float,
    screenHeight: Float,
    showAllPage: Boolean,
    click: (UrlWithSize) -> Unit,
    boxItem: @Composable (Int) -> Unit
) {
    if (info.ugoiraMetadata == null) {
        items(
            if (!showAllPage) 1 else info.page,
            { "ImgPage$it" }) {
            Box(Modifier.fillMaxWidth(), Alignment.Center) {
                pageSizes[it].let { uws ->
                    SharedImage(uws, Modifier.adaptiveSize(
                        screenWidth,
                        screenHeight,
                        uws.w.toFloat(),
                        uws.h.toFloat(),
                        LocalDensity.current,
                    ).clickable {
                        click(uws)
                    },
                        onSizeKnown = { w, h ->
                            if (!(pageSizes[it].w == w && pageSizes[it].h == h)) {
                                pageSizes[it] = pageSizes[it].copy(w = w, h = h)
                            }
                        }
                    )
                }

                boxItem(it)
            }
        }
    } else {
        item {
            info.ugoiraMetadata?.let {
                Box(Modifier.fillMaxWidth(), Alignment.Center) {
                    val u = UrlWithSize(
                        it.src,
                        it.width,it.height, it.originalSrc
                    )
                    SharedImage(u, Modifier.run {
                        if (screenWidth >= screenHeight) {
                            height(with(LocalDensity.current) {screenHeight.dp})
                        } else {
                            width(with(LocalDensity.current) {screenWidth.dp})
                        }
                    }.clickable {
                        click(u)
                    }, ugoiraMetadata = it)
                }
            }
        }
    }
}

val PIXIV_DETAIL_MAIN_HORIZONTAL = 12.dp
val PIXIV_DETAIL_MAIN = 1104.dp

@Composable
fun PIXIVDetail(details: PIXIVDetailModel) {
    val state = rememberLazyListState()
    val context = LocalPlatformContext.current
    val tm = rememberTextMeasurer(0)
    val preload = remember { Preload(context, tm) }
    val screenWidth = with(LocalDensity.current) {
        (LocalWindowSize.current.width.coerceIn(null, PIXIV_DETAIL_MAIN) - PIXIV_DETAIL_MAIN_HORIZONTAL * 2).toPx()
    }
    val ss = rememberCoroutineScope()
    val screenHeight = with(LocalDensity.current) { LocalWindowSize.current.height.toPx()}
    val isThereImg by remember(state) {
        derivedStateOf {
            state.layoutInfo.visibleItemsInfo.any {
                it.key.toString().contains("ImgPage")
            }
        }
    }
    var thumbExpand by remember { mutableStateOf(false) }
    var page by remember { mutableIntStateOf(0) }
    var urlWithSize: UrlWithSize? by remember { mutableStateOf(null) }
    val zoom = rememberZoomState()
    val fcq = remember { FocusRequester() }
    val rlPage = remember { mutableStateListOf<String>() }

    val pageSizes = remember(details.content) {
        mutableStateListOf<UrlWithSize>().also { list ->
            details.content?.pageUrls?.forEach { list.add(UrlWithSize.parse(it)) }
        }
    }

    LaunchedEffect(state) {
        snapshotFlow { state.maxVisibleItem() }
            .filter { it?.key.toString().contains("ImgPage") }
            .distinctUntilChanged()
            .collect {
                page = ("\\d+".toRegex().find(it?.key.toString())?.value?.toInt() ?: 0)
            }

    }
    LaunchedEffect(page) {
        details.content?.let {
            preload.preload(it.pageUrls.map { s -> UrlWithSize.parse(s).url }, page,)
        }
    }
    LaunchedEffect(details.loading) {
        if (!details.loading && details.error == null) {
            details.content?.let {
                HistoryDataManager.addItem(details.searchItemData.fillSelfIfDefective(it), true)
            }
        }
    }

    var showAllDes by remember { mutableStateOf(false) }
    var moreBtn by remember { mutableStateOf(false) }
    var showAllPage by remember { mutableStateOf(false) }
    Box() {
        if (!details.loading && details.content != null) {
            Box(Modifier.align(Alignment.TopCenter).focusRequester(fcq).focusable()
                .onPreviewKeyEvent {
                    if (ConfigUtil.enableVolumeTurn.value) {
                        if (it.type == KeyEventType.KeyUp) {
                            var dt = 0
                            if (it.key == Key.VolumeDown) {
                                dt = 1
                            } else if (it.key == Key.VolumeUp) {
                                dt = -1
                            }
                            val n = (page + dt).coerceIn(0, details.content?.page)
                            if (n != page && showAllPage) {
                                ss.launch {
                                    state.animateScrollToItem(n)
                                }
                                return@onPreviewKeyEvent true
                            }
                        }
                        else if ((it.key == Key.VolumeDown || it.key == Key.VolumeUp)) {
                            return@onPreviewKeyEvent true
                        }
                    }
                    false
                }) {
                Surface(
                    Modifier.width(PIXIV_DETAIL_MAIN).widthIn(0.dp, PIXIV_DETAIL_MAIN)
                        .padding(PIXIV_DETAIL_MAIN_HORIZONTAL, 0.dp).align(Alignment.TopCenter),
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerLow
                ) {
                    LazyColumn(Modifier, state) {
                        imagesOrAnimatedImage(
                            details.content!!, pageSizes,
                            screenWidth,
                            screenHeight,
                            showAllPage,
                            {
                                if (urlWithSize == null) {
                                    urlWithSize = it
                                }
                            }
                        ) {
                            if (!showAllPage && it == 0 && (details.content?.page ?: 0) > 1) {
                                Button(
                                    {
                                        showAllPage = true
                                        ss.launch {
                                            state.animateScrollToItem(0)
                                        }
                                    }, Modifier.align(Alignment.BottomCenter)
                                ) {
                                    Text("查看全部")
                                }
                            }
                        }
                        item {
                            Row(Modifier.fillMaxWidth()) {
                                IconButton({
                                    if (details.content == null) {
                                        return@IconButton
                                    }
                                    GlobalData.nav.push(Routes.Artwork(details.content!!.copy()))
                                }, enabled = details.content?.ugoiraMetadata == null) {
                                    Icon(
                                        painterResource(Res.drawable.book),
                                        "read in artwork view screen"
                                    )
                                }
                                LoveButton(
                                    details.searchItemData,
                                    details.content,
                                    Modifier
                                )
                            }
                        }
                        item("Title") {
                            SelectionContainer {
                                Text(
                                    details.searchItemData.title.ifEmpty { details.content?.title.toString() },
                                    Modifier.padding(6.dp, 0.dp),
                                    style = MaterialTheme.typography.headlineMedium
                                )
                            }
                        }
                        item("Description") {
                            Column(Modifier.fillMaxWidth().padding(8.dp, 3.dp)) {
                                SelectionContainer {
                                    Text(
                                        details.content?.description.toString(),
                                        style = MaterialTheme.typography.bodyMedium,
                                        maxLines = if (showAllDes) Int.MAX_VALUE else 6,
                                        overflow = TextOverflow.Ellipsis,
                                        onTextLayout = {
                                            moreBtn = it.hasVisualOverflow
                                        }
                                    )
                                }
                                if (moreBtn) {
                                    Text("展示更多", Modifier.align(Alignment.End).clickable {
                                        showAllDes = true
                                    }, color = (if (isSystemInDarkTheme()) Color.White else Color.Black).copy(0.5f))
                                }
                            }
                        }
                        item("Tags") {
                            SelectionContainer {
                                FlowRow(
                                    Modifier.fillMaxWidth().padding(6.dp, 4.dp),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    details.content?.let { ct ->
                                        if (ct.restriction != CRestriction.Normal) {
                                            Text(ct.restriction.toString(), fontWeight = FontWeight.Bold,
                                                color = ct.restriction.getColor())
                                        }
                                        if (ct.ai) {
                                            Text("AI生成", fontWeight = FontWeight.Bold)
                                        }
                                        ct.tags.forEach {
                                            if (!it.contains("R-18G*".toRegex())) Text("#$it")
                                        }
                                    }
                                }
                            }
                        }
                        item("IdText") {
                            SelectionContainer {
                                Text(
                                    "#${details.searchItemData.id}",
                                    Modifier.padding(6.dp, 2.dp),
                                    style = MaterialTheme.typography.bodyLarge
                                )
                            }
                        }
                        item("LikeCount") {
                            SelectionContainer {
                                Text(
                                    "收藏数：${details.content!!.likeCount}",
                                    Modifier.padding(6.dp, 2.dp)
                                )
                            }
                        }
                        item("Time") {
                            SelectionContainer {
                                Text(
                                    TimeUtil.formatTime(details.content!!.time),
                                    Modifier.padding(6.dp, 0.dp)
                                )
                            }
                        }
                        item("Author") {
                            SelectionContainer {
                                details.content?.let {
                                    Text(
                                        "${it.authors.first()} (${it.authors.last()})",
                                        Modifier.padding(6.dp, 6.dp).clickable {
                                            GlobalData.nav.pushAuthorSearch(
                                                it.authors.last(),
                                                it.authors.first()
                                            )
                                        }
                                    )
                                }

                            }
                        }
                        item("line1") {
                            HorizontalDivider(Modifier.padding(vertical = 8.dp))
                        }
                        item("recommends") {
                            Box(Modifier.fillMaxWidth()) {
                                Button({
                                    GlobalData.nav.pushRecommend(details)
                                }, Modifier.align(Alignment.Center).padding(vertical = 12.dp)) {
                                    Text("查看推荐")
                                }
                            } // TODO: 单页面加载。
                        }
                    }
                }
                if (isThereImg && showAllPage) {
                    Surface(
                        modifier = Modifier.align(Alignment.TopEnd)
                            .padding(0.dp, 2.dp).semantics { role = Role.Button },
                        color = Color(0f, 0f, 0f, 0.5f), contentColor = Color.White,
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(6.dp),
                        onClick = { thumbExpand = !thumbExpand },
                        enabled = details.content?.thumbUrls?.isNotEmpty() == true
                    ) {
                        Text(
                            "${page + 1} / ${details.content?.page}",
                            modifier = Modifier.padding(4.dp, 2.dp),
                            fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium
                        )
                    }
                }
            }
            if (thumbExpand) {
                ThumbDialog(details.content?.thumbUrls ?: listOf(), {
                    ss.launch { state.animateScrollToItem(it) }
                }, { thumbExpand = false })
            }
            ImageDialog(urlWithSize,
                {
                    urlWithSize = null
                    ss.launch { zoom.reset() }
                },
                zoom, details.content?.ugoiraMetadata, rlPage)
            SideEffect {
                if (urlWithSize == null) {
                    fcq.requestFocus()
                }
            }
        }
    }
}