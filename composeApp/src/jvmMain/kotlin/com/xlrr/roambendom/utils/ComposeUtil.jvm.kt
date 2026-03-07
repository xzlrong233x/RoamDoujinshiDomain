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
import androidx.compose.ui.awt.ComposeWindow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalDensity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImagePainter
import coil3.compose.rememberAsyncImagePainter
import coil3.request.ImageRequest
import coil3.toBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.jetbrains.skiko.toBufferedImage
import java.awt.FileDialog
import java.awt.image.BufferedImage
import java.io.File
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
    IconButton({
        val suc = state.value
        if (suc !is AsyncImagePainter.State.Success) return@IconButton

        val dialog = FileDialog(ComposeWindow(), "保存图片", FileDialog.SAVE)
        dialog.file = imgRequest.data.toString().split("/").last()
        dialog.isVisible = true // 阻塞直到用户操作
        val directory = dialog.directory
        val dFile = dialog.file
        val file = if (directory != null && dFile != null) {
            File(directory, dFile)
        } else {
            null
        }

        if (file == null) {
            return@IconButton // 用户取消
        }

        // 确保扩展名为 .png
        val extension = file.extension
        val format = when (extension.lowercase()) {
            "jpg", "jpeg" -> "JPEG"
            "png" -> "PNG"
            else -> "PNG"
        }

        ssio.launch {
            try {
                val awtImage = suc.result.image.toBitmap().toBufferedImage()
                ImageIO.write(
                    awtImage,
                    if (awtImage.type == BufferedImage.TYPE_INT_RGB) format else "PNG",
                    file
                )
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
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
    Box(modifier,content = content)
}