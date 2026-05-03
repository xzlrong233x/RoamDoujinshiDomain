package com.xlrr.roambendom.ui

import androidx.compose.animation.expandIn
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkOut
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PageSize
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.*
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastJoinToString
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.PlatformContext
import coil3.SingletonImageLoader
import coil3.compose.AsyncImagePainter
import coil3.compose.AsyncImagePainter.Companion.DefaultTransform
import coil3.compose.LocalPlatformContext
import coil3.compose.SubcomposeAsyncImage
import coil3.request.ImageRequest
import com.xlrr.roambendom.config.CalUI
import com.xlrr.roambendom.config.ConfigUtil
import com.xlrr.roambendom.config.StateWithUI
import com.xlrr.roambendom.config.UIType
import com.xlrr.roambendom.data.ArtworkInfo
import com.xlrr.roambendom.network.UrlWithSize
import com.xlrr.roambendom.network.defaultImageRequest
import com.xlrr.roambendom.progressive.SharedPainterManager
import com.xlrr.roambendom.utils.*
import com.xlrr.roambendom.utils.MthUtil.hsv
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import net.engawapg.lib.zoomable.ExperimentalZoomableApi
import net.engawapg.lib.zoomable.rememberZoomState
import net.engawapg.lib.zoomable.zoomable
import net.engawapg.lib.zoomable.zoomableWithScroll
import org.jetbrains.compose.resources.painterResource
import roambendom.composeapp.generated.resources.*
import kotlin.math.floor
import kotlin.math.min

class ArtworkViewModel : ViewModel() {
    val turnPageMode = StateWithUI(
        mutableIntStateOf(0), UIType.SingleSegmentedButton(
        "翻页模式",
        listOf("经典", "日漫")
    ))

    val pageDirection = StateWithUI(
        mutableIntStateOf(0), UIType.SingleSegmentedButton(
        "翻页方向",
        listOf("左右", "上下")
    ))

    val twicePage = StateWithUI(mutableStateOf(false), UIType.SwitchUI(
        "双页模式"
    ))

    val oneScreenOnePage = StateWithUI(mutableStateOf(false), UIType.SwitchUI(
        "一屏一页"
    ))

    val hidePageIndexer = StateWithUI(mutableStateOf(false), UIType.SwitchUI(
        "禁用指示器"
    ))

    fun ifToLeft() : Boolean {
        return pageDirection.value == 0 && turnPageMode.value == 1
    }

    fun shouldTwice(): Boolean {
        return twicePage.value && pageDirection.value == 0
    }
}

@Composable
private fun LoadingImage(
    data: String,
    modifier: Modifier = Modifier,
    transform: (AsyncImagePainter.State) -> AsyncImagePainter.State = DefaultTransform,
    contentScale: ContentScale = ContentScale.Fit,
    successfulContent: @Composable (BoxScope.() -> Unit) = {}
) {
    val uws = data.runCatching {
        UrlWithSize.parse(this)
    }.getOrNull()
    if (data.isEmpty() || uws == null) {
        Image(painterResource(Res.drawable.empty_page), "empty page")
        return
    }
    Box {
        ProgressiveImage(
            defaultImageRequest(uws.url, LocalPlatformContext.current),
            modifier,
            contentScale,
            uws.w.toFloat(),
            uws.h.toFloat()
        )
        successfulContent()
    }
}

@Composable
private fun TypicalShowPage(modifier: Modifier, artworkInfo: ArtworkInfo,
                            twicePage: MutableState<Boolean>, turnPageMode: MutableState<Int>,
                            pager: PagerState, pageIndex: @Composable (BoxScope.(Int) -> Unit) = {}) {
    val trans = LocalWindowSize.current.width > LocalWindowSize.current.height
    val mod = if (trans) modifier.fillMaxHeight()
        else modifier.fillMaxWidth()
    val ps = if (twicePage.value) PageSize.Fixed(LocalWindowSize.current.width/2)
        else PageSize.Fill
    val sH = with(LocalDensity.current) {LocalWindowSize.current.height.toPx()}
    val sW = with(LocalDensity.current) {LocalWindowSize.current.width.toPx()} * if (twicePage.value) 0.5f else 1f
    HorizontalPager(pager, mod,
        pageSize = ps,
        reverseLayout = turnPageMode.value == 1
    ) {
        Box(
            Modifier.fillMaxSize(),
            contentAlignment = if (trans && twicePage.value) {
                if (it % 2 == pager.currentPage % 2) Alignment.CenterEnd else Alignment.CenterStart
            } else Alignment.Center
        ) {
            if (artworkInfo.ugoiraMetadata == null) {
                val uws = UrlWithSize.parse(artworkInfo.pageUrls[it])
                LoadingImage(artworkInfo.pageUrls[it], Modifier.run {
                    val testH = uws.h * sW / uws.w
                    if (testH < sH) {
                        width(with(LocalDensity.current) { sW.toDp() })
                            .heightIn(min = with(LocalDensity.current) { testH.toDp() })
                    } else {
                        height(with(LocalDensity.current) {
                            (floor(testH / sH).coerceIn(
                                1f,
                                null
                            ) * sH)
                                .toDp()
                        })
                    }
                }) {
                    pageIndex(it)
                }
            } else {
                artworkInfo.ugoiraMetadata?.let {u -> LoadingImage(u.src) }
            }
        }
    }
}

@Composable
private fun ListShowPage(
    modifier: Modifier, artworkInfo: ArtworkInfo, lazyListState: LazyListState,
    opos: MutableState<Boolean>, pageIndex: @Composable (BoxScope.(Int) -> Unit) = {}
) {
    val sH = with(LocalDensity.current) {LocalWindowSize.current.height.toPx()}
    val sW = with(LocalDensity.current) {LocalWindowSize.current.width.toPx()}
    Box {
        LazyColumn(modifier, lazyListState, horizontalAlignment = Alignment.CenterHorizontally) {
            items(artworkInfo.pageUrls.size) {
                Box() {
                    if (artworkInfo.ugoiraMetadata == null) {
                        val uws = UrlWithSize.parse(artworkInfo.pageUrls[it])
                        LoadingImage(artworkInfo.pageUrls[it], Modifier.run {
                            if (opos.value) {
                                val testH = uws.h * sW / uws.w
                                if (testH < sH) {
                                    width(with(LocalDensity.current) { sW.toDp() })
                                        .heightIn(min = with(LocalDensity.current) { testH.toDp() })
                                } else {
                                    height(with(LocalDensity.current) {
                                        (floor(testH / sH).coerceIn(
                                            1f,
                                            null
                                        ) * sH)
                                            .toDp()
                                    })
                                }
                            }
                            else fillMaxWidth()
                        })
                    } else {
                        artworkInfo.ugoiraMetadata?.let {u -> LoadingImage(u.src) }
                    }
//                    Text("L1", Modifier.align(Alignment.TopStart))
//                    Text("L2", Modifier.align(Alignment.TopEnd))
//                    Text("L3", Modifier.align(Alignment.BottomStart))
//                    Text("L4", Modifier.align(Alignment.BottomEnd))
                }
            }
        }
        pageIndex(-1)
    }
}

@Composable
private fun ShowContent(
    modifier: Modifier,
    artworkInfo: ArtworkInfo,
    pager: PagerState,
    lazyListState: LazyListState,
    twicePage: MutableState<Boolean>,
    turnPageMode: MutableState<Int>,
    pageDirection: MutableState<Int>,
    oneScreenOnePage: MutableState<Boolean>,
    content: @Composable (BoxScope.(Int) -> Unit) = {}
) {
    if (pageDirection.value == 0) {
        TypicalShowPage(
            modifier,
            artworkInfo,
            twicePage,
            turnPageMode,
            pager,
            content
        )
    } else if (pageDirection.value == 1) {
        ListShowPage(
            modifier,
            artworkInfo,
            lazyListState,
            oneScreenOnePage,
            content
        )
    }
}

@OptIn(ExperimentalZoomableApi::class)
@Composable
fun ArtworkViewScreen(artworkInfo: ArtworkInfo, artworkData: ArtworkViewModel = viewModel { ArtworkViewModel() }) {
    if (artworkInfo.page < 1) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("无效的作品内容")
        }
        return
    }
    val context = LocalPlatformContext.current
    val textMeasurer = rememberTextMeasurer(0)
    val preload = remember { Preload(context, textMeasurer) }
    val zzm = rememberZoomState()
    val screenWidth = with(LocalDensity.current) {LocalWindowSize.current.width.toPx()}
    val screenHeight = with(LocalDensity.current) {LocalWindowSize.current.height.toPx()}
    val ss = rememberCoroutineScope()

    val focusReq = remember { FocusRequester() }
    var exp by remember { mutableStateOf(false) }
    var ssexp by remember { mutableStateOf(false) }
    var thumbExpand by remember { mutableStateOf(false) }
    var hide by remember { mutableStateOf(false) }
    var lockHide by remember { mutableStateOf(true) }

    val pager = rememberPagerState { artworkInfo.page }
    val listState = rememberLazyListState()
    val ifZoomed by remember(zzm) {
        derivedStateOf {
            zzm.scale > 1f
        }
    }
    val toPage : (Int) -> Unit = {x->
        if (artworkData.pageDirection.value == 0) {
            pager.requestScrollToPage(x)
        }
        else {
            ss.launch {
                listState.scrollToItem(x)
            }
        }
    }
    fun turnPage(flag: Boolean, shouldTrans: Boolean) {
        val delta = if (artworkData.shouldTwice()) 2 else 1
        var nex = if (flag) -delta else delta
        if (artworkData.pageDirection.value == 0) {
            nex = if (artworkData.turnPageMode.value == 0) nex else -nex
            val pg = pager.currentPage + nex
            toPage(pg.coerceIn(0, pager.pageCount-1))
        }
        else {
            toPage((pager.currentPage + nex).coerceIn(0, pager.pageCount-1))
        }
        lockHide = !shouldTrans
    }

    LaunchedEffect(listState) {
        snapshotFlow { listState.firstVisibleItemIndex }
        .distinctUntilChanged()
        .collect {
            if (artworkData.pageDirection.value == 1) {
                pager.requestScrollToPage(it)
            }
        }

    }
    LaunchedEffect(pager.currentPage) {
        if (artworkData.pageDirection.value == 0) {
            listState.requestScrollToItem(pager.currentPage)
            zzm.reset()
        }
        if (!lockHide) hide = true
        else lockHide = false
        preload.preload(artworkInfo.pageUrls.map { UrlWithSize.parse(it).url }, pager.currentPage)
    }
    DisposableEffect(Unit) {
        GlobalData.hideStatusBar.tryEmit(true)
        onDispose {
            GlobalData.hideStatusBar.tryEmit(false)
        }
    }
    LaunchedEffect(hide) {
        GlobalData.hideStatusBar.emit(hide)
    }

    val onTop: (Offset) -> Unit = { of ->
        val t = if (artworkData.pageDirection.value == 0) of.x else of.y
        val ck = if (artworkData.pageDirection.value == 0) screenWidth else screenHeight
        if (t <= ck * 0.33f) {
            turnPage(true, true)
        } else if (t <= ck * 0.67f) {
            hide = !hide
        } else {
            turnPage(false, true)
        }
    }

    val toMod = if (artworkData.pageDirection.value == 0)
        Modifier.zoomable(zzm, enableOneFingerZoom = ifZoomed,onTap = onTop)
                else
        Modifier.zoomableWithScroll(zzm, enableOneFingerZoom = ifZoomed, onTap = onTop)

    Box(Modifier.fillMaxSize().background(
        Color.hsv(
            MthUtil.rgbToHsv(LocalContentColor.current).copy(
                v = if (isSystemInDarkTheme()) 0.25f else 0.65f
            )
        )
    ).focusRequester(focusReq).focusable()
        .onPreviewKeyEvent { keyEvent ->
        if (keyEvent.type == KeyEventType.KeyUp) {
            when (keyEvent.key) {
                Key.DirectionLeft -> if (artworkData.pageDirection.value == 0) turnPage(
                    flag = true,
                    shouldTrans = true
                )
                Key.DirectionRight -> if (artworkData.pageDirection.value == 0) turnPage(
                    flag = false,
                    shouldTrans = true
                )
                Key.DirectionUp -> if (artworkData.pageDirection.value == 1) turnPage(
                    flag = true,
                    shouldTrans = true
                )
                Key.DirectionDown -> if (artworkData.pageDirection.value == 1) turnPage(
                    flag = false,
                    shouldTrans = true
                )
                Key.VolumeUp -> if (ConfigUtil.enableVolumeTurn.value) {
                    turnPage(
                        flag = true,
                        shouldTrans = true
                    )
                }
                Key.VolumeDown -> if (ConfigUtil.enableVolumeTurn.value) {
                    turnPage(
                        flag = false,
                        shouldTrans = true
                    )
                }
            }
        }
        if ((keyEvent.key == Key.VolumeDown || keyEvent.key == Key.VolumeUp) && ConfigUtil.enableVolumeTurn.value) {
            return@onPreviewKeyEvent true
        }
        false
    }, contentAlignment = Alignment.Center) {
        ShowContent(
            toMod,
            artworkInfo,
            pager,
            listState,
            artworkData.twicePage.state,
            artworkData.turnPageMode.state,
            artworkData.pageDirection.state,
            artworkData.oneScreenOnePage.state
        ) {
            if (
                hide
                && (artworkData.pageDirection.value == 1
                        || it == pager.currentPage + if (artworkData.shouldTwice()) 1 else 0)
                && !artworkData.hidePageIndexer.value
                ) {
                Surface(
                    modifier = Modifier.align(Alignment.TopEnd)
                        .padding(0.dp,2.dp)
                        .run {
                            if (artworkData.pageDirection.value == 1) statusBarsPadding() else this
                        }.semantics { role = Role.Button },
                    color = Color(0f,0f,0f, 0.5f), contentColor = Color.White,
                    shape = RoundedCornerShape(6.dp),
                    onClick = {thumbExpand = !thumbExpand},
                    enabled = artworkInfo.thumbUrls.isNotEmpty()
                ) {
                    val lis = ArrayList<Int>()
                    lis.add(pager.currentPage + 1)
                    if (artworkData.shouldTwice() && artworkInfo.pageUrls.size > 1) {
                        lis.add(pager.currentPage + 2)
                    }
                    Text("${lis.fastJoinToString()} / ${artworkInfo.page}",
                        modifier = Modifier.padding(4.dp, 2.dp),
                        fontWeight = FontWeight.Bold, style = typography.labelMedium)
                }
            }
        }
        CtrlAnimatedVisibility(
            !hide,
            Modifier.fillMaxWidth().align(Alignment.TopCenter),
            enter = fadeIn() + expandIn(expandFrom = Alignment.TopCenter),
            exit = fadeOut() + shrinkOut(shrinkTowards = Alignment.TopCenter),
            label = "artworkTopBar"
        ) {
            Surface(Modifier.fillMaxWidth().align(Alignment.TopCenter),
                color = MaterialTheme.colorScheme.surface.copy(0.35f)) {
                Row(Modifier.fillMaxWidth().statusBarsPadding().height(RootBarHeight)) {
                    IconButton({ GlobalData.nav.defaultBack()}) {
                        Text("返")
                    }
                }
            }
        }
        CtrlAnimatedVisibility(
            !hide,
            Modifier.fillMaxWidth().align(Alignment.BottomCenter),
            enter = fadeIn() + expandIn(expandFrom = Alignment.BottomCenter),
            exit = fadeOut() + shrinkOut(shrinkTowards = Alignment.BottomCenter),
            label = "artworkBottomBar"
        ) {
            Box(contentAlignment = Alignment.BottomCenter) {
                Surface(Modifier.padding(16.dp),
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    val lbtn = pager.currentPage > 0
                    val rbtn = pager.currentPage < pager.pageCount
                    Row(Modifier.padding(8.dp).height(48.dp),
                        verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            {turnPage(true, false)},
                            enabled = if (artworkData.ifToLeft()) rbtn else lbtn
                        ) {
                            Icon(
                                painterResource(Res.drawable.chevron_left_icon),
                                contentDescription = null
                            )
                        }
                        Box() {
                            TextButton({ exp = !exp}) {
                                Text((pager.currentPage + 1).toString())
                                Icon(
                                    painterResource(Res.drawable.down_caret),
                                    contentDescription = null,
                                    modifier = Modifier.offset(0.dp, 1.dp)
                                )
                            }
                            DropdownMenu(exp, {exp = false},
                                Modifier.heightIn(0.dp, (LocalWindowSize.current.height * 0.8f))) {
                                for (x in 0 until artworkInfo.page) {
                                    DropdownMenuItem({Text((x+1).toString())},
                                        {
                                            toPage(x)
                                            exp = false
                                        }, enabled = pager.currentPage != x)
                                }
                            }
                        }
                        IconButton(
                            {turnPage(false, false)},
                            enabled = if (artworkData.ifToLeft()) lbtn else rbtn
                        ) {
                            Icon(
                                painterResource(Res.drawable.chevron_right_icon),
                                contentDescription = null
                            )
                        }
                        Box {
                            IconButton({ssexp = !ssexp}) {
                                Icon(
                                    painterResource(Res.drawable.sim_setting),
                                    contentDescription = null
                                )
                            }
                            DropdownMenu(ssexp, {ssexp = false},
                                modifier = Modifier.padding(8.dp)) {
                                artworkData.let {
                                    CalUI(it.pageDirection)
                                    CalUI(it.turnPageMode, it.pageDirection.value == 0)
                                    CalUI(
                                        it.twicePage,
                                        it.pageDirection.value == 0 && artworkInfo.pageUrls.size > 1
                                    )
                                    CalUI(it.oneScreenOnePage, it.pageDirection.value == 1)
                                    CalUI(it.hidePageIndexer)
                                    Spacer(Modifier.height(6.dp))
                                    CalUI(ConfigUtil.enableVolumeTurn)
                                }
                            }
                        }

                    }
                }
            }
        }
    }
    if (thumbExpand) {
        ThumbDialog(artworkInfo.thumbUrls, toPage) {
            thumbExpand = false
        }
    }
    SideEffect {
        focusReq.requestFocus()
    }
}

@Composable
fun ThumbDialog(lis: List<String>, changePage: (Int) -> Unit, dismiss: () -> Unit) {
    Dialog(onDismissRequest = dismiss) {
        Card(modifier = Modifier
            .fillMaxWidth()
            .height((min(LocalWindowSize.current.height.value * 0.8f, 627f)).dp)
            .padding(16.dp),
            shape = RoundedCornerShape(12.dp),) {
            if (lis.isEmpty()) {
                Box(Modifier.fillMaxSize(), Alignment.Center) {
                    Text("这里什么都没有")
                }
            } else {
                LazyVerticalGrid(
                    GridCells.Adaptive(96.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.fillMaxSize().padding(8.dp)) {
                    items(lis) {x ->
                        SubcomposeAsyncImage(
                            model = defaultImageRequest(x, LocalPlatformContext.current),
                            contentDescription = null,
                            loading = {
                                Image(
                                    painterResource(Res.drawable.loading_jpg),
                                    contentDescription = "Image in loading"
                                )
                            },
                            error = {
                                Column(Modifier.background(Color.DarkGray), horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("WHAT?")
                                    Text(it.result.throwable.message.toString())
                                }
                            },
                            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).clickable {
                                changePage(lis.indexOf(x))
                                dismiss()
                            },
                            contentScale = ContentScale.FillWidth
                        )
                    }
                }
            }
        }
    }
}

class Preload(private val context: PlatformContext, private val textMeasurer: TextMeasurer? = null) {
    private val preloadRequests = mutableSetOf<ImageRequest>()

    fun preload(data: List<String>, cur: Int, preCount: Int = 5) {
        val end = min(data.size, cur + preCount + 1)
        val imageLoader = SingletonImageLoader.get(context)

        for (x in cur + 1 until end) {
            val req = defaultImageRequest(data[x], context)

            if (preloadRequests.contains(req)) {
                continue
            }
            preloadRequests.add(req)
            try {
                if (ConfigUtil.streamDisplay.value) {
                    SharedPainterManager.add(
                        req,
                        context,
                        textMeasurer,
                        req.data.toString().let { it.endsWith(".gif") || it.endsWith(".zip") }
                    )
                }
                imageLoader.enqueue(req)
            } catch (e: Exception) {
                println(e.message)
            }
        }
    }

    fun clear() {
        preloadRequests.clear()
    }
}
