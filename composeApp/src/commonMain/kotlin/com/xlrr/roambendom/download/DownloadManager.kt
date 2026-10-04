package com.xlrr.roambendom.download

import coil3.PlatformContext
import com.xlrr.roambendom.data.MessageData
import com.xlrr.roambendom.manager.MessageManager
import com.xlrr.roambendom.utils.orResource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import roambendom.composeapp.generated.resources.Res
import roambendom.composeapp.generated.resources.message_save_failed_title
import roambendom.composeapp.generated.resources.message_save_successful_title

object DownloadManager {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    fun submit(context: PlatformContext, items: List<DownloadItem>, target: SaveTarget) {
        if (items.isEmpty()) return
        scope.launch {
            val ok = target.save(items, context)
            MessageManager.addMessage(
                MessageData(
                    (if (ok > 0) Res.string.message_save_successful_title
                    else Res.string.message_save_failed_title).orResource(),
                    "$ok/${items.size}".orResource()
                )
            )
        }
    }
}
