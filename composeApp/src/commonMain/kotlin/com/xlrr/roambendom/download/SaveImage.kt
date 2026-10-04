package com.xlrr.roambendom.download

import coil3.PlatformContext

/** 单图保存：Android 进相册，桌面弹另存对话框 */
expect suspend fun saveImage(context: PlatformContext, bytes: ByteArray, name: String, ext: String): Boolean

/** 批量保存的存储目录：Android 逐张进相册，桌面写入已选目录 */
interface SaveTarget {
    suspend fun save(images: List<DownloadItem>, context: PlatformContext): Int
}

/** 准备获取存储目录，用户取消则返回 null */
expect suspend fun prepareDownloadTarget(context: PlatformContext): SaveTarget?
