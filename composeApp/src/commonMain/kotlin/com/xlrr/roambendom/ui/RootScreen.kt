package com.xlrr.roambendom.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.xlrr.roambendom.LocalAnimatedVisibilityScope
import com.xlrr.roambendom.nav.Routes
import com.xlrr.roambendom.utils.CtrlAnimatedVisibility
import com.xlrr.roambendom.utils.GlobalData
import com.xlrr.roambendom.utils.LocalWindowSize
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import roambendom.composeapp.generated.resources.Res
import roambendom.composeapp.generated.resources.chevron_left_icon
import roambendom.composeapp.generated.resources.chevron_right_icon
import roambendom.composeapp.generated.resources.home_icon
import roambendom.composeapp.generated.resources.settings_icon

private data class NavItem(
    val label: String,
    val icon: DrawableResource,
    val selected: (Any) -> Boolean,
    val onClick: () -> Unit,
)

private val navItems: List<NavItem> = listOf(
    NavItem(
        "首页",
        Res.drawable.home_icon,
        { it is Routes.Root.Home },
        { GlobalData.nav.navigateTo(Routes.Root.Home)}
    ),
    NavItem(
        "设置",
        Res.drawable.settings_icon,
        { it is Routes.Root.Settings },
        { GlobalData.nav.navigateTo(Routes.Root.Settings)}
    )
)

private class TopAppBarOffsetState(
    val maxUpPx: Float,
    val minUpPx: Float
) {
    private val _toolbarOffset = mutableFloatStateOf(0f)
    var toolbarOffsetHeightPx: Float
        get() {
//            if (GlobalData.nav.backStack.last() is Routes.Root.Detail) {
//                return 0f
//            } 本来换成类储存是为了在某些情况下锁定顶边栏，但我现在还没想好在那些情况下锁定。
            return _toolbarOffset.floatValue
        }
        set(value) {
            _toolbarOffset.floatValue = value
        }
    val nestedScrollConnection = object : NestedScrollConnection {
        override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
            val delta = available.y
            val newOffset = toolbarOffsetHeightPx + delta
            toolbarOffsetHeightPx = newOffset.coerceIn(-maxUpPx, -minUpPx)
            return Offset.Zero
        }
    }
}

val SmallScreenDpLine = 480.dp
val MediumScreenDpLine = 720.dp

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun AdaptiveScaffold(content: @Composable (PaddingValues) -> Unit) {
    var lessThan3 by remember { mutableStateOf(false) }
    var nailOpen by remember { mutableStateOf(true) }
    var smallMode by remember { mutableStateOf(false) }
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val curScreen = GlobalData.nav.backStack.last()
    val scope = rememberCoroutineScope()

    val maxUpPx = with(LocalDensity.current) { 56.dp.roundToPx().toFloat() }
    val minUpPx = 0f
    val topBarState: TopAppBarOffsetState = remember { TopAppBarOffsetState(maxUpPx, minUpPx) }

    val toggleNav = {
        if (smallMode)
            scope.launch {
                drawerState.apply {
                    if (isClosed) open() else close()
                }
            }
    }

    if (LocalWindowSize.current.width < MediumScreenDpLine && !lessThan3) {
        Snapshot.withMutableSnapshot {
            lessThan3 = true
            if (nailOpen) {
                nailOpen = false
            }
        }
    }
    else if (LocalWindowSize.current.width >= MediumScreenDpLine && lessThan3) {
        Snapshot.withMutableSnapshot {
            lessThan3 = false
            if (!nailOpen) {
                nailOpen = true
            }
        }
    }
    if (LocalWindowSize.current.width < SmallScreenDpLine) {
        smallMode = true
    }
    else if (smallMode) {
        smallMode = false
        topBarState.toolbarOffsetHeightPx = 0f
    }
    LaunchedEffect(smallMode) {
        if (!smallMode && drawerState.isOpen) {
            drawerState.close()
        }
    }
    LaunchedEffect(GlobalData.nav.backStack.last()) {
        topBarState.toolbarOffsetHeightPx = 0f
    }
    ModalNavigationDrawer(
        modifier = Modifier.fillMaxSize(),
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                Column {
                    Spacer(Modifier.height(12.dp))
                    Text("导航", modifier = Modifier.padding(16.dp),
                        style = MaterialTheme.typography.titleLarge)
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                    navItems.forEach {
                        NavigationDrawerItem(
                            label = { Text(it.label) },
                            selected = it.selected(curScreen),
                            icon = { Icon(painterResource(it.icon), contentDescription = null) },
                            onClick = {
                                it.onClick()
                                toggleNav()
                            },
                        )
                    }
                }
            }
        },
        gesturesEnabled = smallMode
    ) {
        Scaffold(
            Modifier.fillMaxSize(),
            floatingActionButton = {
                CtrlAnimatedVisibility(
                    !nailOpen && !smallMode,
                    enter = fadeIn(),
                    exit = fadeOut(),
                    label = "FloatingRailOpenBtn"
                ) {
                    IconButton(
                        { nailOpen = true },
                        Modifier.border(2.dp, Color.Black, IconButtonDefaults.standardShape)
                    ) {
                        Icon(
                            painterResource(Res.drawable.chevron_right_icon),
                            null, Modifier
                        )
                    }
                }
            },
            floatingActionButtonPosition = FabPosition.Start,
            contentWindowInsets = WindowInsets(
                top = 0.dp,
                bottom = 0.dp
            )
        ) { x ->
            val h: Float by animateFloatAsState(
                if (smallMode) maxUpPx + topBarState.toolbarOffsetHeightPx else 0f
            )
            Box(Modifier.padding(x).nestedScroll(topBarState.nestedScrollConnection)) {
                Column {
                    Spacer(Modifier.background(Color(0,0,0,0))
                        .height(with(LocalDensity.current){h.toDp()}))
                    Row(Modifier.fillMaxSize()) {
                        CtrlAnimatedVisibility(
                            nailOpen && !smallMode,
                            Modifier.fillMaxHeight(),
                            enter = fadeIn() + expandIn(expandFrom = Alignment.CenterStart),
                            exit = fadeOut() + shrinkOut(shrinkTowards = Alignment.CenterStart),
                            label = "RailShow"
                        ) {
                            NavigationRail(
                                Modifier.fillMaxHeight().padding(x),
                            ) {
                                navItems.forEach {
                                    NavigationRailItem(
                                        selected = it.selected(curScreen),
                                        onClick = it.onClick,
                                        icon = {
                                            Icon(
                                                painterResource(it.icon),
                                                contentDescription = null
                                            )
                                        },
                                        label = { Text(it.label) }
                                    )
                                }

                                Box(Modifier.fillMaxHeight()) {
                                    IconButton(
                                        { nailOpen = false },
                                        Modifier.padding(0.dp, 0.dp, 0.dp, 12.dp)
                                            .border(2.dp, Color.Black, IconButtonDefaults.standardShape)
                                            .align(Alignment.BottomCenter)
                                    ) {
                                        Icon(
                                            painterResource(Res.drawable.chevron_left_icon),
                                            null
                                        )
                                    }
                                }
                            }
                        }
                        content(x)
                    }
                }

                Surface(Modifier.statusBarsPadding()
                    .height(with(LocalDensity.current){h.toDp()}).fillMaxWidth(),
                    color = MaterialTheme.colorScheme.secondaryContainer) {
                    Row(
                        Modifier.height(with(LocalDensity.current){h.toDp()})
                            .fillMaxWidth().offset {
                                IntOffset(0, topBarState.toolbarOffsetHeightPx.toInt())
                            },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (GlobalData.nav.backStack.size > 1) {
                            IconButton({ scope.launch { GlobalData.nav.defaultBack() } }) {
                                Text("返")
                            }
                        } else {
                            IconButton({ scope.launch { drawerState.open() } }) {
                                Text("☰")
                            }
                        }
                    }
                }
//                CtrlAnimatedVisibility(smallMode,
//                    Modifier.fillMaxWidth(),
//                    enter = fadeIn() + expandIn(expandFrom = Alignment.TopCenter),
//                    exit = fadeOut() + shrinkOut(shrinkTowards = Alignment.TopCenter),
//                    label = "TopBar"
//                ) {
//
//                }
            }
        }
    }
}

@Composable
fun RootScreen(modifier: Modifier = Modifier) {
    val last = GlobalData.nav.backStack.last()
    AdaptiveScaffold {
        AnimatedContent(last) {x ->
            CompositionLocalProvider(LocalAnimatedVisibilityScope provides this) {
                when (x) {
                    is Routes.Root.Home -> HomeScreen(modifier.padding(it))
                    is Routes.Root.Detail -> DetailScreen(x.searchItemData, modifier.padding(it))
                    is Routes.Root.Settings -> SettingScreen(Modifier.padding(it))
                }
            }
        }
    }
}

@Composable
@Preview
fun Test() {
    AdaptiveScaffold {
        Box(Modifier.fillMaxSize(), Alignment.Center) {
            Text("HHHHHHHH")
        }
    }
}