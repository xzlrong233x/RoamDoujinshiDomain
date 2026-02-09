package com.xlrr.roambendom.ui

import androidx.compose.animation.AnimatedContent
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
    modifier = Modifier
    ) {
        when (it) {
            is Routes.Root -> RootScreen()
            else -> Text("空空如也")
        }
    }
}