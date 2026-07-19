package com.xlrr.roambendom.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntSize
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import com.xlrr.roambendom.LocalSharedTransitionScope
import com.xlrr.roambendom.config.ConfigUtil
import com.xlrr.roambendom.data.CLanguage
import com.xlrr.roambendom.data.SearchItemData
import com.xlrr.roambendom.model.detail.asDetail
import com.xlrr.roambendom.model.search.SearchParameterModel
import com.xlrr.roambendom.nav.Routes
import com.xlrr.roambendom.network.PIXIVApiHelper
import com.xlrr.roambendom.utils.*
import org.jetbrains.compose.resources.stringResource
import roambendom.composeapp.generated.resources.Res
import roambendom.composeapp.generated.resources.ai_warn
import roambendom.composeapp.generated.resources.new_updates
import roambendom.composeapp.generated.resources.pixiv_login_btn
import roambendom.composeapp.generated.resources.pixiv_no_login_info
import roambendom.composeapp.generated.resources.popular

enum class HomeSelection {
    NH,
    PIXIV
}

class HomeViewModel() : ViewModel() {
    var local: HomeSelection by mutableStateOf(HomeSelection.NH)
    var error: Throwable? by mutableStateOf(null)
    var loading by mutableStateOf(false)
    var tempLeave by mutableStateOf(false)  //这个变量主要是为了标记是否是暂时离开（即Home被压在栈下）
    val spm = SearchParameterModel("")
    val pixivSPM = SearchParameterModel("").config {
        searchFunction = PIXIVApiHelper.nextUrlSearchFunction { _, offset, viewed ->
            PIXIVApiHelper.illustRecommend(offset, viewed)
        }
    }

    suspend fun reload() {
        if (local == HomeSelection.NH) {
            spm.reload()
        } else {
            pixivSPM.reload()
        }
    }

    fun clear() {
        pixivSPM.clear()
        spm.clear()
    }
}

@Composable
fun ShowSearchItem(it: SearchItemData, orColumn: Boolean = true) {
    Box() {
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
            },
            it.isAnimation,
            onClick = {
                GlobalData.nav.push(Routes.Root.Detail(it.asDetail()))
            },
            imgLabel = it.thumb,
            toColumn = orColumn
        )
    }
}

@Composable
fun HomeScreen(modifier: Modifier = Modifier, viewModel: HomeViewModel = viewModel { HomeViewModel() }) {
    val small = LocalWindowSize.current.width < SmallScreenDpLine
    LaunchedEffect(viewModel.local) {
        GlobalData.homeContentSelection = viewModel.local
        if (!viewModel.tempLeave || (viewModel.local == HomeSelection.NH && viewModel.spm.content.isEmpty())) {
            viewModel.reload()
        } else {
            viewModel.tempLeave = false
        }
    }
    DisposableEffect(Unit) {
        onDispose {
            GlobalData.homeContentSelection = null
            if (GlobalData.nav.backStack.any { it is Routes.Root.Home }) {
                viewModel.tempLeave = true
            } else {
                viewModel.clear()
            }
        }
    }
    val ss = rememberCoroutineScope()
    val curSpm = if (viewModel.local == HomeSelection.NH) viewModel.spm else viewModel.pixivSPM
    StandardSearchLikeWithUp(
        curSpm,
        modifier
    ) {
        SearchContent(
            modifier,
            curSpm,
            defaultStaggeredGridCell(LocalWindowSize.current.width),
            small && !ConfigUtil.forceGrid.value && viewModel.local == HomeSelection.NH,
            {
                Column(Modifier.fillMaxWidth()) {
                    SingleChoiceSegmentedButtonRow {
                        HomeSelection.entries.forEachIndexed { index, selection ->
                            SegmentedButton(
                                viewModel.local == selection,
                                {
                                    viewModel.local = selection
                                    if (selection == HomeSelection.PIXIV) {
                                        viewModel.spm.reset()
                                    }
                                },
                                SegmentedButtonDefaults.itemShape(
                                    index = index,
                                    count = 2
                                ),
                            ) {
                                Text(selection.toString())
                            }
                        }
                    }
                    if (viewModel.local == HomeSelection.PIXIV
                        && viewModel.pixivSPM.content.isEmpty()
                        && viewModel.pixivSPM.completed) {
                        Text(stringResource(Res.string.pixiv_no_login_info))
                        Button({GlobalData.nav.push(Routes.Auth.Choose())}) {
                            Text(stringResource(Res.string.pixiv_login_btn))
                        }
                    }
                }
            },
            { spm ->
                if (viewModel.local == HomeSelection.NH) {
                    item("popular") {
                        Text(stringResource(Res.string.popular), style = MaterialTheme.typography.headlineSmall)
                    }
                    items(spm.content.distinct().subList(0, 5), { "popular${it.id}" }) {
                        with(LocalSharedTransitionScope.current) {
                            ShowSearchItem(it, false)
                        }
                    }
                    item("lastest") {
                        Text(stringResource(Res.string.new_updates), style = MaterialTheme.typography.headlineSmall)
                    }
                    items(spm.content.distinct().subList(5, spm.content.size), { "lasest${it.id}" }) {
                        with(LocalSharedTransitionScope.current) {
                            ShowSearchItem(it, false)
                        }
                    }
                } else {
                    items(spm.content.distinct(), {it.id}) {
                        with(LocalSharedTransitionScope.current) {
                            ShowSearchItem(it, true)
                        }
                    }
                }
            },
            {spm ->
                if (viewModel.local == HomeSelection.NH) {
                    item("popular", span = StaggeredGridItemSpan.FullLine) {
                        Text(stringResource(Res.string.popular), style = MaterialTheme.typography.headlineSmall)
                    }
                    items(spm.content.distinct().subList(0, 5), { "popular${it.id}" }) {
                        with(LocalSharedTransitionScope.current) {
                            ShowSearchItem(it)
                        }
                    }
                    item("lastest", span = StaggeredGridItemSpan.FullLine) {
                        Text(stringResource(Res.string.new_updates), style = MaterialTheme.typography.headlineSmall)
                    }
                    items(spm.content.distinct().subList(5, spm.content.size), { "lasest${it.id}" }) {
                        with(LocalSharedTransitionScope.current) {
                            ShowSearchItem(it)
                        }
                    }
                } else {
                    items(spm.content.distinct(), {it.id}) {
                        with(LocalSharedTransitionScope.current) {
                            ShowSearchItem(it, true)
                        }
                    }
                }
            },
            ss = ss
        )
    }
}

@Preview
@Composable
fun test() {
    HomeScreen()
}