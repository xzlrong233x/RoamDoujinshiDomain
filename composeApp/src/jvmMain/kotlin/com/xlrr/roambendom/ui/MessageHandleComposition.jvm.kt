package com.xlrr.roambendom.ui

import androidx.compose.animation.core.animateIntOffsetAsState
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.min
import com.xlrr.roambendom.LocalSharedTransitionScope
import com.xlrr.roambendom.manager.MessageEntry
import com.xlrr.roambendom.manager.MessageManager
import com.xlrr.roambendom.utils.LocalWindowSize
import kotlinx.coroutines.delay
import java.awt.GraphicsEnvironment
import kotlin.time.Clock
import kotlin.time.Duration.Companion.milliseconds

val MESSAGE_ITEM_WIDTH = 366.dp

/** 进度条步进间隔：4fps。用低频状态写入替代逐帧 Animatable，避免整窗每帧重绘。 */
private val PROGRESS_STEP_MS = 1000L / GraphicsEnvironment.getLocalGraphicsEnvironment().defaultScreenDevice.displayMode.refreshRate

@Composable
actual fun MessageHandleComposition(content: @Composable (() -> Unit)) {
    val entries = MessageManager.entries
    val mx = min(LocalWindowSize.current.width * 0.35f, MESSAGE_ITEM_WIDTH)
    val ls = rememberLazyListState()
    // key 只用条目数量（int），不会像旧实现那样每帧用新 Map 实例重启 effect。
    LaunchedEffect(entries.size) {
        if (entries.isNotEmpty() && ls.firstVisibleItemIndex > 0) {
            ls.animateScrollToItem(0)
        }
    }
    Box(Modifier.fillMaxSize()) {
        content()
        Box(Modifier.align(Alignment.TopEnd)) {
            with(LocalSharedTransitionScope.current) {
                LazyColumn(
                    Modifier.width(mx),
                    ls
                ) {
                    items(entries, key = { it.id }) { entry ->
                        MessageItemComposition(entry) { MessageManager.remove(entry.id) }
                    }
                }
            }
        }
    }
}

@Composable
private fun LazyItemScope.MessageItemComposition(entry: MessageEntry, onDismiss: () -> Unit) {
    val id = entry.id
    val messageData = entry.data
    val mx = min(LocalWindowSize.current.width * 0.35f, MESSAGE_ITEM_WIDTH)

    // 滑入动画：只在条目首次出现时运行约 300ms。
    var enb by remember(id) { mutableStateOf(false) }
    val off by animateIntOffsetAsState(
        targetValue = if (enb) IntOffset(0, 0) else IntOffset(mx.value.toInt(), 0),
        label = "messageSlideIn"
    )
    LaunchedEffect(id) {
        enb = true
    }

    // 超时进度：低频步进（4fps）写状态，drawBehind 只在 draw 阶段读取，
    // 因此不会触发重组，也不会像逐帧 Animatable 那样让整窗每帧重绘。
    // 计时起点是条目创建时刻：条目被 LazyColumn 回收 / 页面重建后按墙钟已流逝时间续算，不会重新计时。
    val totalMs = messageData.timeOut.inWholeMilliseconds.coerceAtLeast(1L)
    val progress = remember(id) {
        val elapsed = (Clock.System.now().toEpochMilliseconds() - entry.createdAt).coerceIn(0L, totalMs)
        mutableFloatStateOf(elapsed.toFloat() / totalMs)
    }
    LaunchedEffect(id) {
        // 每一轮都按墙钟重新计算已流逝时间（而不是累加 delay 的名义步长）：
        // 这样 delay 被调度延迟时不会把消息展示时长整体拖长。
        while (true) {
            val elapsed = (Clock.System.now().toEpochMilliseconds() - entry.createdAt).coerceIn(0L, totalMs)
            if (elapsed >= totalMs) break
            progress.floatValue = elapsed.toFloat() / totalMs
            delay(minOf(PROGRESS_STEP_MS, totalMs - elapsed).milliseconds)
        }
        progress.floatValue = 1f
        onDismiss()
    }

    Surface(
        shape = RoundedCornerShape(12.dp),
        shadowElevation = 4.dp,
        modifier = Modifier.width(mx).padding(2.dp, 3.dp)
            .offset { off.times(this.density) }
            .animateItem()
    ) {
        Box(
            Modifier.fillMaxSize()
                .drawBehind {
                    val p = progress.floatValue
                    drawRect(
                        color = Color(p, 1f - p, 0f),
                        size = Size(size.width * p, 4.dp.toPx())
                    )
                }
                .clickable {
                    if (messageData.click != null) {
                        messageData.click.invoke { onDismiss() }
                    } else {
                        onDismiss()
                    }
                }
        ) {
            Column(
                Modifier.padding(8.dp, 12.dp)
            ) {
                Text(messageData.title.getComposeOrString())
                Spacer(Modifier.height(4.dp))
                Text(messageData.content.getComposeOrString(), style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}
