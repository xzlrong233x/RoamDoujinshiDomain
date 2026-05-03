package com.xlrr.roambendom.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.clearText
import androidx.compose.material3.*
import androidx.compose.material3.SearchBarDefaults.InputFieldHeight
import androidx.compose.material3.SearchBarDefaults.inputFieldColors
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.takeOrElse
import androidx.compose.ui.input.key.*
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import com.xlrr.roambendom.LocalAnimatedVisibilityScope
import com.xlrr.roambendom.data.CSources
import com.xlrr.roambendom.data.SearchItemData
import com.xlrr.roambendom.data.SearchResult
import com.xlrr.roambendom.data.SuggestionItem
import com.xlrr.roambendom.data.pixiv.KeywordSuggestionItem
import com.xlrr.roambendom.model.detail.asDetail
import com.xlrr.roambendom.model.search.SearchParameterModel
import com.xlrr.roambendom.nav.Routes
import com.xlrr.roambendom.nav.shouldShowTrailingIcon
import com.xlrr.roambendom.network.PIXIVApiHelper
import com.xlrr.roambendom.ui.detailScreen.DetailScreen
import com.xlrr.roambendom.utils.CtrlAnimatedVisibility
import com.xlrr.roambendom.utils.GlobalData
import com.xlrr.roambendom.utils.LocalWindowSize
import com.xlrr.roambendom.utils.MthUtil
import io.ktor.util.reflect.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import roambendom.composeapp.generated.resources.*
import kotlin.math.abs
import kotlin.reflect.KClass

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
        { GlobalData.nav.replace(Routes.Root.Home)}
    ),
    NavItem(
        "历史",
        Res.drawable.history_icon,
        { it is Routes.Root.History },
        { GlobalData.nav.replace(Routes.Root.History(
            SearchParameterModel("").config {
                searchFunction = {k, p, e ->
                    val l = GlobalData.historyData.search(k)
                    SearchResult(
                        l.size,
                        MthUtil.calWindow(p, 30, l.size)
                            .let { l.subList(it.first, it.second) },
                        k,
                        p
                    )
                }
            }
        ) { true })}
    ),
    NavItem(
        "设置",
        Res.drawable.settings_icon,
        { it is Routes.Root.Settings },
        { GlobalData.nav.push(Routes.Root.Settings)}
    )
)

private class TopAppBarOffsetState(
    val maxUpPx: Float,
    val minUpPx: Float,
    val coroutineScope: CoroutineScope
) {
    private val _toolbarOffset = mutableFloatStateOf(0f)
    var lockBar: Boolean = false
    private var _job: Job? = null
        set(value) {
            field?.cancel()
            field = value
        }
    var toolbarOffsetHeightPx: Float
        get() {
            if (lockBar) {
                return 0f
            }
            return _toolbarOffset.floatValue
        }
        set(value) {
            _toolbarOffset.floatValue = value
        }

    fun startReturn() {
        val b = if (abs(_toolbarOffset.floatValue) < maxUpPx / 2) 1 else -1
        val d = maxUpPx - minUpPx
        _job = coroutineScope.launch {
            while (true) {
                val newV = _toolbarOffset.floatValue + d / (ANIMATION_DURATION / ANIMATION_DELAY) * b
                toolbarOffsetHeightPx = newV.coerceIn(-maxUpPx,-minUpPx)
                if (abs(newV) !in minUpPx..maxUpPx) {
                    break
                }
                delay(ANIMATION_DELAY)
            }
        } // 我不认为有那么好
    }
    val nestedScrollConnection = object : NestedScrollConnection {
        private var fling = false
        override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
            _job?.cancel()
            if (!WhatShouldFixBar.any { GlobalData.nav.backStack.last().instanceOf(it) } || lockBar) {
                val delta = available.y
                val newOffset = toolbarOffsetHeightPx + delta
                toolbarOffsetHeightPx = newOffset.coerceIn(-maxUpPx, -minUpPx)
            }
            return Offset.Zero
        }

        override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
            if ((source == NestedScrollSource.SideEffect && !fling)
                || (available.y == 0f && consumed.y == 0f)) { // 桌面端在一些操作后会出现非用户操作的滑动事件
                startReturn()
            }
            return super.onPostScroll(consumed, available, source)
        }

        override suspend fun onPreFling(available: Velocity): Velocity {
            fling = true
            _job?.cancel()
            return super.onPreFling(available)
        }

        override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
            fling = false
            startReturn()
            return super.onPostFling(consumed, available)
        }
    }

    companion object {
        const val ANIMATION_DURATION = 280L
        const val ANIMATION_DELAY = 10L
    }
}

private class SearchSuggestionsService(
    val coroutineScope: CoroutineScope
) {
    val list: SnapshotStateList<SuggestionItem> = SnapshotStateList()
    private var job: Job? = null
        set(value) {
            field?.cancel()
            field = value
        }

    suspend fun suggest(q: String, curScreen: Any) {
        if ((GlobalData.homeContentSelection == HomeSelection.PIXIV
                    && curScreen !is Routes.Root.Search) || (curScreen is Routes.Root.Search
                    && curScreen.searchModel.configs.searchTarget.value == 1)) {
            PIXIVApiHelper.keywordSuggestion(q).also { list.clear() }.forEach {
                list.add(SuggestionItem(
                    it.tagName,
                    it.tagTranslation ?: ""
                ))
            }
        }
        else if ((GlobalData.homeContentSelection == HomeSelection.NH
                    && curScreen !is Routes.Root.Search) || (curScreen is Routes.Root.Search
                    && curScreen.searchModel.configs.searchTarget.value == 0)) {
            GlobalData.historyData.requestTokens(q).also { list.clear() }.forEach {
                list.add(SuggestionItem(it))
            }
        }
    }

    fun reload(text: String, curScreen: Any) {
        // TODO: 支持一些复杂的解析
        job = coroutineScope.launch {
            suggest(text, curScreen)
        }
    }

    fun clear() {
        list.clear()
    }
}

private val WhatShouldShowSearch: List<KClass<*>> = listOf(
    Routes.Root.Home::class,
    Routes.Root.Search::class,
    Routes.Root.History::class
)

private val WhatShouldFixBar: List<KClass<*>> = listOf(
    Routes.Root.History::class,
    Routes.Root.Settings::class
)

val SmallScreenDpLine = 480.dp
val MediumScreenDpLine = 720.dp
val RootBarHeight = 58.dp

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun RootSearchBar(
    modifier: Modifier, query: String, onQueryChange: (String) -> Unit,expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit, onSearch: (String) -> Unit, enabled: Boolean, freq: FocusRequester,
    placeholder: String,
    colors: TextFieldColors = inputFieldColors(),
    leadingIcon: @Composable (() -> Unit)? = null, trailingIcon: @Composable (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val fM = LocalFocusManager.current
    DockedSearchBar(
        inputField = {
            val interactionSource = remember { MutableInteractionSource() }

            val focused = interactionSource.collectIsFocusedAsState().value

            val textColor =
                LocalTextStyle.current.color.takeOrElse {
                    colors.textColor(enabled, isError = false, focused = focused)
                }

            BasicTextField(
                value = query,
                onValueChange = onQueryChange,
                modifier =
                    modifier
                        .sizeIn(
                            minWidth = 360.dp,
                            maxWidth = 720.dp,
                            minHeight = InputFieldHeight,
                        )
                        .focusRequester(freq)
                        .onFocusChanged { if (it.isFocused) onExpandedChange(true) else onExpandedChange(false) }
                        .onKeyEvent {
                            if (it.type == KeyEventType.KeyUp && it.key == Key.Escape) {
                                fM.clearFocus()
                            }
                            false
                        },
                enabled = enabled,
                singleLine = true,
                textStyle = LocalTextStyle.current.merge(TextStyle(color = textColor)),
                cursorBrush = SolidColor(colors.cursorColor(isError = false)),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { onSearch(query) }),
                interactionSource = interactionSource,
                decorationBox =
                    @Composable { innerTextField ->
                        TextFieldDefaults.DecorationBox(
                            value = query,
                            innerTextField = innerTextField,
                            enabled = enabled,
                            singleLine = true,
                            visualTransformation = VisualTransformation.None,
                            interactionSource = interactionSource,
                            placeholder = { Text(placeholder) },
                            leadingIcon =
                                leadingIcon?.let { leading ->
                                    { Box(Modifier.offset(x = 4.dp)) { leading() } }
                                },
                            trailingIcon =
                                trailingIcon?.let { trailing ->
                                    { Box(Modifier.offset(x = (-4).dp)) { trailing() } }
                                },
                            shape = CircleShape,
                            colors = colors,
                            contentPadding = TextFieldDefaults.contentPaddingWithoutLabel(),
                            container = {
                                val containerColor =
                                    animateColorAsState(
                                        targetValue =
                                            colors.containerColor(
                                                enabled = enabled,
                                                isError = false,
                                                focused = focused,
                                            ),
                                        animationSpec = MaterialTheme.motionScheme.fastEffectsSpec(),
                                    )
                                Box(
                                    Modifier.drawWithCache {
                                        val outline = CircleShape.createOutline(size, layoutDirection, this)
                                        onDrawBehind { drawOutline(outline, color = containerColor.value) }
                                    }
                                )
                            },
                        )
                    },
            )
        },
        expanded = expanded,
        onExpandedChange = onExpandedChange,
        modifier = modifier,
        content = content
    )
}

private fun screenSearch(it: String, ss: CoroutineScope, curScreen: Any, fM: FocusManager) {
    val cs = GlobalData.nav.backStack.last()
    val res = "([np])(\\d+)".toRegex().find(it)
    if (res != null && res.groupValues.size == 3) {
        val s = when(res.groupValues[1]) {
            "n" -> CSources.NHENTAI
            "p" -> CSources.PIXIV
            else -> null
        }
        if (s != null && res.groupValues.last().let { str -> str.isNotEmpty() && str.toIntOrNull() != null }) {
            GlobalData.nav.push(Routes.Root.Detail(
                SearchItemData(res.groupValues.last(), s).asDetail()
            ))
        }
    }
    else if (cs is Routes.Root.SearchLike) {
        cs.searchModel.key = it
        cs.searchModel.configs.applyChange()
        ss.launch {
            cs.searchModel.reload()
            GlobalData.forListState?.scrollBy(-Float.MAX_VALUE)
        }
        if (curScreen is Routes.Root.Search
            && curScreen.searchModel.configs.searchTarget.realValue == 0) {
            GlobalData.historyData.addSearchToken(it)
        }
    } else {
        if (GlobalData.homeContentSelection == HomeSelection.NH) {
            GlobalData.historyData.addSearchToken(it)
        }
        GlobalData.nav.push(
            Routes.Root.Search(
                SearchParameterModel(it).config {
                    if (GlobalData.homeContentSelection != null) {
                        val t = if (GlobalData.homeContentSelection == HomeSelection.NH) 0 else 1
                        searchTarget.setAll(t)
                    }
                }
            )
        )
    }
    fM.clearFocus()
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun RootHeadBar(smallMode: Boolean, h: Float, searchText: TextFieldState, topBarState: TopAppBarOffsetState,
                        should: Boolean, curScreen: Any,
                        drawerCaller: @Composable () -> Unit,
                        returnCaller: @Composable () -> Unit) {
    val ss = rememberCoroutineScope()
    val fM = LocalFocusManager.current
    var suggestions by remember { mutableStateOf(listOf<KeywordSuggestionItem>()) }
    var job: Job? by remember { mutableStateOf(null) }
    var exp by remember { mutableStateOf(false) }
    val freq = remember { FocusRequester() }
    var searchSetting by remember { mutableStateOf(false) }
    val suggestionsService = remember {
        SearchSuggestionsService(ss)
    }
    Surface(Modifier.statusBarsPadding()
        .fillMaxWidth().offset {
            IntOffset(0, topBarState.toolbarOffsetHeightPx.toInt())
        },) {
        Row(
            Modifier.height(with(LocalDensity.current){h.toDp()})
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (GlobalData.nav.backStack.size > 1) {
                returnCaller()
            } else if (!should) {
                drawerCaller()
            }
            CtrlAnimatedVisibility(
                !should && curScreen is Routes.Root && curScreen.headerTitle.isNotEmpty(),
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Text(if (curScreen is Routes.Root) curScreen.headerTitle else "")
            }
        }
    }
    LaunchedEffect(GlobalData.forListState?.isScrollInProgress) {
        GlobalData.forListState?.let {
            if (exp && GlobalData.forListState?.isScrollInProgress == true) {
                exp = false
                fM.clearFocus()
            }
        }
    }
    LaunchedEffect(GlobalData.nav.backStack.last()) {
        exp = false
    }

    if (should) {
        Box(Modifier.statusBarsPadding()
            .fillMaxWidth().offset {
                IntOffset(0, topBarState.toolbarOffsetHeightPx.toInt())
            }, Alignment.Center) {
            RootSearchBar(Modifier.widthIn(0.dp, 1200.dp), searchText.text.toString(),
                {
                    searchText.edit {
                        replace(0, length, it)
                    }
                    suggestionsService.reload(it, curScreen)
                },
                exp && suggestionsService.list.isNotEmpty(),
                {
                    exp = it
                    if (it) {
                        suggestionsService.reload(searchText.text.toString(), curScreen)
                    }
                },
                {
                    if (it.isNotEmpty()) screenSearch(it, ss, curScreen, fM)
                    else fM.clearFocus()
                },
                true,
                freq,
                "search...",
                 leadingIcon = if (smallMode) {
                    {
                        drawerCaller()
                    }
                } else null,
                trailingIcon = if (curScreen is Routes.Root.SearchLike && curScreen.shouldShowTrailingIcon()) {
                    {
                        Row {
                            if (curScreen.canChangeSettings) {
                                IconButton(
                                    {
                                        searchSetting = true
                                    }
                                ) {
                                    Icon(
                                        painterResource(Res.drawable.sim_setting),
                                        "search bar setting"
                                    )
                                }
                            }
                            if (curScreen.clearInput != null && curScreen.searchModel.key.isNotEmpty()) {
                                IconButton(
                                    {
                                        searchText.clearText()
                                        val n = curScreen.clearInput()
                                        if (n) screenSearch(searchText.text.toString(), ss, curScreen, fM)
                                    }
                                ) {
                                    Icon(
                                        painterResource(Res.drawable.close_icon),
                                        "clear search bar text button"
                                    )
                                }
                            }
                        }
                    }
                } else null
            ) {
                Column(Modifier.verticalScroll(rememberScrollState())) {
                    suggestionsService.list.forEach {
                        ListItem(
                            {
                                Text(it.key)
                            }, Modifier.clickable {
                                searchText.edit {
                                    replace(0, length, it.key)
                                }
                                exp = false
                                screenSearch(searchText.text.toString(), ss, curScreen, fM)
                                suggestionsService.clear()
                            }, supportingContent = if (it.extra.isNotEmpty()) {
                                {
                                    Text(it.extra)
                                }
                            } else null
                        )
                    }
                }
            }
        }
    }
    if (searchSetting && curScreen is Routes.Root.SearchLike) {
        SearchSettingDialog({
            searchSetting = false
        }, curScreen.searchModel)
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun AdaptiveScaffold(content: @Composable (PaddingValues) -> Unit) {
    var lessThan3 by remember { mutableStateOf(false) }
    var nailOpen by remember { mutableStateOf(true) }
    var smallMode by remember { mutableStateOf(false) }
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val curScreen = GlobalData.nav.backStack.last()
    val scope = rememberCoroutineScope()
    val searchText = remember(curScreen) {
        if (curScreen is Routes.Root.SearchLike) {
            TextFieldState(curScreen.searchModel.key)
        } else {
            GlobalData.rootSearchQuery
        }
    }

    val shouldShowSearch = WhatShouldShowSearch.any { curScreen.instanceOf(it) }

    val maxUpPx = with(LocalDensity.current) { RootBarHeight.roundToPx().toFloat() }
    val minUpPx = 0f
    //TODO：令其可以向下传递
    val topBarState: TopAppBarOffsetState = remember { TopAppBarOffsetState(maxUpPx, minUpPx, scope) }

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
            ModalDrawerSheet(if (smallMode) Modifier.fillMaxWidth(0.8f) else Modifier) {
                // 这里如果只填Modifier.fillMaxWidth(0.8f)的话，会在桌面端上出现最大化时弹出导航的问题
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
                    !nailOpen && !smallMode && !GlobalData.shouldHideRailBtn(),
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
            val h: Float by remember(shouldShowSearch, GlobalData.nav.backStack.size,
                smallMode, topBarState.toolbarOffsetHeightPx) {
                derivedStateOf {
                    if (!shouldShowSearch && GlobalData.nav.backStack.size == 1) {
                        if (smallMode) maxUpPx else 0f
                    } else {
                        maxUpPx + topBarState.toolbarOffsetHeightPx
                    }
                }
            }
            Box(Modifier.padding(x)) {
                Column(Modifier.nestedScroll(topBarState.nestedScrollConnection)) {
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

                RootHeadBar(smallMode, h, searchText, topBarState, shouldShowSearch, curScreen,
                    {
                        IconButton(
                            { scope.launch { drawerState.open() } },
                            Modifier.semantics { role = Role.Button }) {
                            Text("☰")
                        }
                    },
                    {
                        IconButton({ scope.launch { GlobalData.nav.defaultBack() } }) {
                            Text("返")
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun RootScreen(modifier: Modifier = Modifier) {
    val last = GlobalData.nav.backStack.last()
    AdaptiveScaffold {
        AnimatedContent(
            last,
            transitionSpec = {
                (fadeIn(animationSpec = tween(230)))
                    .togetherWith(fadeOut(animationSpec = tween(300)))
            }) {x ->
            CompositionLocalProvider(LocalAnimatedVisibilityScope provides this) {
                when (x) {
                    is Routes.Root.Home -> HomeScreen(modifier.padding(it))
                    is Routes.Root.Detail -> DetailScreen(x.detailModel, modifier.padding(it))
                    is Routes.Root.Settings -> SettingScreen(Modifier.padding(it))
                    is Routes.Root.Search -> SearchScreen(Modifier.padding(it), x.searchModel)
                    is Routes.Root.History -> HistoryScreen(Modifier.padding(it), x.searchModel)
                    is Routes.Root.FixedSearch -> SearchScreen(Modifier.padding(it), x.searchModel)
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