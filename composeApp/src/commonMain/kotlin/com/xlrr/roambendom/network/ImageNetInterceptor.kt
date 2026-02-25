package com.xlrr.roambendom.network

import coil3.PlatformContext
import coil3.intercept.Interceptor
import coil3.request.ErrorResult
import coil3.request.ImageResult
import coil3.request.SuccessResult

class ImageNetInterceptor(
    val platformContext: PlatformContext,
    val maxCount: Int = 5
) : Interceptor {
    override suspend fun intercept(chain: Interceptor.Chain): ImageResult {
        if (chain.request.data.let { if (it is String) it.contains("pixiv") || it.contains("pximg") else false }) {
            println("start request ${chain.request.data}")
        }
        val tm = System.currentTimeMillis()
        var t = chain.proceed()
        var c = 0
        while (t is ErrorResult && t.throwable.message == "timeout" && c < maxCount) {
            c++
            println("${chain.request.data} retry $c time: ${System.currentTimeMillis() - tm}")
            t = chain.proceed()
        }
        println(StringBuilder("${chain.request.data} time: ${System.currentTimeMillis() - tm}").apply {
            t.image?.let { append(" size: ${it.size}") }
            if (t is SuccessResult) {
                append(" source: ${t.dataSource}")
                t.diskCacheKey?.let {
                    append(" dk: $it")
                }
                t.memoryCacheKey?.let {
                    append(" mk: $it")
                }
            } else if (t is ErrorResult) {
                t.throwable.message?.let {
                    append(" error msg: $it")
                }
            }
        })
        return t
    }
}