package com.xlrr.roambendom.ui

import androidx.compose.animation.expandIn
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkOut
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DrawerState
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.FabPosition
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
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
    )
)

@Composable
fun AdaptiveScaffold(content: @Composable (PaddingValues) -> Unit) {
    var lessThan3 by remember { mutableStateOf(false) }
    var nailOpen by remember { mutableStateOf(true) }
    var smallMode by remember { mutableStateOf(false) }
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val curScreen = GlobalData.nav.backStack.last()
    val scope = rememberCoroutineScope()
    val toggleNav = {
        if (smallMode)
            scope.launch {
                drawerState.apply {
                    if (isClosed) open() else close()
                }
            }
    }

    if (LocalWindowSize.current.width.value < 640f && !lessThan3) {
        Snapshot.withMutableSnapshot {
            lessThan3 = true
            if (nailOpen) {
                nailOpen = false
            }
        }
    }
    else if (LocalWindowSize.current.width.value > 640f && lessThan3) {
        Snapshot.withMutableSnapshot {
            lessThan3 = false
            if (!nailOpen) {
                nailOpen = true
            }
        }
    }
    else if (LocalWindowSize.current.width.value < 480) {
        smallMode = true
    }
    else if (smallMode) {
        smallMode = false
    }
    LaunchedEffect(smallMode) {
        if (!smallMode && drawerState.isOpen) {
            drawerState.close()
        }
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
        }
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
            topBar = {
                CtrlAnimatedVisibility(smallMode,
                    Modifier.fillMaxWidth(),
                    enter = fadeIn() + expandIn(expandFrom = Alignment.TopCenter),
                    exit = fadeOut() + shrinkOut(shrinkTowards = Alignment.TopCenter),
                    label = "TopBar"
                ) {
                    Surface {
                        Box(Modifier.fillMaxWidth()) {
                            IconButton({scope.launch { drawerState.open() }}) {
                                Text("☰")
                            }
                        }
                    }
                }
            }
        ) { x ->
            Row(Modifier.fillMaxSize().padding(x)) {
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
    }
}

@Composable
fun RootScreen(modifier: Modifier = Modifier) {
    val last = GlobalData.nav.backStack.last()
    AdaptiveScaffold {
        when (last) {
            is Routes.Root.Home -> HomeScreen(modifier)
        }
    }
}

@Composable
@Preview
fun Text() {
    AdaptiveScaffold {
        Box(Modifier.fillMaxSize(), Alignment.Center) {
            Text("HHHHHHHH")
        }
    }
}