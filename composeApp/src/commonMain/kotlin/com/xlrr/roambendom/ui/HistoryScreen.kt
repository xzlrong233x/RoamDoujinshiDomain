package com.xlrr.roambendom.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.IntSize
import com.xlrr.roambendom.LocalSharedTransitionScope
import com.xlrr.roambendom.config.ConfigUtil
import com.xlrr.roambendom.data.CLanguage
import com.xlrr.roambendom.data.SearchItemData
import com.xlrr.roambendom.manager.HistoryDataManager
import com.xlrr.roambendom.model.detail.asDetail
import com.xlrr.roambendom.model.search.SearchParameterModel
import com.xlrr.roambendom.nav.Routes
import com.xlrr.roambendom.utils.*
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import roambendom.composeapp.generated.resources.Res
import roambendom.composeapp.generated.resources.ai_warn
import roambendom.composeapp.generated.resources.clear_all
import roambendom.composeapp.generated.resources.delete_icon
import roambendom.composeapp.generated.resources.history_title

@Composable
private fun ShowHistoryItem(it: SearchItemData, modifier: Modifier,
                            remove: (String) -> Unit, orColumn: Boolean = true) {
    Box(modifier) {
        ItemInfoCardWithShared(
            it.thumb,
            it.pageCount,
            it.title,
            it.restriction,
            if (it.width <= 0 || it.height <= 0) null else IntSize(it.width, it.height),
            it.lang.let { x ->
                if (x != CLanguage.Unknown) x.toString().lowercase() else it.author.ifEmpty { null }
            },
            {
                if (it.ai) Text(stringResource(Res.string.ai_warn))
                if (it.time > 0) Text(TimeUtil.formatTime(it.time))
                IconButton({
                    remove(it.uid())
                }, Modifier.align(Alignment.End)) {
                    Icon(painterResource(Res.drawable.delete_icon), "delete icon")
                }
            },
            it.isAnimation,
            imgLabel = it.thumb,
            onClick = {
                GlobalData.nav.push(Routes.Root.Detail(it.asDetail()))
            },
            toColumn = orColumn
        )
    }
}

@Composable
fun HistoryScreen(modifier: Modifier, searchParameterModel: SearchParameterModel) {
    val ss = rememberCoroutineScope()
    val mx = LocalWindowSize.current.width
    StandardSearchLikeWithUp(searchParameterModel, modifier) {
        SearchContent(
            modifier,
            searchParameterModel,
            defaultStaggeredGridCell(LocalWindowSize.current.width),
            mx < SmallScreenDpLine && !ConfigUtil.forceGrid.value,
            header = {
                Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                    Text(stringResource(Res.string.history_title), style = MaterialTheme.typography.headlineMedium)
                    Button({
                        HistoryDataManager.removeAll()
                        ss.launch {
                            searchParameterModel.reload()
                        }
                    }) {
                        Text(stringResource(Res.string.clear_all))
                    }
                }
            },
            listMain = {spm ->
                items(spm.content.distinct(), {it.uid()}) {
                    with(LocalSharedTransitionScope.current) {
                        ShowHistoryItem(it, Modifier.animateItem(),{ s ->
                            spm.content.remove(it)
                            HistoryDataManager.remove(s)
                        }, false)
                    }
                }
            },
            gridMain = {spm ->
                items(spm.content.distinct(), {it.uid()}) {
                    with(LocalSharedTransitionScope.current) {
                        ShowHistoryItem(it, Modifier.animateItem(),{s ->
                            spm.content.remove(it)
                            HistoryDataManager.remove(s)
                        }, true)
                    }
                }
            },
            ss = ss)
    }
}