package com.xlrr.roambendom.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.animation.with
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.xlrr.roambendom.nav.Routes
import com.xlrr.roambendom.utils.GlobalData


@Composable
fun MainContent() {
    val state = GlobalData.nav.backStack.last()
    AnimatedContent(
        targetState = state,
        contentKey = {
            when (it) {
                is Routes.Root -> "root"
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
        when (it) {
            is Routes.Root -> RootScreen()
            is Routes.Artwork -> ArtworkViewScreen(it.artworkInfo)
            else -> Text("空空如也")
        }
    }
}