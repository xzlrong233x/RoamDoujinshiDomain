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
    //无传参，url里需要自带
    private external fun baseHttpGetImpl(url: String, candidates: String, token: String): String
    private external fun baseHttpPostImpl(url: String, form: String, candidates: String): String

    // 公开 suspend 包装（在 IO 线程执行 JNI 调用，不阻塞 UI / 协程调度器）
    suspend fun refreshOAuthToken(refreshToken: String): String =
        withContext(Dispatchers.IO) { refreshOAuthTokenImpl(refreshToken) }

    suspend fun requestOAuthToken(code: String, codeVerifier: String): String =
        withContext(Dispatchers.IO) { requestOAuthTokenImpl(code, codeVerifier) }

    /**
     * 通用 GET，返回响应体文本。
     *
     * @param url 完整 url，查询参数需要自己拼进 url
     * @param candidates ECH 连接地址候补（逗号分隔的 IP）。
     *   cloudflare-ech.com 的IP被间歇性封锁，
     *   但经测试，直接指向源站点的cf IP也可以代替cloudflare-ech.com 的IP。
     * @param token P站API的令牌，如果是app-api.pixiv.net域名就要用
     * @throws RuntimeException 非 2xx 响应（原生层抛出）
     * */
    suspend fun baseHttpGet(url: String, candidates: String = "", token: String = ""): String =
        withContext(Dispatchers.IO) { baseHttpGetImpl(url, candidates, token) }

    /**
     * 通用 POST，返回响应体文本。
     *
     * @param url 完整 url
     * @param form 已序列化好的请求体，按 application/json 原样发出
     * @param candidates 同 [baseHttpGet]
     * @throws RuntimeException 非 2xx 响应（原生层抛出）
     * */
    suspend fun baseHttpPost(url: String, form: String, candidates: String = ""): String =
        withContext(Dispatchers.IO) { baseHttpPostImpl(url, form, candidates) }
}
