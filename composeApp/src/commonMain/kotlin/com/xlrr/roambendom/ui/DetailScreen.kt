package com.xlrr.roambendom.ui

import androidx.compose.animation.expandIn
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
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
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.AsyncImage
import coil3.compose.LocalPlatformContext
import com.xlrr.roambendom.LocalAnimatedVisibilityScope
import com.xlrr.roambendom.LocalSharedTransitionScope
import com.xlrr.roambendom.data.ArtworkInfo
import com.xlrr.roambendom.data.CSources
import com.xlrr.roambendom.data.CSources.NHENTAI
import com.xlrr.roambendom.data.CSources.PIXIV
import com.xlrr.roambendom.data.SearchItemData
import com.xlrr.roambendom.nav.Routes
import com.xlrr.roambendom.network.NHWebHelper
import com.xlrr.roambendom.network.PIXIVApiHelper
import com.xlrr.roambendom.network.UrlWithSize
import com.xlrr.roambendom.network.defaultImageRequest
import com.xlrr.roambendom.utils.*
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.painterResource
import roambendom.composeapp.generated.resources.Res
import roambendom.composeapp.generated.resources.book
import roambendom.composeapp.generated.resources.empty_page
import roambendom.composeapp.generated.resources.loading_jpg
import kotlin.math.floor

class DetailViewModel() : ViewModel() {
    var content: ArtworkInfo? by mutableStateOf(null)
    var loading by mutableStateOf(false)
    var error : Exception? by mutableStateOf(null)
    var source: CSources = NHENTAI
    var id: String = ""

    fun isSuccessful() : Boolean {
        return !loading && content != null && error == null
    }

    suspend fun reload(idd: String, sourceIn: CSources, forceLoad: Boolean = false) {
        error = null
        if (idd == id && sourceIn == source && !forceLoad) {
            return
        }
        loading = true
        id = idd
        source = sourceIn
        try {
            content = when (source) {
                NHENTAI -> NHWebHelper.artwork(id)
                PIXIV -> PIXIVApiHelper.artwork(id)
            }
        } catch (e : Exception) {
            e.printStackTrace()
            error = e
        }
        loading = false
    }
}

// 我使用SelectionContainer的时候，曾遇到过一个关于select range的报错，不过我难以复现。


@Composable
fun NHDetail(searchItemData: SearchItemData, details: DetailViewModel) {
    with(LocalSharedTransitionScope.current) {
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally) {
            DetailBox() {
                Row(Modifier.fillMaxWidth()) {
                    DefaultErrorHandleImage(
                        searchItemData.thumb,
                        Modifier.sharedBounds(
                            rememberSharedContentState(searchItemData.thumb),
                            LocalAnimatedVisibilityScope.current
                        ).width((LocalWindowSize.current.width.value * 0.382).dp.coerceIn(98.dp, 256.dp))
                    )
                    Spacer(Modifier.width(8.dp))
                    SelectionContainer {
                        Column {
                            Text(searchItemData.title, style = typography.titleLarge,
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
                                        if (it.page > 0) Text(
                                            "页数：${it.page}",
                                            style = typography.titleMedium
                                        )
                                        Text(
                                            "语言：${it.language.toString().lowercase()}",
                                            style = typography.titleMedium
                                        )
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
                            GlobalData.nav.push(Routes.Artwork(details.content!!))
                        }, shape = RoundedCornerShape(20),
                            modifier = Modifier.width(128.dp).height(36.dp),
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
                    MultiCardTagBox("标签", details.content?.tags.orEmpty())
                    MultiCardTagBox("作者", details.content?.authors.orEmpty())
                    MultiCardTagBox("团体", details.content?.groups.orEmpty())
                }
            }
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
    val screenHeight = with(LocalDensity.current) {LocalWindowSize.current.height.toPx()}
    val isThereImg by remember(state) {
        derivedStateOf {
            state.layoutInfo.visibleItemsInfo.any {
                it.key.toString().contains("ImgPage")
            }
        }
    }
    var thumbExpand by remember { mutableStateOf(false) }
    var page by remember { mutableIntStateOf(0) }
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
            preload.preload(it.pageUrls.map { s -> UrlWithSize.parse(s).url }, page, 7)
        }
    }

    with(LocalSharedTransitionScope.current) {
        var showAllDes by remember { mutableStateOf(false) }
        var moreBtn by remember { mutableStateOf(false) }
        var showAllPage by remember { mutableStateOf(false) }
        Box() {
            if (!details.loading && details.content != null) {
                Box (Modifier.align(Alignment.TopCenter)) {
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
                                Box(Modifier.fillMaxWidth()) {
                                    UrlWithSize.parse(details.content!!.pageUrls[it]).let { uws ->
                                        AsyncImage(
                                            model = defaultImageRequest(
                                                uws.url,
                                                LocalPlatformContext.current
                                            ),
                                            filterQuality = FilterQuality.Medium,
                                            contentDescription = null,
                                            placeholder = painterResource(Res.drawable.loading_jpg),
                                            error = painterResource(Res.drawable.empty_page),
                                            modifier = Modifier.run {
                                                val testH = uws.h * screenWidth / uws.w
                                                if (testH < screenHeight) {
                                                    width(with(LocalDensity.current) { screenWidth.toDp() })
                                                } else {
                                                    height(with(LocalDensity.current) {
                                                        (floor(testH / screenHeight).coerceIn(1f, null) * screenHeight)
                                                            .toDp()
                                                    })
                                                }
                                            }.align(Alignment.TopCenter)
                                        )
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
                                        Icon(painterResource(Res.drawable.book),
                                            "read in artwork view screen")
                                    }
                                }
                            }
                            item("Title") {
                                SelectionContainer {
                                    Text(
                                        searchItemData.title, Modifier.padding(6.dp, 0.dp),
                                        style = typography.headlineMedium
                                    )
                                }
                            }
                            item("Description") {
                                Column(Modifier.fillMaxWidth().padding(8.dp, 3.dp)) {
                                    SelectionContainer {
                                        Text(
                                            details.content?.description.toString(),
                                            style = typography.bodyMedium,
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
                            shape = RoundedCornerShape(6.dp),
                            onClick = { thumbExpand = !thumbExpand },
                            enabled = details.content?.thumbUrls?.isNotEmpty() == true
                        ) {
                            Text(
                                "${page + 1} / ${details.content?.page}",
                                modifier = Modifier.padding(4.dp, 2.dp),
                                fontWeight = FontWeight.Bold, style = typography.labelMedium
                            )
                        }
                    }
                }
                if (thumbExpand) {
                    ThumbDialog(details.content?.thumbUrls ?: listOf(), {
                        ss.launch { state.animateScrollToItem(it) }
                    }, {thumbExpand = false})
                }
            }
        }
    }
}

@Composable
fun DetailScreen(searchItemData: SearchItemData, modifier: Modifier = Modifier, details: DetailViewModel = viewModel { DetailViewModel() }) {
    LaunchedEffect(Unit) {
        details.reload(searchItemData.id, searchItemData.source)
    }
    val co = rememberCoroutineScope()
    Scaffold(modifier.fillMaxSize()) { pd ->
        Surface(modifier.fillMaxSize().padding(pd)) {
            when (searchItemData.source) {
                NHENTAI -> NHDetail(searchItemData, details)
                PIXIV -> PIXIVDetail(searchItemData, details)
            }
            if (details.loading) {
                CenterCircular()
            }
            if (details.error != null) {
                details.error?.let {
                    Box(Modifier.fillMaxSize(), Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("错误：${it.message}")
                            Button({
                                co.launch {
                                    details.reload(
                                        searchItemData.id,
                                        searchItemData.source,
                                        true
                                    )
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