package com.xlrr.roambendom.utils

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.expandIn
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowColumn
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.Modifier.Companion
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.LocalPlatformContext
import coil3.compose.SubcomposeAsyncImage
import coil3.request.ImageRequest
import coil3.size.Size
import com.xlrr.roambendom.data.CRestriction
import com.xlrr.roambendom.data.getColor
import org.jetbrains.compose.resources.painterResource
import roambendom.composeapp.generated.resources.Res
import roambendom.composeapp.generated.resources.empty_page
import roambendom.composeapp.generated.resources.loading_jpg
import kotlin.math.abs
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
fun ItemInfoCard(
    url: String,
    page: String,
    title: String,
    restriction: CRestriction?,
    extraText: String? = null,
    extraComposer: @Composable () -> Unit = {},
    onclick: () -> Unit = {}
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
                SubcomposeAsyncImage(
                    model = ImageRequest.Builder(LocalPlatformContext.current)
                        .data(url)
                        .size(Size.ORIGINAL)
                        .build(),
                    filterQuality = FilterQuality.Medium,
                    contentDescription = null,
                    modifier = Modifier.clip(RoundedCornerShape(12.dp)).fillMaxWidth(),
                    loading = { x ->
                        Image(
                            painterResource(Res.drawable.loading_jpg),
                            contentDescription = "Image in loading"
                        )
                    },
                    error = {x ->
                        Image(
                            painterResource(Res.drawable.empty_page),
                            contentDescription = "Image in error"
                        )
                        Text(x.result.throwable.message.toString())
                    }
                )
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
            var sz = (availableSize - (num - 1) * spacing) / num
            if (p > 0 && num > 1 && px > sz) { //添加这个判断是为了减少StaggeredGrid因为项目大小微调而产生的鬼畜
                sz = px - p * ceil((px - sz).toDouble() / p).toInt()
            }
            IntArray(num) { min(sz, px) }
        } else {
            IntArray(1) {availableSize}
        }
    }
}