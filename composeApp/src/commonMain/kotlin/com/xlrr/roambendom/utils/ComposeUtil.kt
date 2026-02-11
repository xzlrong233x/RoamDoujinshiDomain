package com.xlrr.roambendom.utils

import androidx.compose.animation.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.*
import coil3.compose.LocalPlatformContext
import coil3.compose.SubcomposeAsyncImage
import coil3.request.ImageRequest
import coil3.size.Size
import com.xlrr.roambendom.LocalAnimatedVisibilityScope
import com.xlrr.roambendom.LocalSharedTransitionScope
import com.xlrr.roambendom.data.CRestriction
import com.xlrr.roambendom.data.getColor
import org.jetbrains.compose.resources.painterResource
import roambendom.composeapp.generated.resources.Res
import roambendom.composeapp.generated.resources.empty_page
import roambendom.composeapp.generated.resources.loading_jpg
import kotlin.math.ceil
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
    fontStyle: TextStyle = typography.labelSmall,
    shape: Shape = CircleShape,
    sufColor: Color = Color(0.7f, 0.7f, 0.7f, 0.7f),
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
fun DefaultErrorHandleImage(url: String, modifier: Modifier) {
    SubcomposeAsyncImage(
        model = Regex("https://[it]\\d.nhentai.net")
            .replace(url, "").let {
                ImageRequest.Builder(LocalPlatformContext.current)
                    .diskCacheKey(it)
                    .memoryCacheKey(it)
                    .data(url)
                    .size(Size.ORIGINAL)
                    .build()
            },
        filterQuality = FilterQuality.Medium,
        contentDescription = null,
        modifier = modifier.clip(RoundedCornerShape(12.dp)).fillMaxWidth(),
        loading = { x ->
            Image(
                painterResource(Res.drawable.loading_jpg),
                contentDescription = "Image in loading"
            )
        },
        error = { x ->
            println("request $url failed, msg: ${x.result.throwable.message}")
            Image(
                painterResource(Res.drawable.empty_page),
                contentDescription = "Image in error"
            )
            Text(x.result.throwable.message.toString())
        }
    )
}

@Composable
fun ItemInfoCardWithShared(
    url: String,
    page: String,
    title: String,
    restriction: CRestriction?,
    extraText: String? = null,
    extraComposer: @Composable () -> Unit = {},
    onclick: () -> Unit = {},
    imgLabel: String? = null
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        shadowElevation = 4.dp,
        modifier = Modifier.padding(5.dp, 0.dp)
    ) {
        Column(
            Modifier.clickable(onClick = onclick).padding(6.dp).fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(modifier = Modifier.fillMaxWidth()) {
                with(LocalSharedTransitionScope.current) {
                    var mod: Modifier = Modifier
                    if (imgLabel != null) {
                        mod = mod.sharedBounds(
                            rememberSharedContentState(imgLabel),
                            LocalAnimatedVisibilityScope.current,
                            enter = fadeIn(),
                            exit = fadeOut(),
                            resizeMode = SharedTransitionScope.ResizeMode.scaleToBounds()
                        )
                    }
                    DefaultErrorHandleImage(url, mod)
                }
                FlowColumn(
                    Modifier.align(Alignment.TopEnd), verticalArrangement = Arrangement.spacedBy(4.dp),
                    itemHorizontalAlignment = Alignment.End
                ) {
                    page.toIntOrNull()?.let {
                        CardLabel("P${it}")
                    }
                }
            }
            Text(title, modifier = Modifier.fillMaxWidth(), maxLines = 3, overflow = TextOverflow.Ellipsis)
            if (restriction != null) {
                Box(Modifier.fillMaxWidth(), Alignment.CenterStart) {
                    Text(
                        restriction.toString(),
                        modifier = Modifier.background(restriction.getColor())
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