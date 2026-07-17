package com.xlrr.roambendom.utils

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.BitmapImage
import coil3.compose.AsyncImagePainter
import coil3.compose.rememberAsyncImagePainter
import coil3.request.ImageRequest
import coil3.toBitmap
import com.xlrr.roambendom.ugoira.MultiImagePackage
import kotlinx.coroutines.launch

@Composable
actual fun Coil3SaveImageButton(
    imgRequest: ImageRequest,
    icon: Painter
) {
    val imgState = rememberAsyncImagePainter(imgRequest)
    val state = imgState.state.collectAsStateWithLifecycle()
    val suc = state.value
    val context = LocalContext.current
    val c = rememberCoroutineScope()
    IconButton({
        c.launch {
            if (suc !is AsyncImagePainter.State.Success) return@launch
            val fileName = imgRequest.data.toString().split("/").last().split(".").first()
            val success = when(val img = suc.result.image) {
                is BitmapImage -> {
                    saveBitmap(
                        img.toBitmap(),
                        context,
                        fileName
                    )
                }
                is MultiImagePackage -> {
                    saveAnimatedDrawable(img, context, fileName)
                }
                else -> false
            }
            val msg = if (success) "已保存到相册" else "保存失败"
            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
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
    PullToRefreshBox(
        isRefreshing,
        onRefresh,
        modifier,
        state,
        contentAlignment,
        indicator,
        content
    )
}

@Composable
actual fun CommandBox(
    modifier: Modifier,
    privateCommands: List<CommandData>,
    contentAlignment: Alignment,
    publicCommands: List<CommandData>,
    content: @Composable BoxScope.((Boolean) -> Unit) -> Unit
) {
    var opened by remember { mutableStateOf(false) }
    val public = LocalPublicCommandItem.current.toTypedArray()
    val list = listOf(
        *publicCommands.toTypedArray(),
        *public
    )
    CompositionLocalProvider(LocalPublicCommandItem provides list) {
        Box(
            modifier, contentAlignment = contentAlignment
        ) {
            content {opened = it}
            DropdownMenu(opened, { opened = false }) {
                privateCommands.forEach {
                    ListItem(
                        {
                            Text(it.label)
                        },
                        Modifier.clickable(it.enabled(),onClick = {
                            it.click()
                            opened = false
                        })
                    )

                }
                list.forEach {
                    ListItem(
                        {
                            Text(it.label)
                        },
                        Modifier.clickable(it.enabled(),onClick = {
                            it.click()
                            opened = false
                        })
                    )
                }
            }
        }
    }
}