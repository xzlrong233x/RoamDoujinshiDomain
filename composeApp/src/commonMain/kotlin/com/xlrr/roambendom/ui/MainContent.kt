package com.xlrr.roambendom.ui

import androidx.compose.animation.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import com.xlrr.roambendom.LocalAnimatedVisibilityScope
import com.xlrr.roambendom.nav.Routes
import com.xlrr.roambendom.ui.loginScreen.TotalAuthScreen
import com.xlrr.roambendom.utils.GlobalData
import org.jetbrains.compose.resources.stringResource
import roambendom.composeapp.generated.resources.Res
import roambendom.composeapp.generated.resources.nothing


@Composable
fun MainContent() {
    val state = GlobalData.nav.backStack.last()
    AnimatedContent(
        targetState = state,
        contentKey = {
            when (it) {
                is Routes.Root -> "root"
                is Routes.Auth -> "auth"
                else -> it
            }
        },
        modifier = Modifier,
        transitionSpec = {
            if (state is Routes.Root) {
                slideInHorizontally(initialOffsetX = { fullW ->  -fullW}).togetherWith(
                    slideOutHorizontally(targetOffsetX = {w -> w}) + fadeOut())
            } else {
                slideInHorizontally(initialOffsetX = { fullW ->  fullW}).togetherWith(
                    slideOutHorizontally(targetOffsetX = {w -> -w}) + fadeOut())
            }
        }
    ) {
        CompositionLocalProvider(
            LocalAnimatedVisibilityScope provides this
        ) {
            MessageHandleComposition {
                when (it) {
                    is Routes.Root -> RootScreen()
                    is Routes.Artwork -> ArtworkViewScreen(it.artworkInfo, pageChange = it.pageChange)
                    is Routes.Auth -> TotalAuthScreen(Modifier)
                    else -> Text(stringResource(Res.string.nothing))
                }
            }
        }
    }
}