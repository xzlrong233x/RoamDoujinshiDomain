package com.xlrr.roambendom.ui

import androidx.compose.animation.core.animateIntOffsetAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.min
import com.xlrr.roambendom.LocalSharedTransitionScope
import com.xlrr.roambendom.data.MessageData
import com.xlrr.roambendom.manager.MessageManager
import com.xlrr.roambendom.utils.LocalWindowSize
import kotlin.time.Clock
import kotlin.time.Duration.Companion.seconds
import kotlin.time.DurationUnit
import kotlin.time.Instant

val MESSAGE_ITEM_WIDTH = 366.dp

@Composable
actual fun MessageHandleComposition(content: @Composable (() -> Unit)) {
    val map = MessageManager.get()
    val mx = min(LocalWindowSize.current.width * 0.35f, MESSAGE_ITEM_WIDTH)
    val ls = rememberLazyListState()
    var t by remember { mutableIntStateOf(0) }
    LaunchedEffect(map) {
        ls.animateScrollToItem(0)
    }
    Box(Modifier.fillMaxSize()) {
        content()
        Box(Modifier.align(Alignment.TopEnd)) {
            with(LocalSharedTransitionScope.current) {
                LazyColumn(
                    Modifier.width(mx),
                    ls
                ) {
                    items(
                        map.entries.toList()
                            .sortedWith { entry, entry1 -> (entry1.value - entry.value).toInt(DurationUnit.MILLISECONDS) },
                        {x -> x.hashCode()}
                    ) { i ->
                        MessageItemComposition(i.key, i.value)
                    }
                }
            }
        }
    }
    for (X in map) {
        if (Clock.System.now() - X.value > X.key.timeOut + 3.seconds) {
            MessageManager.delectMessage(X.key)
        }
    }
    t++
}

@Composable
private fun LazyItemScope.MessageItemComposition(messageData: MessageData, time: Instant) {
    var t by remember { mutableIntStateOf(0) }
    var enb by remember(messageData) { mutableStateOf(false) }
    val mx = min(LocalWindowSize.current.width * 0.35f, MESSAGE_ITEM_WIDTH)
    val off by animateIntOffsetAsState(
        if (enb) IntOffset(0,0) else IntOffset(mx.value.toInt(), 0),
    ) {
        if (!enb) {
            MessageManager.delectMessage(messageData)
        }
    }
    val mod = Modifier
    LaunchedEffect(messageData) {
        enb = true
    }
    Surface(
        shape = RoundedCornerShape(12.dp),
        shadowElevation = 4.dp,
        modifier = Modifier.width(mx).padding(2.dp, 3.dp)
            .offset { off.times(this.density) }
            .animateItem()
    ) {
        Box(Modifier.fillMaxSize().clickable {
            if (messageData.click != null) {
                messageData.click.invoke {
                    enb = false
                }
            } else {
                enb = false
            }
        }) {
            val p = ((Clock.System.now() - time) / messageData.timeOut).toFloat()
            Spacer(
                Modifier.height(4.dp).background(
                    Color(p, 1f - p, 0f)
                ).width(
                    mx * p
                )
            )
            Column(
                mod.padding(8.dp, 12.dp)
            ) {

                Text(messageData.title.getComposeOrString())
                Spacer(Modifier.height(4.dp))
                Text(messageData.content.getComposeOrString(), style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
    if (Clock.System.now() - time > messageData.timeOut) {
        enb = false
    }
    t++
}