package com.xlrr.roambendom.ui.loginScreen

import androidx.browser.customtabs.CustomTabsIntent
import androidx.core.net.toUri
import androidx.lifecycle.ViewModel
import coil3.PlatformContext
import com.xlrr.roambendom.utils.PixivTokenUtil

actual class LoginViewModel : ViewModel() {
    val intent = CustomTabsIntent.Builder()
        .build()
    actual fun launchBrowser(platformContext: PlatformContext) {
        PixivTokenUtil.genCodeChallenge()
        val url = PixivTokenUtil.genPixivLoginUrl()
        intent.launchUrl(platformContext,url.toUri())
    }
}