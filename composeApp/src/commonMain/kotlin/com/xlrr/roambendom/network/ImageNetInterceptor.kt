package com.xlrr.roambendom.network

import coil3.PlatformContext
import coil3.SingletonImageLoader
import coil3.intercept.Interceptor
import coil3.network.HttpException
import coil3.request.ErrorResult
import coil3.request.ImageResult

class ImageNetInterceptor(
    val platformContext: PlatformContext,
    val maxCount: Int = 5
) : Interceptor {
    override suspend fun intercept(chain: Interceptor.Chain): ImageResult {
        var t = chain.proceed()
        var c = 0
//        if (t is ErrorResult && t.throwable is HttpException && t.throwable.message?.contains("943") == true) {
//            t.request.diskCacheKey?.let { SingletonImageLoader.get(chain.request.context).diskCache?.remove(it) }
//            t = chain.proceed()
//        }
        while (t is ErrorResult && t.throwable.message == "timeout" && c < maxCount) {
            c++
            t = chain.proceed()
        }
        return t
    }
}