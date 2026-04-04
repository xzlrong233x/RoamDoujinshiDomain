package com.xlrr.roambendom.network

import coil3.PlatformContext
import coil3.SingletonImageLoader
import coil3.memory.MemoryCache
import com.xlrr.roambendom.config.ConfigUtil
import com.xlrr.roambendom.progressive.ProgressivePainter
import com.xlrr.roambendom.progressive.SharedPainterManager
import kotlinx.io.Buffer
import kotlinx.io.IOException
import kotlinx.io.readByteArray
import okhttp3.Interceptor
import okhttp3.MediaType
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody
import okhttp3.ResponseBody.Companion.toResponseBody
import okhttp3.internal.closeQuietly
import okhttp3.internal.connection.RealCall
import java.io.ByteArrayOutputStream
import java.util.concurrent.Executors
import kotlin.random.Random

class ImageOkHttpInterceptor(
    private val chunkCount: Int = 16,
    private val minSizeForChunk: Long = 1024 * 64,
    private val timeoutSeconds: Long = 30,
    private val platformContext: PlatformContext
): Interceptor {
    class ItemAlreadyCachedException(msg: String) : Exception(msg)
    companion object {
        private const val SKIP_CHUNK_HEADER = "X-Skip-Chunked"
    }

    private fun isCached(key: String) : Boolean {
        val loader = SingletonImageLoader.get(platformContext)
        return loader.memoryCache?.get(MemoryCache.Key(key)) != null
    }

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val call = chain.call()
        val ls = request.url.pathSegments.last()

        if (request.header(SKIP_CHUNK_HEADER) != null) {
            // 移除标记头，避免影响服务器
            val newRequest = request.newBuilder()
                .removeHeader(SKIP_CHUNK_HEADER)
                .build()
            return chain.proceed(newRequest)
        }

        val painter = SharedPainterManager.map[request.url.toString()] // painter的存在应由外部显示组件负责
        // 仅对符合条件的图片请求启用分块（可根据需求自定义）
        if (!isSupportedUrl(request) ||
            !(ConfigUtil.useMultithread.value || ConfigUtil.streamDisplay.value) ||
            call !is RealCall ||
            (painter != null && !painter.focus())) {
            if (painter != null && ConfigUtil.streamDisplay.value) {
                return failedRespond(request, "there has been a painter focus on its multithread download", 943)
            } // 这部分代码主要是为了让coil不发送过多请求，但似乎会导致一些问题，或许我应该让它直接报错
            return chain.proceed(request)
        }
        val client = call.client

        // 1. 获取文件总大小并检查服务器是否支持 Range
        val totalSize = getTotalSize(client, request)
        if (totalSize == null) {
            painter?.release()
            return chain.proceed(request)
        }
        painter?.setFileSize(totalSize)

        // 2. 创建分块请求
        val al = painter?.fileSize() ?: 0
        val rest = totalSize - al
        if (rest <= 0 && painter != null) {
            painter.release()
            return successResponse(
                request,
                painter.bytes().toResponseBody(request.body?.contentType()),
                al
            )
        }

        if (ConfigUtil.streamDisplay.value && !ConfigUtil.useMultithread.value) {
            try {
                return streamRead(chain, painter, client, totalSize)
            } finally {
                painter?.release()
            }
        }

        val chunkSize = rest / chunkCount
        val rangeRequests = (0 until chunkCount).map { index ->
            val start = index * chunkSize + al
            val end = (if (index == chunkCount - 1) rest - 1 else (index + 1) * chunkSize - 1) + al
            request.newBuilder()
                .header("Range", "bytes=$start-$end")
                .header(SKIP_CHUNK_HEADER, "true")
                .build()
        }

        // 3. 使用固定线程池并发下载（限制最大并发数，避免创建过多线程）
        val executor = Executors.newFixedThreadPool(chunkCount.coerceAtMost(6))
        try {
            val maxRetries = 5

            val futures = rangeRequests.map { rangeRequest ->
                executor.submit<ByteArray> {
                    var retryCount = 0
                    var lastException: Exception? = null
                    while (retryCount < maxRetries) {
                        var response: Response? = null
                        try {
                            // 每次重试都创建新的 Call（使用分块专用的 client）
                            response = client.newCall(rangeRequest).execute()
                            if (!response.isSuccessful) {
                                throw IOException("HTTP ${response.code}")
                            }
                            if (isCached(request.url.toString())) {
                                retryCount = 114514
                                throw ItemAlreadyCachedException("url: ${request.url}")
                            }
                            val body = response.body ?: throw IOException("Empty body")
                            val bytes = body.bytes()
                            return@submit bytes // 成功，返回字节数组
                        } catch (e: IOException) {
                            lastException = e
                            retryCount++
                            if (retryCount < maxRetries) {
                                Thread.sleep(Random.nextLong(700,1500) * retryCount)
                            }
                        } finally {
                            response?.closeQuietly()
                        }
                    }
                    throw IOException("Chunk download failed after $maxRetries retries", lastException)
                }
            }

            val chunks = mutableListOf<ByteArray>()
            for (future in futures) {
                try {
                    val bytes = future.get()
                    painter?.write(bytes)
                    chunks.add(bytes)
                    if (isCached(request.url.toString())) {
                        throw ItemAlreadyCachedException("url: ${request.url}")
                    }
                } catch (e: Exception) {
                    // 任何一个分块失败，取消所有未完成的任务
                    futures.forEach { it.cancel(true) }
                    if (e.cause is ItemAlreadyCachedException || e is ItemAlreadyCachedException) {
                        return failedRespond(request,"item is already exist")
                    }
                    throw IOException("Chunk download failed", e)
                }
            }

            // 4. 合并所有分块数据
            val mergedData = chunks.fold(ByteArrayOutputStream(totalSize.toInt()).apply {
                painter?.bytes()?.let { write(it) }
            }) { acc, bytes ->
                acc.write(bytes)
                acc
            }.toByteArray()

            // 5. 构建新的成功响应
            val responseBody = mergedData.toResponseBody(request.body?.contentType())
            return successResponse(request, responseBody, mergedData.size)
        } catch (e: Exception) {
            e.printStackTrace()
            return chain.proceed(request)
        } finally {
            painter?.release()
            executor.shutdownNow()
        }
    }

    private fun successResponse(request: Request, responseBody: ResponseBody, len: Number): Response {
        return Response.Builder()
            .request(request)
            .protocol(Protocol.HTTP_1_1)
            .code(200)
            .message("OK")
            .body(responseBody)
            .addHeader("Content-Length", len.toString())
            .build()
    }

    // deepseek真不错
    private fun streamRead(
        chain: Interceptor.Chain,
        painter: ProgressivePainter?,
        client: OkHttpClient,
        totalSize: Long,
        chunkSize: Long = 64 * 1024L,
        maxRetries: Int = 3
    ): Response {
        val originalRequest = chain.request()
        val alreadyLoaded = painter?.fileSize() ?: 0L
        val buffer = Buffer()
        if (painter != null && alreadyLoaded > 0) {
            buffer.write(painter.bytes())
        }

        var start = alreadyLoaded
        var remaining = totalSize - alreadyLoaded
        var contentType: MediaType? = null

        var attempt = 0
        while (remaining > 0 && attempt < maxRetries) {
            val rangeRequest = originalRequest.newBuilder()
                .header("Range", "bytes=$start-")   // 请求从 start 到文件末尾
                .header(SKIP_CHUNK_HEADER, "true")
                .build()

            try {
                client.newCall(rangeRequest).execute().use { response ->
                    if (!response.isSuccessful) {
                        throw IOException("Unexpected code ${response.code}")
                    }
                    if (contentType == null) {
                        contentType = response.body?.contentType()
                    }
                    val source = response.body?.source() ?: throw IOException("Empty body")

                    // 分块读取，边读边写入 painter 和 buffer
                    while (remaining > 0) {
                        val readSize = minOf(chunkSize, remaining)
                        val chunk = source.readByteArray(readSize)
                        if (chunk.isEmpty()) break  // 流提前结束

                        painter?.write(chunk)
                        buffer.write(chunk)

                        remaining -= chunk.size
                        start += chunk.size
                    }

                    // 如果正常读完，退出 while 循环
                    if (remaining == 0L) break
                    else throw IOException("Stream ended prematurely, missing ${remaining} bytes")
                }
            } catch (e: IOException) {
                attempt++
                if (attempt >= maxRetries) {
                    throw IOException("Failed to download remaining data after $maxRetries retries", e)
                }
                // 重试前等待递增时间
                val delayMs = 100L * attempt
                Thread.sleep(delayMs)
                // 注意：remaining 和 start 已经更新为已成功写入的进度，重试时自动从断点继续
            }
        }

        if (remaining > 0) {
            throw IOException("Unable to complete download after $maxRetries retries")
        }

        val responseBody = buffer.readByteArray().toResponseBody(contentType)
        return successResponse(originalRequest, responseBody, totalSize)
    }

    private fun failedRespond(request: Request,msg: String = "", code: Int = 404) : Response {
        return Response.Builder()
            .request(request)
            .code(code)
            .protocol(Protocol.HTTP_1_1)
            .message(msg)
            .build()
    }

    private fun isSupportedUrl(request: Request): Boolean {
        val url = request.url.toString().lowercase()
        return (url.endsWith(".jpg") || url.endsWith(".jpeg") ||
                url.endsWith(".png") || url.endsWith(".webp") ||
                url.endsWith(".gif") || url.endsWith(".bmp")) &&
                (url.contains("master1200") ||
                        url.contains("i\\d.nhentai.net".toRegex()) ||
                        url.contains("ugoira"))
    }

    // 通过 HEAD 请求获取文件总大小，同时验证服务器是否支持 Range
    private fun getTotalSize(client: OkHttpClient, request: Request): Long? {
        val headRequest = request.newBuilder()
            .header("Range", "bytes=0-0")
            .header(SKIP_CHUNK_HEADER, "true")
            .build()

        try {
            val headResponse = client.newCall(headRequest).execute()
            // 服务器支持 Range 时应返回 206 Partial Content
            if (headResponse.code != 206) {
                headResponse.closeQuietly()
                return null
            }
            val contentRange = headResponse.header("Content-Range") ?: return null
            headResponse.closeQuietly()
            return contentRange.substringAfter('/').toLongOrNull()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }
}