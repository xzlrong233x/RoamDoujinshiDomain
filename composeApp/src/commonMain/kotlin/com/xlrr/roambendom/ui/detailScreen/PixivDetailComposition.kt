package com.xlrr.roambendom.ui.detailScreen

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.compose.LocalPlatformContext
import com.xlrr.roambendom.LocalSharedTransitionScope
import com.xlrr.roambendom.data.SearchItemData
import com.xlrr.roambendom.nav.Routes
import com.xlrr.roambendom.network.UrlWithSize
import com.xlrr.roambendom.network.defaultImageRequest
import com.xlrr.roambendom.ui.Preload
import com.xlrr.roambendom.ui.ThumbDialog
import com.xlrr.roambendom.utils.CtrlAnimatedVisibility
import com.xlrr.roambendom.utils.GlobalData
import com.xlrr.roambendom.utils.LocalWindowSize
import com.xlrr.roambendom.utils.visibleItems
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.painterResource
import roambendom.composeapp.generated.resources.Res
import roambendom.composeapp.generated.resources.book
import roambendom.composeapp.generated.resources.empty_page
import roambendom.composeapp.generated.resources.loading_jpg
import kotlin.math.floor

@Composable
fun ImageDialog(urlWithSize: UrlWithSize?, onDismiss: () -> Unit) {
    val screenWidth = with(LocalDensity.current) {
        LocalWindowSize.current.width.toPx()
    }
    val screenHeight = with(LocalDensity.current) { LocalWindowSize.current.height.toPx()}

    with(LocalSharedTransitionScope.current) {
        AnimatedContent(urlWithSize, transitionSpec = {
            fadeIn() togetherWith fadeOut()
        }, label = "image-dialog") { uws ->
            Box(
                Modifier.fillMaxSize()
            ) {
                if (uws != null) {
                    Box(Modifier.fillMaxSize().clickable {
                        onDismiss()
                    }.background(Color.Black.copy(alpha = 0.5f)), Alignment.Center) {
                        SharedImage(uws, this@AnimatedContent, Modifier.sharedElement(
                            rememberSharedContentState(uws.url.split("/").last()),
                            this@AnimatedContent,
                        ).run {
                            if (screenWidth > screenHeight) fillMaxHeight()
                            else fillMaxWidth()
                        })
                    }
                }
            }
        }
    }
}

@Composable
private fun SharedImage(uws: UrlWithSize, aniScope: AnimatedVisibilityScope, modifier: Modifier) {
    with(LocalSharedTransitionScope.current){
        Box(
            Modifier.sharedBounds(
                rememberSharedContentState(uws.url.split("/").last() + "-bound"),
                aniScope,
                resizeMode = SharedTransitionScope.ResizeMode.scaleToBounds()
            )
        ) {
            AsyncImage(
                model = defaultImageRequest(
                    uws.url,
                    LocalPlatformContext.current
                ),
                filterQuality = FilterQuality.Medium,
                contentDescription = null,
                placeholder = painterResource(Res.drawable.loading_jpg),
                error = painterResource(Res.drawable.empty_page),
                modifier = modifier
            )
        }
    }
}

@Composable
fun PIXIVDetail(searchItemData: SearchItemData, details: DetailViewModel) {
    val state = rememberLazyListState()
    val context = LocalPlatformContext.current
    val preload = remember { Preload(context) }
    val screenWidth = with(LocalDensity.current) {
        LocalWindowSize.current.width.coerceIn(null, 1104.dp).toPx()
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

    LaunchedEffect(state) {
        snapshotFlow { state.visibleItems(50f).firstOrNull() }
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

    with(LocalSharedTransitionScope.current) {
        var showAllDes by remember { mutableStateOf(false) }
        var moreBtn by remember { mutableStateOf(false) }
        var showAllPage by remember { mutableStateOf(false) }
        Box() {
            if (!details.loading && details.content != null) {
                Box(Modifier.align(Alignment.TopCenter)) {
                    Surface(
                        Modifier.width(1104.dp).widthIn(0.dp, 1104.dp)
                            .padding(12.dp, 8.dp).align(Alignment.TopCenter),
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerLow
                    ) {
                        LazyColumn(Modifier, state) {
                            items(
                                if (!showAllPage) 1 else details.content!!.page,
                                { "ImgPage$it" }) {
                                Box(Modifier.fillMaxWidth(), Alignment.Center) {
                                    UrlWithSize.parse(details.content!!.pageUrls[it]).let { uws ->
                                        CtrlAnimatedVisibility(
                                            uws != urlWithSize,
                                            Modifier.animateItem(),
                                            label = "img-vis-$it"
                                        ) {
                                            SharedImage(uws, this, Modifier.sharedElement(
                                                rememberSharedContentState(
                                                    uws.url.split("/").last()
                                                ),
                                                this,
                                            ).run {
                                                val testH = uws.h * screenWidth / uws.w
                                                if (testH < screenHeight) {
                                                    width(with(LocalDensity.current) { screenWidth.toDp() })
                                                } else {
                                                    height(with(LocalDensity.current) {
                                                        (floor(testH / screenHeight).coerceIn(
                                                            1f,
                                                            null
                                                        ) * screenHeight)
                                                            .toDp()
                                                    })
                                                }
                                            }.pointerInput(Unit) {
                                                awaitEachGesture {
                                                    val p = awaitFirstDown()
                                                    if (urlWithSize == null) {
                                                        urlWithSize = uws
                                                        p.consume()
                                                    }
                                                }
                                            })
                                        }
                                    }

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
                            }
                            item {
                                Row(Modifier.fillMaxWidth()) {
                                    IconButton({
                                        if (details.content == null) {
                                            return@IconButton
                                        }
                                        GlobalData.nav.push(Routes.Artwork(details.content!!.apply {
                                            pageUrls = pageUrls.map { s ->
                                                UrlWithSize.parse(s).url
                                            }
                                        }))
                                    }) {
                                        Icon(
                                            painterResource(Res.drawable.book),
                                            "read in artwork view screen"
                                        )
                                    }
                                }
                            }
                            item("Title") {
                                SelectionContainer {
                                    Text(
                                        searchItemData.title, Modifier.padding(6.dp, 0.dp),
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
                                            GlobalData.historyData.addItem(searchItemData, true)
                                        }, color = Color(0f, 0f, 0f, 0.5f))
                                    }
                                }
                            }
                            item("Tags") {
                                SelectionContainer {
                                    FlowRow(
                                        Modifier.fillMaxWidth().padding(6.dp, 4.dp),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        details.content!!.tags.forEach {
                                            if (it == "R18" || it == "R18G") {
                                                Text(it, color = Color.Red)
                                            } else {
                                                Text("#$it")
                                            }
                                        }
                                    }
                                }
                            }
                            item("Author") {
                                SelectionContainer {
                                    Text(searchItemData.author, Modifier.padding(6.dp, 3.dp))
                                }
                            }
                        }
                    }
                    if (isThereImg) {
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
                ImageDialog(urlWithSize, { urlWithSize = null })
            }
        }
    }
}