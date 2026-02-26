package com.xlrr.roambendom.network

import coil3.PlatformContext
import coil3.SingletonImageLoader
import coil3.memory.MemoryCache
import com.xlrr.roambendom.config.ConfigUtil
import kotlinx.io.IOException
import okhttp3.Interceptor
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import okhttp3.internal.closeQuietly
import okhttp3.internal.connection.RealCall
import okio.Path.Companion.toPath
import java.io.ByteArrayOutputStream
import java.util.concurrent.Executors
import kotlin.random.Random

class ImageOkHttpInterceptor(
    private val chunkCount: Int = Runtime.getRuntime().availableProcessors(),
    private val minSizeForChunk: Long = 1024 * 512,
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

        // 仅对符合条件的图片请求启用分块（可根据需求自定义）
        if (!shouldUseMultithreadDownload(request) || call !is RealCall) {
            return chain.proceed(request)
        }
        val client = call.client

        // 1. 获取文件总大小并检查服务器是否支持 Range
        val totalSize = getTotalSize(chain, request) ?: return chain.proceed(request)

        // 如果图片太小，不分块，直接返回普通请求
        if (totalSize < minSizeForChunk) {
            return chain.proceed(request)
        }

        // 2. 创建分块请求
        val chunkSize = totalSize / chunkCount
        val rangeRequests = (0 until chunkCount).map { index ->
            val start = index * chunkSize
            val end = if (index == chunkCount - 1) totalSize - 1 else (index + 1) * chunkSize - 1
            request.newBuilder()
                .header("Range", "bytes=$start-$end")
                .header(SKIP_CHUNK_HEADER, "true")
                .build()
        }

        // 3. 使用固定线程池并发下载（限制最大并发数，避免创建过多线程）
        val executor = Executors.newFixedThreadPool(chunkCount.coerceAtMost(4))
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
                                Thread.sleep(Random.nextLong(700,1500))
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
                    chunks.add(bytes)
                    if (isCached(request.url.toString())) {
                        throw ItemAlreadyCachedException("url: ${request.url}")
                    }
                } catch (e: Exception) {
                    // 任何一个分块失败，取消所有未完成的任务
                    futures.forEach { it.cancel(true) }
                    if ((e is IOException && e.cause is ItemAlreadyCachedException) || e is ItemAlreadyCachedException) {
                        return Response.Builder()
                            .request(request)
                            .code(404)
                            .protocol(Protocol.HTTP_1_1)
                            .message("Item was already Cached")
                            .build()
                    }
                    throw IOException("Chunk download failed", e)
                }
            }

            // 4. 合并所有分块数据
            val mergedData = chunks.fold(ByteArrayOutputStream(totalSize.toInt())) { acc, bytes ->
                acc.write(bytes)
                acc
            }.toByteArray()

            // 5. 构建新的成功响应
            val responseBody = mergedData.toResponseBody(request.body?.contentType())
            return Response.Builder()
                .request(request)
                .protocol(Protocol.HTTP_1_1)
                .code(200)
                .message("OK")
                .body(responseBody)
                .addHeader("Content-Length", mergedData.size.toString())
                .build()
        } catch (e: Exception) {
            e.printStackTrace()
            return chain.proceed(request)
        } finally {
            executor.shutdownNow()
        }
    }

    private fun shouldUseMultithreadDownload(request: Request): Boolean {
        val url = request.url.toString().lowercase()
        return (url.endsWith(".jpg") || url.endsWith(".jpeg") ||
                url.endsWith(".png") || url.endsWith(".webp") ||
                url.endsWith(".gif") || url.endsWith(".bmp")) && url.contains("master1200")
                && ConfigUtil.useMultithread.state.value
    }

    // 通过 HEAD 请求获取文件总大小，同时验证服务器是否支持 Range
    private fun getTotalSize(chain: Interceptor.Chain, request: Request): Long? {
        val headRequest = request.newBuilder()
            .header("Range", "bytes=0-0")
            .build()

        val headResponse = chain.proceed(headRequest)
        try {
            // 服务器支持 Range 时应返回 206 Partial Content
            if (headResponse.code != 206) {
                return null
            }
            val contentRange = headResponse.header("Content-Range") ?: return null
            return contentRange.substringAfter('/').toLongOrNull()
        } finally {
            headResponse.closeQuietly()
        }
    }
}