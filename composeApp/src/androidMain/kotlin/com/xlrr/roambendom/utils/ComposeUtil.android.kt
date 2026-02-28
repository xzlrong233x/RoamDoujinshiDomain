package com.xlrr.roambendom.utils

import android.widget.Toast
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImagePainter
import coil3.compose.rememberAsyncImagePainter
import coil3.request.ImageRequest
import coil3.toBitmap

@Composable
actual fun Coil3SaveImageButton(
    imgRequest: ImageRequest,
    icon: Painter
) {
    val imgState = rememberAsyncImagePainter(imgRequest)
    val state = imgState.state.collectAsStateWithLifecycle()
    val suc = state.value
    val context = LocalContext.current
    IconButton({
        if (suc !is AsyncImagePainter.State.Success) return@IconButton
        val success = saveBitmap(
            suc.result.image.toBitmap(),
            context,
            imgRequest.data.toString().split("/").last()
        )
        val msg = if (success) "已保存到相册" else "保存失败"
        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
    }, enabled = state.value is AsyncImagePainter.State.Success) {
        Icon(icon, "save button", tint = Color.White)
    }
}