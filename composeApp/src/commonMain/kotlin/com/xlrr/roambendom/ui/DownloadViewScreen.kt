package com.xlrr.roambendom.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.xlrr.roambendom.progressive.SharedPainterManager
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.milliseconds

@Composable
fun DownloadViewScreen(modifier: Modifier) {
    var tick by remember { mutableIntStateOf(0) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(500.milliseconds)
            tick++
        }
    }
    val entries = remember(tick) {
        SharedPainterManager.map.entries
            .filter { it.value.getFileSize() > 0 && it.value.fileSize() < it.value.getFileSize() }
            .toList()
    }
    LazyColumn(modifier = modifier) {
        item("DownloadInfo") {
            Row {
                Text("Total ${entries.size}")
            }
        }
        items(entries, { it.key }) {
            val size = it.value.getFileSize().toFloat()
            ListItem(
                {
                    Text(it.key, Modifier.padding(16.dp))
                },
                Modifier.padding(12.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.secondaryContainer),
                supportingContent = {
                    LinearProgressIndicator(
                        { (it.value.fileSize() / size).coerceIn(0f, 1f) },
                        Modifier.fillMaxWidth(0.8f).padding(12.dp, 12.dp)
                    )
                }
            )
        }
    }
}
