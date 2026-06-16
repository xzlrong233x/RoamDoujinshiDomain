package com.xlrr.roambendom.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.xlrr.roambendom.LocalSharedTransitionScope
import com.xlrr.roambendom.data.CLanguage
import com.xlrr.roambendom.data.SearchItemData
import com.xlrr.roambendom.manager.FavoriteDataManager
import com.xlrr.roambendom.model.FavoriteScreenModel
import com.xlrr.roambendom.model.detail.asDetail
import com.xlrr.roambendom.nav.Routes
import com.xlrr.roambendom.utils.*
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.painterResource
import roambendom.composeapp.generated.resources.Res
import roambendom.composeapp.generated.resources.folder_icon
import kotlin.math.ceil
import kotlin.math.max

@Composable
private fun ShowFavoriteItem(
    it: SearchItemData,
    modifier: Modifier,
    deleteFunc: (String) -> Unit,
    preToNewFolder: (String) -> Unit,
    moveEnable: () -> Boolean,
) {
    CommandBox(
        modifier,
        listOf(
            CommandData(
                "删除",
                { deleteFunc(it.uid()) }
            ),
            CommandData(
                "移动到",
                { preToNewFolder(it.uid()) },
                moveEnable
            )
        )
    ) {f ->
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
                if (it.ai) Text("*有AI参与的作品")
                if (it.time > 0) Text(TimeUtil.formatTime(it.time))
            },
            it.isAnimation,
            imgLabel = it.thumb,
            onClick = {
                GlobalData.nav.push(Routes.Root.Detail(it.asDetail()))
            },
            onLongClick = {
                f(true)
            },
            toColumn = true
        )
    }
}

@Composable
private fun ShowFolderItem(
    toPath: String,
    name: String,
    modifier: Modifier,
    deleteFolder: (String, Boolean) -> Unit,
) {
    CommandBox(modifier, listOf(
        CommandData(
            "删除文件夹",
            { deleteFolder(toPath, false) }
        ),
        CommandData(
            "删除文件夹及内容",
            { deleteFolder(toPath, true) }
        )
    )) {
        Surface(
            shape = RoundedCornerShape(12.dp),
            shadowElevation = 4.dp,
            modifier = Modifier.padding(5.dp, 0.dp)
        ) {
            with(LocalSharedTransitionScope.current) {
                Column(
                    Modifier.combinedClickable(
                        onClick = {
                            GlobalData.nav.push(Routes.Root.Favorite(toPath))
                        },
                        onLongClick = {it(true)},
                    ).padding(6.dp).fillMaxSize()
                ) {
                    Image(
                        painterResource(Res.drawable.folder_icon),
                        null,
                        Modifier.fillMaxWidth().heightIn(145.dp),
                        contentScale = ContentScale.FillBounds
                    )
                    Text(name, Modifier.fillMaxWidth())
                }
            }
        }
    }
}

@Composable
private fun MakeFolderAsk(
    show: Boolean,
    nowPath: String,
    favoriteScreenModel: FavoriteScreenModel,
    onDismiss: () -> Unit
) {
    var pathName by remember(nowPath) { mutableStateOf("") }
    var folderName by remember(nowPath) { mutableStateOf("") }
    val sc = rememberCoroutineScope()
    if (show) {
        AlertDialog(onDismiss, {
            TextButton(
                {
                    FavoriteDataManager.addFolder(pathName, folderName, nowPath)
                    sc.launch {
                        favoriteScreenModel.reload()
                    }
                    onDismiss()
                },
                enabled = nowPath.isNotEmpty() && folderName.isNotEmpty() && pathName.isNotEmpty()
            ) {
                Text("保存")
            }
        }, text = {
            Column {
                OutlinedTextField(pathName, { pathName = it }, label = { Text("文件夹路径") })
                OutlinedTextField(folderName, { folderName = it }, label = { Text("文件夹名称") })
            }
        })
    }
}

@Composable
fun PrepareMoveItemAsk(uid: String, favoriteScreenModel: FavoriteScreenModel, onDismiss: () -> Unit) {
    val sc = rememberCoroutineScope()
    if (uid.isNotEmpty()) {
        AlertDialog(onDismiss, {
            TextButton(
                onDismiss
            ) {
                Text("取消")
            }
        }, text = {
            val p = FavoriteDataManager.parent(favoriteScreenModel.path)
            val item = FavoriteDataManager.content.folderInfo[p]
            LazyColumn {
                if (item != null && p != favoriteScreenModel.path) {
                    item {
                        ListItem(
                            {
                                Text("${item.name} (上一级)")
                            },
                            Modifier.clickable {
                                FavoriteDataManager.moveItem(uid, p)
                                favoriteScreenModel.content.removeIf { s -> s.uid() == uid }
                                onDismiss()
                            },
                            supportingContent = {
                                Text(p)
                            }
                        )
                    }
                }
                items(favoriteScreenModel.folderInfos.toList(), {it.first}) {
                    ListItem(
                        {
                            Text(it.second)
                        },
                        Modifier.clickable {
                            FavoriteDataManager.moveItem(uid, it.first)
                            favoriteScreenModel.content.removeIf { s -> s.uid() == uid }
                            onDismiss()
                        },
                        supportingContent = {
                            Text(it.first)
                        }
                    )
                }
            }
        })
    }
}

@Composable
fun FavoriteScreen(modifier: Modifier, searchParameterModel: FavoriteScreenModel, path: String) {
    val ss = rememberCoroutineScope()
    val mx = LocalWindowSize.current.width
    var showAskMaker by remember { mutableStateOf(false) }
    var preToUid by remember { mutableStateOf("") }
    LaunchedEffect(searchParameterModel) {
        if (searchParameterModel.content.isNotEmpty()) {
            searchParameterModel.reload()
        }
    }
    StandardSearchLikeWithUp(searchParameterModel, modifier) {
        CommandBox(Modifier, listOf(), publicCommands = listOf(
            CommandData(
                "添加文件夹",
                { showAskMaker = true }
            )
        )) {
            SearchContent(modifier, searchParameterModel, StaggeredGridCells.Fixed(
                ceil(mx.value / 216f).coerceIn(1f, max(6f, mx.value / 216 - 2)).toInt()
            ), false,
                header = {},
                gridMain = {spm ->
                    items(searchParameterModel.folderInfos.toList(), {it.first}) {
                        with(LocalSharedTransitionScope.current) {
                            ShowFolderItem(
                                it.first,
                                it.second,
                                Modifier.animateItem(),
                                {s, b ->
                                    FavoriteDataManager.removeFolder(s, b)
                                    ss.launch {
                                        searchParameterModel.reload()
                                    }
                                }
                            )
                        }
                    }
                    items(spm.content.distinct(), {it.uid()}) {
                        if (it.id == "theElementIsNotDisplayed") return@items
                        with(LocalSharedTransitionScope.current) {
                            ShowFavoriteItem(it, Modifier.animateItem(),{ s ->
                                spm.content.remove(it)
                                FavoriteDataManager.removeItem(s)
                            }, {s ->
                                preToUid = s
                            }, {
                                searchParameterModel.folderInfos.isNotEmpty() || path != "/"
                            })
                        }
                    }
                },
                ss = ss
            )
        }
    }
    MakeFolderAsk(showAskMaker, path, searchParameterModel) {
        showAskMaker = false
    }
    PrepareMoveItemAsk(preToUid, searchParameterModel) { preToUid = "" }
}