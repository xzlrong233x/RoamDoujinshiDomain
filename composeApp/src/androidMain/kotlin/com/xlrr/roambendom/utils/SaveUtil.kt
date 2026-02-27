package com.xlrr.roambendom.utils

import android.content.ContentValues
import android.content.Context
import android.provider.MediaStore
import java.io.OutputStream

fun saveBitmap(bitmap: coil3.Bitmap, context: Context, fileName: String?) : Boolean {
    val resolver = context.contentResolver
    val displayName = (fileName ?: "IMG_${System.currentTimeMillis()}") + ".png"

    val contentValues = ContentValues().apply {
        put(MediaStore.Images.Media.DISPLAY_NAME, displayName)
        put(MediaStore.Images.Media.MIME_TYPE, "image/png")
        // Android 10+ 指定相对路径，不需要权限
        put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/ROAMBENDOM")
    }

    val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
        ?: return false

    return try {
        resolver.openOutputStream(uri)?.use { outputStream: OutputStream ->
            bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, outputStream)
            true
        } ?: false
    } catch (e: Exception) {
        e.printStackTrace()
        // 保存失败时删除可能已创建的条目
        resolver.delete(uri, null, null)
        false
    }
}