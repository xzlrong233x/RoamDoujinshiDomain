package com.xlrr.roambendom.utils

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults.Indicator
import androidx.compose.material3.pulltorefresh.PullToRefreshState
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.*
import coil3.compose.AsyncImage
import coil3.compose.LocalPlatformContext
import coil3.decode.DataSource
import coil3.network.HttpException
import coil3.request.ImageRequest
import com.xlrr.roambendom.LocalAnimatedVisibilityScope
import com.xlrr.roambendom.LocalSharedTransitionScope
import com.xlrr.roambendom.config.ConfigUtil
import com.xlrr.roambendom.data.CRestriction
import com.xlrr.roambendom.data.getColor
import com.xlrr.roambendom.network.ImageOkHttpInterceptor
import com.xlrr.roambendom.network.defaultImageRequest
import com.xlrr.roambendom.painter.TextPlaceholderPainter
import com.xlrr.roambendom.progressive.SharedPainterManager
import org.jetbrains.compose.resources.painterResource
import roambendom.composeapp.generated.resources.Res
import roambendom.composeapp.generated.resources.empty_page
import roambendom.composeapp.generated.resources.loading_jpg
import kotlin.math.min
import kotlin.math.round

val LocalWindowSize = compositionLocalOf { DpSize.Zero }

@Composable
fun WindowSizeBox(modifier: Modifier,content: @Composable () -> Unit) {
    BoxWithConstraints(modifier) {
        CompositionLocalProvider(
            LocalWindowSize provides DpSize(maxWidth, maxHeight),
            content = content
        )
    }
}

@Composable
fun CenterFlowRow(modifier: Modifier, content: @Composable FlowRowScope.() -> Unit) {
    FlowRow(modifier,horizontalArrangement = Arrangement.SpaceBetween,
        verticalArrangement = Arrangement.Center,
        itemVerticalAlignment = Alignment.CenterVertically, content = content)
}

@Composable
fun CtrlAnimatedVisibility(
    visible: Boolean,
    modifier: Modifier = Modifier,
    enter: EnterTransition = fadeIn() + expandIn(),
    exit: ExitTransition = shrinkOut() + fadeOut(),
    label: String = "AnimatedVisibility",
    content: @Composable() AnimatedVisibilityScope.() -> Unit,
    ) {
    AnimatedVisibility(visible, modifier, enter, exit, label, content)
}

@Composable
expect fun CtrlPullToRefreshBox(
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
    state: PullToRefreshState = rememberPullToRefreshState(),
    contentAlignment: Alignment = Alignment.TopStart,
    indicator: @Composable BoxScope.() -> Unit = {
        Indicator(
            modifier = Modifier.align(Alignment.TopCenter),
            isRefreshing = isRefreshing,
            state = state,
        )
    },
    content: @Composable BoxScope.() -> Unit,
)

@Composable
fun CenterCircular(width: Dp = 48.dp) {
    Box(Modifier.fillMaxSize(), Alignment.Center) {
        CircularProgressIndicator(
            Modifier.width(width)
        )
    }
}

@Composable
fun CardLabel(
    label: String,
    modifier: Modifier = Modifier,
    fontStyle: TextStyle = typography.labelMedium,
    shape: Shape = RoundedCornerShape(30),
    sufColor: Color = Color(0.286f, 0.286f, 0.286f, 0.702f),
    fontColor: Color = Color.White
) {
    Surface(
        color = sufColor,
        modifier = modifier.clip(shape)
    ) {
        Text(label, color = fontColor, style = fontStyle, modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp))
    }
}

@Composable
fun DefaultErrorHandleImage(url: String, modifier: Modifier, size: IntSize? = null) {
    val txtM = rememberTextMeasurer()
    val loading = remember(size) {
        if (size != null) TextPlaceholderPainter(size.toSize(), "loading...", txtM)
        else null
    }
    val error = remember(size) {
        if (size != null) TextPlaceholderPainter(size.toSize(), "empty", txtM)
        else null
    }
    if (loading != null && error != null) {
        AsyncImage(
            model = defaultImageRequest(url, LocalPlatformContext.current, true),
            filterQuality = FilterQuality.Medium,
            contentDescription = null,
            modifier = modifier.clip(RoundedCornerShape(12.dp)).widthIn(128.dp).fillMaxWidth(),
            placeholder = loading,
            error = error,
            contentScale = ContentScale.FillWidth
        )
    } else {
        AsyncImage(
            model = defaultImageRequest(url, LocalPlatformContext.current),
            filterQuality = FilterQuality.Medium,
            contentDescription = null,
            modifier = modifier.clip(RoundedCornerShape(12.dp)).widthIn(128.dp).fillMaxWidth(),
            placeholder = painterResource(Res.drawable.loading_jpg),
            error = painterResource(Res.drawable.empty_page),
            contentScale = ContentScale.FillWidth
        )
    }
}

@Composable
private fun RowOrColumn(modifier: Modifier, or: Boolean = true,
                        horizontalAlignment: Alignment.Horizontal = Alignment.CenterHorizontally,
                        verticalAlignment: Alignment.Vertical = Alignment.CenterVertically,
                        content: @Composable () -> Unit) {
    if (or) {
        Column(modifier, horizontalAlignment = horizontalAlignment) {
            content()
        }
    } else {
        Row(modifier, verticalAlignment = verticalAlignment) {
            content()
        }
    }
}

@Composable
fun ItemInfoCardWithShared(
    url: String,
    page: Int,
    title: String,
    restriction: CRestriction?,
    size: IntSize? = null,
    extraText: String? = null,
    extraComposer: @Composable (ColumnScope.() -> Unit) = {},
    isAnimation: Boolean = false,
    onclick: () -> Unit = {},
    toColumn: Boolean = true,
    imgLabel: String? = null
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        shadowElevation = 4.dp,
        modifier = Modifier.padding(5.dp, 0.dp)
    ) {
        with(LocalSharedTransitionScope.current) {
            RowOrColumn(
                Modifier.clickable(onClick = onclick).padding(6.dp).fillMaxSize()
                    .sharedBounds(
                        rememberSharedContentState("$title$url".hashCode().toString()),
                        LocalAnimatedVisibilityScope.current,
                        enter = fadeIn(),
                        exit = fadeOut(),
                        resizeMode = SharedTransitionScope.ResizeMode.scaleToBounds()),
                verticalAlignment = Alignment.Top,
                or = toColumn
            ) {
                Box(modifier = if (toColumn) Modifier.fillMaxWidth() else Modifier.widthIn(32.dp, 128.dp)) {
                    var mod: Modifier = if (toColumn) Modifier.fillMaxWidth() else Modifier
                    if (imgLabel != null) {
                        mod = mod.sharedBounds(
                            rememberSharedContentState(imgLabel),
                            LocalAnimatedVisibilityScope.current,
                            enter = fadeIn(),
                            exit = fadeOut(),
                            resizeMode = SharedTransitionScope.ResizeMode.scaleToBounds()
                        )
                    }
                    DefaultErrorHandleImage(url, mod, size)
                    FlowColumn(
                        Modifier.align(Alignment.TopEnd).padding(2.dp), verticalArrangement = Arrangement.spacedBy(4.dp),
                        itemHorizontalAlignment = Alignment.End
                    ) {
                        if (page > 1) CardLabel("P${page}")
                        if (isAnimation) CardLabel("动图")
                    }
                }
                Column(modifier = if (toColumn) Modifier else Modifier.padding(PaddingValues(start = 4.dp))) {
                    Text(title, modifier = Modifier.fillMaxWidth(), maxLines = 3, overflow = TextOverflow.Ellipsis)
                    if (restriction != null) {
                        Box(Modifier.fillMaxWidth(), Alignment.CenterStart) {
                            Text(
                                restriction.toString(),
                                modifier = Modifier.background(restriction.getColor())
                                    .padding(2.dp, 0.dp)
                            )
                        }
                    }
                    extraText?.let {
                        Text(
                            it,
                            modifier = Modifier.fillMaxWidth(),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            fontSize = 14.sp
                        )
                    }
                    extraComposer()
                }
            }
        }
    }
}

@Composable
fun CenterColumnInfo(content: @Composable ColumnScope.() -> Unit) {
    Box(Modifier.fillMaxSize(), Alignment.Center) {
        Column(Modifier, Arrangement.Center,Alignment.CenterHorizontally) {
            content()
        }
    }
}

@Composable
fun ProgressiveImage(
    request: ImageRequest,
    modifier: Modifier,
    contentScale: ContentScale = ContentScale.Fit,
    width: Float? = null,
    height: Float? = null,
    onSizeKnown: ((Int, Int) -> Unit)? = null
) {
    val txtM = rememberTextMeasurer(0)
    val k = request.data.toString()
    if (!ConfigUtil.streamDisplay.value) {
        AsyncImage(
            model = request,
            filterQuality = FilterQuality.Medium,
            contentDescription = null,
            placeholder = painterResource(Res.drawable.loading_jpg),
            error = painterResource(Res.drawable.empty_page),
            contentScale = contentScale,
            modifier = modifier
        )
    } else {
        val p = SharedPainterManager.add(
            request,
            LocalPlatformContext.current,
            txtM,
            animated = k.endsWith(".gif") || k.endsWith(".zip")
        )
        LaunchedEffect(width, height, onSizeKnown) {
            if (width != null && height != null) {
                p.setSize(width, height)
            }
            p.onSizeKnown = onSizeKnown
        }
        AsyncImage(
            model = request,
            filterQuality = FilterQuality.Medium,
            contentDescription = null,
            placeholder = p,
            error = p,
            contentScale = contentScale,
            modifier = modifier,
            onSuccess = {
                p.destroy()
                it.result.image.let {s -> onSizeKnown?.invoke(s.width, s.height) }
                if (it.result.dataSource != DataSource.NETWORK) {
                    SharedPainterManager.checkDestroyed(request.context)
                }
            },
            onError = {
                if (
                    it.result.throwable !is HttpException
                            && it.result.throwable !is ImageOkHttpInterceptor.ItemHasBeenCaughtException
                ) {
                    p.error()
                }
            }
        )
    }
}

@Composable
expect fun Coil3SaveImageButton(imgRequest: ImageRequest, icon: Painter)

class MaxSize(private val size: Dp, private val maxCount: Int, private val perDecrease: Dp = 0.dp) : StaggeredGridCells {
    override fun Density.calculateCrossAxisCellSizes(
        availableSize: Int,
        spacing: Int
    ): IntArray {
        val px = size.roundToPx()
        val p = perDecrease.roundToPx()
        return if (availableSize > px) {
            val num = min(round((availableSize + spacing.toDouble()) / (px + spacing)).toInt(), maxCount)
            val sz = (availableSize - (num - 1) * spacing) / num
//            if (p > 0 && num > 1 && px > sz) { //添加这个判断是为了减少StaggeredGrid因为项目大小微调而产生的鬼畜
//                sz = px - p * ceil((px - sz).toDouble() / p).toInt()
//            }
            IntArray(num) { min(sz, px) }
        } else {
            IntArray(1) {availableSize}
        }
    }
}