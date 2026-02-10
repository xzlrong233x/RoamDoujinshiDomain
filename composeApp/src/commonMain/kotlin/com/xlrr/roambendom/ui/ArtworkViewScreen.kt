package com.xlrr.roambendom.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImagePainter
import coil3.compose.AsyncImagePainter.Companion.DefaultTransform
import coil3.compose.LocalPlatformContext
import coil3.compose.SubcomposeAsyncImage
import coil3.request.ImageRequest
import coil3.size.Size
import com.xlrr.roambendom.data.ArtworkInfo
import com.xlrr.roambendom.utils.GlobalData
import com.xlrr.roambendom.utils.LocalWindowSize
import net.engawapg.lib.zoomable.rememberZoomState
import net.engawapg.lib.zoomable.zoomable
import org.jetbrains.compose.resources.painterResource
import roambendom.composeapp.generated.resources.Res
import roambendom.composeapp.generated.resources.empty_page
import roambendom.composeapp.generated.resources.loading_jpg

@Composable
private fun LoadingImage(
    data: String,
    transform: (AsyncImagePainter.State) -> AsyncImagePainter.State = DefaultTransform,
    contentScale: ContentScale = ContentScale.Fit
) {
    if (data.isEmpty()) {
        Image(painterResource(Res.drawable.empty_page), "empty page")
        return
    }
    SubcomposeAsyncImage(
        model = ImageRequest.Builder(LocalPlatformContext.current)
            .data(data)
            .size(Size.ORIGINAL)
            .build(),
        filterQuality = FilterQuality.Medium,
        contentDescription = null,
        loading = {
            Image(
                painterResource(Res.drawable.loading_jpg),
                contentDescription = "Image in loading"
            )
        },
        error = {
            Box(contentAlignment = Alignment.TopCenter) {
                Image(painterResource(Res.drawable.empty_page), "empty page")
                Column(
                    Modifier.background(Color.DarkGray),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("WHAT?")
                    Text(it.result.throwable.message.toString())
                }
            }
        },
        transform = transform,
        contentScale = contentScale
    )
}

@Composable
fun ArtworkViewScreen(artworkInfo: ArtworkInfo) {
    if (artworkInfo.page < 1) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("无效的作品内容")
        }
        return
    }
    val zzm = rememberZoomState()
    val maxWidth = with(LocalDensity.current) {LocalWindowSize.current.width.toPx()}
    val maxHeight = with(LocalDensity.current) {LocalWindowSize.current.height.toPx()}
    val pager = rememberPagerState { artworkInfo.page }
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        HorizontalPager(pager, Modifier.fillMaxSize()
            ) {
            Box(Modifier.fillMaxSize().zoomable(zzm, enableOneFingerZoom = zzm.scale > 1f,
                onTap = { of ->
                    val t = of.x
                    if (t <= maxWidth * 0.33f) {
                        pager.requestScrollToPage(pager.currentPage - 1)
                    } else if ( t <= maxWidth * 0.67f ) {

                    } else {
                        pager.requestScrollToPage(pager.currentPage + 1)
                    }
                }),contentAlignment = Alignment.Center) { LoadingImage(artworkInfo.pageUrls[it]) }
        }
        Surface(Modifier.fillMaxWidth().statusBarsPadding().height(56.dp).align(Alignment.TopCenter),
            color = MaterialTheme.colorScheme.surface.copy(0.35f)) {
            Row(Modifier.fillMaxWidth().height(56.dp)) {
                IconButton({ GlobalData.nav.defaultBack()}) {
                    Text("返")
                }
            }
        }
    }
}