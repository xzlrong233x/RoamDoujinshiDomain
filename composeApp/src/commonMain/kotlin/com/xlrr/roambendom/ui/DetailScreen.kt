package com.xlrr.roambendom.ui

import androidx.compose.animation.expandIn
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import com.xlrr.roambendom.LocalAnimatedVisibilityScope
import com.xlrr.roambendom.LocalSharedTransitionScope
import com.xlrr.roambendom.data.ArtworkInfo
import com.xlrr.roambendom.data.CSources
import com.xlrr.roambendom.data.CSources.NHENTAI
import com.xlrr.roambendom.data.CSources.PIXIV
import com.xlrr.roambendom.data.SearchItemData
import com.xlrr.roambendom.nav.Routes
import com.xlrr.roambendom.network.NHWebHelper
import com.xlrr.roambendom.utils.*
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.painterResource
import roambendom.composeapp.generated.resources.Res
import roambendom.composeapp.generated.resources.love_btn_icon
import roambendom.composeapp.generated.resources.loved_btn_icon

class DetailViewModel() : ViewModel() {
    var content: ArtworkInfo? by mutableStateOf(null)
    var loading by mutableStateOf(false)
    var error : Exception? by mutableStateOf(null)
    var source: CSources = NHENTAI
    var id: String = ""

    fun isSuccessful() : Boolean {
        return !loading && content != null && error == null
    }

    suspend fun reload(idd: String, sourceIn: CSources, forceLoad: Boolean = false) {
        error = null
        if (idd == id && sourceIn == source && !forceLoad) {
            return
        }
        loading = true
        id = idd
        source = sourceIn
        try {
            content = when (source) {
                NHENTAI -> NHWebHelper.artwork(id)
                PIXIV -> error("in developing")
            }
        } catch (e : Exception) {
            e.printStackTrace()
            error = e
        }
        loading = false
    }
}

@Composable
fun NHDetail(searchItemData: SearchItemData, details: DetailViewModel) {
    with(LocalSharedTransitionScope.current) {
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally) {
            DetailBox() {
                Row(Modifier.fillMaxWidth()) {
                    DefaultErrorHandleImage(
                        searchItemData.thumb,
                        Modifier.sharedBounds(
                            rememberSharedContentState(searchItemData.thumb),
                            LocalAnimatedVisibilityScope.current
                        ).width((LocalWindowSize.current.width.value * 0.382).dp.coerceIn(98.dp, 256.dp))
                    )
                    Spacer(Modifier.width(8.dp))
                    Column {
                        Text(searchItemData.title, style = typography.titleLarge,
                            maxLines = 3, overflow = TextOverflow.Ellipsis)
                        CtrlAnimatedVisibility(
                            details.isSuccessful(),
                            enter = fadeIn(),
                            exit = fadeOut(),
                            label = "smallTitle"
                        ) {
                            details.content?.let {
                                Column {
                                    if (it.altitle.isNotBlank()) Text(
                                        it.altitle,
                                        color = Color(0.5f, 0.5f, 0.5f),
                                        style = typography.titleSmall,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    if (it.page > 0) Text(
                                        "页数：${it.page}",
                                        style = typography.titleMedium
                                    )
                                    Text(
                                        "语言：${it.language.toString().lowercase()}",
                                        style = typography.titleMedium
                                    )
                                }
                            }
                        }
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Button({
                        if (details.content == null) {
                            return@Button
                        }
                        GlobalData.nav.push(Routes.Artwork(details.content!!))
                    // TODO: 实现Artwork。
                    }, shape = RoundedCornerShape(20), modifier = Modifier.width(128.dp).height(36.dp)) {
                        Text("阅读")
                    }
//                    IconButton({loved = !loved}, ) {
//                        Icon(
//                            painterResource(if (loved) Res.drawable.loved_btn_icon else Res.drawable.love_btn_icon),
//                            contentDescription = null
//                        )
//                    } 类似收藏的功能，还没想好怎么做。
                }
            }
            CtrlAnimatedVisibility(
                details.isSuccessful(),
                modifier = Modifier.fillMaxWidth(),
                enter = fadeIn() + expandIn(expandFrom = Alignment.TopCenter),
                exit = fadeOut(),
                label = "otherInfo"
            ) {
                Column(Modifier.fillMaxWidth()) {
                    MultiCardTagBox("标签", details.content?.tags.orEmpty())
                    MultiCardTagBox("作者", details.content?.authors.orEmpty())
                    MultiCardTagBox("团体", details.content?.groups.orEmpty())
                }
            }
        }
    }
}

@Composable
fun DetailScreen(searchItemData: SearchItemData, modifier: Modifier = Modifier, details: DetailViewModel = viewModel { DetailViewModel() }) {
    LaunchedEffect(Unit) {
        details.reload(searchItemData.id, searchItemData.source)
    }
    val co = rememberCoroutineScope()
    Scaffold(modifier.fillMaxSize()) { pd ->
        Surface(modifier.fillMaxSize().padding(pd)) {
            when (searchItemData.source) {
                NHENTAI -> NHDetail(searchItemData, details)
                PIXIV -> TODO("in developing")
            }
            if (details.loading) {
                CenterCircular()
            }
            if (details.error != null) {
                details.error?.let {
                    Box(Modifier.fillMaxSize(), Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("错误：${it.message}")
                            Button({
                                co.launch {
                                    details.reload(
                                        searchItemData.id,
                                        searchItemData.source,
                                        true
                                    )
                                }
                            }) {
                                Text("点我重载")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MultiCardTagBox(head: String,list: List<String>) {
    if (list.isNotEmpty()) {
        DetailBox {
            Text(head,
                style = typography.headlineMedium)
            Spacer(Modifier.height(12.dp))
            FlowRow(Modifier.fillMaxWidth()) {
                for (x in list) {
                    CardLabel(
                        x,
                        shape = RoundedCornerShape(15),
                        sufColor = Color(142, 142, 142, 255),
                        fontColor = Color.Black,
                        fontStyle = typography.bodyLarge,
                        modifier = Modifier.padding(4.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun DetailBox(color: Color = MaterialTheme.colorScheme.surfaceContainerLow, content: @Composable () -> Unit) {
    Surface(Modifier.padding(12.dp).fillMaxWidth(0.96f),
        color = color, shape = RoundedCornerShape(12.dp)) {
        Column(
            Modifier.fillMaxSize()
                .padding(8.dp)
        ) {
            content()
        }
    }
}