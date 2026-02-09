package com.xlrr.roambendom

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import coil3.ImageLoader
import coil3.compose.setSingletonImageLoaderFactory
import coil3.disk.DiskCache
import coil3.network.okhttp.OkHttpNetworkFetcherFactory
import com.xlrr.roambendom.data.SearchResult
import com.xlrr.roambendom.network.NHWebHelper
import com.xlrr.roambendom.network.NetHelper.getTrustManagers
import com.xlrr.roambendom.network.RBDDns
import com.xlrr.roambendom.network.RBDSocketFactory
import com.xlrr.roambendom.ui.MainContent
import com.xlrr.roambendom.utils.GlobalData
import com.xlrr.roambendom.utils.WindowSizeBox
import io.github.vinceglb.filekit.coil.addPlatformFileSupport
import okhttp3.OkHttpClient
import javax.net.ssl.SSLSocketFactory
import javax.net.ssl.X509TrustManager

val LocalAnimatedVisibilityScope =
    compositionLocalOf<AnimatedVisibilityScope> { error("not provided") }
@OptIn(ExperimentalSharedTransitionApi::class)
val LocalSharedTransitionScope = compositionLocalOf<SharedTransitionScope> { error("not provided") }

@Composable
fun App() {
    setupCoil()
    MaterialTheme {
        WindowSizeBox(Modifier.fillMaxWidth()) {
            SharedTransitionLayout {
                CompositionLocalProvider(LocalSharedTransitionScope provides this) {
                    MainContent()
                }
            }
        }
    }
}

@Suppress("ComposableNaming")
@Composable
fun setupCoil() {
    setSingletonImageLoaderFactory { context ->
        ImageLoader.Builder(context)
            .components {
                addPlatformFileSupport()
                add(
                    OkHttpNetworkFetcherFactory(
                        callFactory = {
                            val ssls = RBDSocketFactory(SSLSocketFactory.getDefault() as SSLSocketFactory)
                            val mgs = getTrustManagers()
                            OkHttpClient.Builder()
                                .dns(RBDDns).apply {
                                    if (mgs?.isNotEmpty() == true && mgs[0] is X509TrustManager) {
                                        sslSocketFactory(
                                            ssls ,
                                            mgs[0] as X509TrustManager
                                        )
                                    }
                                }.build()

                        }
                    )
                )
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(GlobalData.cacheDir)
                    .maxSizeBytes(250L * 1024 * 1024) // 250MB
                    .build()
            }
            .build()
    }
}