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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImagePainter
import coil3.compose.LocalPlatformContext
import coil3.compose.rememberAsyncImagePainter
import coil3.request.ImageRequest
import com.shakster.gifkt.GifEncoder
import com.xlrr.roambendom.LocalStringResStorage
import com.xlrr.roambendom.data.MessageData
import com.xlrr.roambendom.download.DownloadItem
import com.xlrr.roambendom.download.ImageDownloader
import com.xlrr.roambendom.download.saveImage
import com.xlrr.roambendom.gif.GifImage
import com.xlrr.roambendom.manager.MessageManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.io.asSink
import kotlinx.io.buffered
import java.io.ByteArrayOutputStream
import kotlin.time.Duration.Companion.milliseconds

@Composable
actual fun Coil3SaveImageButton(
    imgRequest: ImageRequest,
    icon: Painter
) {
    val txt = LocalStringResStorage.current
    val imgState = rememberAsyncImagePainter(imgRequest)
    val state = imgState.state.collectAsStateWithLifecycle()
    val context = LocalPlatformContext.current
    val ssio = rememberCoroutineScope()
    IconButton({
        val suc = state.value
        if (suc !is AsyncImagePainter.State.Success) return@IconButton
        ssio.launch {
            val item = DownloadItem.of(imgRequest.data.toString())
            // ugoira 的 zip 需解码后转 gif，其余直接存原始字节
            val gif = if (item.ext == "zip") (suc.result.image as? GifImage)?.let { encodeGif(it) } else null
            val bytes = gif ?: ImageDownloader.bytes(item.url, context)
            val ext = if (gif != null) "gif" else item.ext
            val success = bytes != null && saveImage(context, bytes, item.name, ext)
            MessageManager.addMessage(
                MessageData(
                    txt[if (success) "message_save_successful_title" else "message_save_failed_title"]
                        ?.orResource() ?: "unknow".orResource(),
                    if (success) "$item.name.$ext".orResource() else "unknow".orResource()
                )
            )
        }
    }, enabled = state.value is AsyncImagePainter.State.Success) {
        Icon(icon, "save button", tint = Color.White)
    }
}

private suspend fun encodeGif(img: GifImage): ByteArray? = withContext(Dispatchers.IO) {
    runCatching {
        val out = ByteArrayOutputStream()
        val enc = GifEncoder(out.asSink().buffered())
        for ((f, d) in img.map) enc.writeFrame(f.toComposeImageBitmap().toAwtImage(), d.milliseconds)
        enc.close()
        out.toByteArray()
    }.getOrNull()
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