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
        var t = chain.proceed()
        var c = 0
        while (t is ErrorResult && t.throwable.message == "timeout" && c < maxCount) {
            c++
            t = chain.proceed()
        }
        return t
    }
}