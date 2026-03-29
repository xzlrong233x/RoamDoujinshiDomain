package com.xlrr.roambendom.utils

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material3.pulltorefresh.PullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalDensity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImagePainter
import coil3.compose.rememberAsyncImagePainter
import coil3.request.ImageRequest
import coil3.toBitmap
import com.shakster.gifkt.GifEncoder
import com.xlrr.roambendom.gif.GifImage
import io.github.vinceglb.filekit.dialogs.FileKitDialogSettings
import io.github.vinceglb.filekit.dialogs.compose.rememberFileSaverLauncher
import io.github.vinceglb.filekit.extension
import io.github.vinceglb.filekit.sink
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.io.buffered
import org.jetbrains.skiko.toBufferedImage
import java.awt.image.BufferedImage
import javax.imageio.ImageIO

@Composable
actual fun Coil3SaveImageButton(
    imgRequest: ImageRequest,
    icon: Painter
) {
    val den = LocalDensity.current
    val imgState = rememberAsyncImagePainter(imgRequest)
    val state = imgState.state.collectAsStateWithLifecycle()
    val ssio = rememberCoroutineScope { Dispatchers.IO }
    val launcher = rememberFileSaverLauncher(FileKitDialogSettings(
        "保存图片"
    )) { file ->
        if (file != null && state.value is AsyncImagePainter.State.Success) {
            val extension = file.extension
            val format = when (extension.lowercase()) {
                "jpg", "jpeg" -> "JPEG"
                "png" -> "PNG"
                "gif" -> "GIF"
                else -> "PNG"
            }
            val img = (state.value as AsyncImagePainter.State.Success).result.image
            ssio.launch {
                if (img is GifImage) {
                    val enc = GifEncoder(file.sink().buffered())
                    for (i in img.gifDecoder.asList()) {
                        enc.writeFrame(i)
                    }
                    enc.close()
                } else {
                    val awtImage = img.toBitmap().toBufferedImage()
                    ImageIO.write(
                        awtImage,
                        if (awtImage.type == BufferedImage.TYPE_INT_RGB) format else "PNG",
                        file.file
                    )
                }
            }
        }
    }
    IconButton({
        val suc = state.value
        if (suc !is AsyncImagePainter.State.Success) return@IconButton

        val fn = imgRequest.data.toString().split("/").last().split(".").first()
        val img = suc.result.image
        launcher.launch(fn, if (img is GifImage) "gif" else "png")
    }, enabled = state.value is AsyncImagePainter.State.Success) {
        Icon(icon, "save button", tint = Color.White)
    }
}

@Composable
actual fun CtrlPullToRefreshBox(
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    modifier: Modifier,
    state: PullToRefreshState,
    contentAlignment: Alignment,
    indicator: @Composable (BoxScope.() -> Unit),
    content: @Composable (BoxScope.() -> Unit)
) {
    Box(modifier, contentAlignment,content = content)
}