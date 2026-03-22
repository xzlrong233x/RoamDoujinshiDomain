package com.xlrr.roambendom.utils

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.AnimationDrawable
import android.provider.MediaStore
import androidx.core.graphics.drawable.toBitmap
import com.shakster.gifkt.GifEncoder
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.write
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.io.asSink
import kotlinx.io.buffered
import java.io.OutputStream
import kotlin.time.Duration.Companion.milliseconds

private const val ROAMBENDOM_IMG_PATH = "Pictures/ROAMBENDOM"
fun saveBitmap(bitmap: coil3.Bitmap, context: Context, fileName: String?) : Boolean {
    val resolver = context.contentResolver
    val displayName = (fileName ?: "IMG_${System.currentTimeMillis()}") + ".png"

    val contentValues = ContentValues().apply {
        put(MediaStore.Images.Media.DISPLAY_NAME, displayName)
        put(MediaStore.Images.Media.MIME_TYPE, "image/png")
        // Android 10+ 指定相对路径，不需要权限
        put(MediaStore.Images.Media.RELATIVE_PATH, ROAMBENDOM_IMG_PATH)
    }

    val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
        ?: return false

    return try {
        resolver.openOutputStream(uri)?.use { outputStream: OutputStream ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
            true
        } ?: false
    } catch (e: Exception) {
        e.printStackTrace()
        // 保存失败时删除可能已创建的条目
        resolver.delete(uri, null, null)
        false
    }
}

suspend fun saveAsGif(byteArray: ByteArray, context: Context, fileName: String?) : Boolean {
    val resolver = context.contentResolver
    val displayName = (fileName ?: "GIF_${System.currentTimeMillis()}") + ".gif"

    val contentValues = ContentValues().apply {
        put(MediaStore.Images.Media.DISPLAY_NAME, displayName)
        put(MediaStore.Images.Media.MIME_TYPE, "image/gif")
        // Android 10+ 指定相对路径，不需要权限
        put(MediaStore.Images.Media.RELATIVE_PATH, ROAMBENDOM_IMG_PATH)
    }

    val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
        ?: return false
    return try {
        val pf = PlatformFile(uri)
        pf.write(byteArray)
        true
    } catch (e: Exception) {
        e.printStackTrace()
        // 保存失败时删除可能已创建的条目
        resolver.delete(uri, null, null)
        false
    }
}

suspend fun saveAnimatedDrawable(animationDrawable: AnimationDrawable, context: Context, fileName: String?) : Boolean {
    val frames = mutableListOf<Pair<Bitmap, Int>>()
    for (i in 0 until animationDrawable.numberOfFrames) {
        val frameDrawable = animationDrawable.getFrame(i)
        val duration = (animationDrawable.getDuration(i))

        // 将 Drawable 转为 Bitmap
        val bitmap = frameDrawable.toBitmap()
        frames.add(bitmap to duration)
    }

    if (frames.isEmpty()) return false



    val resolver = context.contentResolver
    val displayName = (fileName ?: "GIF_${System.currentTimeMillis()}") + ".gif"

    val contentValues = ContentValues().apply {
        put(MediaStore.Images.Media.DISPLAY_NAME, displayName)
        put(MediaStore.Images.Media.MIME_TYPE, "image/gif")
        // Android 10+ 指定相对路径，不需要权限
        put(MediaStore.Images.Media.RELATIVE_PATH, ROAMBENDOM_IMG_PATH)
    }

    val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
        ?: return false

    return withContext(Dispatchers.IO) {
        try {
            resolver.openOutputStream(uri)?.use { outputStream: OutputStream ->
                val enc = GifEncoder(outputStream.asSink().buffered())
                for ((bitmap, delayMs) in frames) {
                    // 设置每一帧的延迟时间（毫秒）
                    enc.writeFrame(bitmap, delayMs.milliseconds)
                }
                enc.close()
                true
            } ?: false
        } catch (e: Exception) {
            e.printStackTrace()
            // 保存失败时删除可能已创建的条目
            resolver.delete(uri, null, null)
            false
        }
    }
}