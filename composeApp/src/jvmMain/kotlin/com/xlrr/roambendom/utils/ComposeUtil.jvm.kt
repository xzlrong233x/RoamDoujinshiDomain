package com.xlrr.roambendom.utils

import androidx.compose.foundation.ContextMenuArea
import androidx.compose.foundation.ContextMenuItem
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material3.pulltorefresh.PullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.toAwtImage
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.platform.LocalDensity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImagePainter
import coil3.compose.rememberAsyncImagePainter
import coil3.request.ImageRequest
import coil3.toBitmap
import com.shakster.gifkt.GifEncoder
import com.xlrr.roambendom.LocalStringResStorage
import com.xlrr.roambendom.data.MessageData
import com.xlrr.roambendom.gif.GifImage
import com.xlrr.roambendom.manager.MessageManager
import io.github.vinceglb.filekit.dialogs.FileKitDialogSettings
import io.github.vinceglb.filekit.dialogs.compose.rememberFileSaverLauncher
import io.github.vinceglb.filekit.extension
import io.github.vinceglb.filekit.name
import io.github.vinceglb.filekit.path
import io.github.vinceglb.filekit.sink
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.io.buffered
import org.jetbrains.skiko.toBufferedImage
import java.awt.image.BufferedImage
import javax.imageio.ImageIO
import kotlin.time.Duration.Companion.milliseconds

@Composable
actual fun Coil3SaveImageButton(
    imgRequest: ImageRequest,
    icon: Painter
) {
    val den = LocalDensity.current
    val txt = LocalStringResStorage.current
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
                "gif", "zip" -> "GIF"
                else -> "PNG"
            }
            val img = (state.value as AsyncImagePainter.State.Success).result.image
            ssio.launch {
                try {
                    if (img is GifImage) {
                        val enc = GifEncoder(file.sink().buffered())
                        for (i in img.map) {
                            enc.writeFrame(i.key.toComposeImageBitmap().toAwtImage(), i.value.milliseconds)
                        }
                        enc.close()
                    } else {
                        val awtImage = img.toBitmap().toBufferedImage()
                        val rgbImage = BufferedImage(awtImage.width, awtImage.height, BufferedImage.TYPE_INT_RGB)
                        val g = rgbImage.createGraphics()
                        g.drawImage(awtImage, 0, 0, null)
                        g.dispose()
                        ImageIO.write(
                            rgbImage,
                            if (awtImage.type == BufferedImage.TYPE_INT_RGB) format else "PNG",
                            file.file
                        )
                    }
                } catch (e: Exception) {
                    MessageManager.addMessage(
                        MessageData(
                            txt["message_save_failed_title"]?.orResource() ?: "unknow".orResource(),
                            txt["message_save_failed_content"]?.orResource(e.message.toString()) ?: "unknow".orResource(),
                        )
                    )
                    return@launch
                }
                MessageManager.addMessage(
                    MessageData(
                        txt["message_save_successful_title"]?.orResource() ?: "unknow".orResource(),
                        txt["message_save_successful_content"]?.orResource(file.name, file.path) ?: "unknow".orResource(),
                    )
                )
            }
        }
    }
    IconButton({
        val suc = state.value
        if (suc !is AsyncImagePainter.State.Success) return@IconButton

        val fn = imgRequest.data.toString().split("/").last().split(".").first()
        val img = suc.result.image
        launcher.launch(suggestedName = fn, defaultExtension = if (img is GifImage) "gif" else "png")
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
    additionalCommands: @Composable () -> List<CommandData>,
    content: @Composable (BoxScope.() -> Unit)
) {
    val n = additionalCommands()
    val me = n.map {
        ContextMenuItem(it.label.getComposeOrString(), it.enabled(), it.click)
    }
    ContextMenuArea({me}) {
        val al = arrayListOf<CommandData>()
        al.addAll(n)
        al.addAll(LocalPublicCommandItem.current)
        CompositionLocalProvider(LocalPublicCommandItem provides al) {
            Box(modifier, contentAlignment,content = content)
        }
    }
}

@Composable
actual fun CommandBox(
    modifier: Modifier,
    privateCommands: List<CommandData>,
    contentAlignment: Alignment,
    publicCommands: List<CommandData>,
    content: @Composable BoxScope.((Boolean) -> Unit) -> Unit
) {
    val public = LocalPublicCommandItem.current.toTypedArray()
    val list = listOf(
        *privateCommands.toTypedArray(),
        *publicCommands.toTypedArray(),
        *public
    )
    val orr = list.map {
        ContextMenuItem(
            it.label.getComposeOrString(),
            it.enabled(),
            it.click,
        )
    }
    Box(modifier) {
        ContextMenuArea(
            {
                orr
            }
        ) {
            CompositionLocalProvider(
                LocalPublicCommandItem provides list
            ) {
                Box(contentAlignment = contentAlignment) {
                    content {}
                }
            }
        }
    }
}