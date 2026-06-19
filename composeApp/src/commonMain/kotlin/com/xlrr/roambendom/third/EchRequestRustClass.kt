package com.xlrr.roambendom.third

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object EchRequestRustClass {
    // 暂时没有将所有网络请求全加入ech的计划
    init {
        loadEchRequestLibrary()
    }

    // 私有 JNI 绑定（阻塞调用线程，不可直接在 UI 线程调用）
    private external fun refreshOAuthTokenImpl(refreshToken: String): String
    private external fun requestOAuthTokenImpl(code: String, codeVerifier: String): String

    // 公开 suspend 包装（在 IO 线程执行 JNI 调用，不阻塞 UI / 协程调度器）
    suspend fun refreshOAuthToken(refreshToken: String): String =
        withContext(Dispatchers.IO) { refreshOAuthTokenImpl(refreshToken) }

    suspend fun requestOAuthToken(code: String, codeVerifier: String): String =
        withContext(Dispatchers.IO) { requestOAuthTokenImpl(code, codeVerifier) }
}
