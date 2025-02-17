@file:OptIn(ExperimentalMaterial3Api::class)

package com.funny.translation.translate.ui.main

import android.util.Log
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement.spacedBy
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.ModalBottomSheetLayout
import androidx.compose.material.ModalBottomSheetValue
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.rememberModalBottomSheetState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.FontWeight.Companion.W600
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.items
import com.funny.cmaterialcolors.MaterialColors
import com.funny.data_saver.core.rememberDataSaverState
import com.funny.translation.AppConfig
import com.funny.translation.Consts
import com.funny.translation.helper.ClipBoardUtil
import com.funny.translation.helper.DataSaverUtils
import com.funny.translation.helper.toastOnUi
import com.funny.translation.translate.*
import com.funny.translation.translate.R
import com.funny.translation.translate.activity.WebViewActivity
import com.funny.translation.translate.database.TransHistoryBean
import com.funny.translation.translate.ui.bean.RoundCornerConfig
import com.funny.translation.translate.ui.widget.*
import com.funny.translation.translate.utils.AudioPlayer
import com.funny.translation.ui.touchToScale
import com.google.accompanist.flowlayout.FlowRow
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

private const val TAG = "MainScreen"

// 用于选择引擎时的回调
private interface UpdateSelectedEngine {
    fun add(engine: TranslationEngine)
    fun remove(engine: TranslationEngine)
}

enum class ShowListType {
    History, Result
}

/**
 * 项目的翻译页面, [图片](https://web.funnysaltyfish.fun/temp_img/202111102032441.jpg)
 */
@ExperimentalComposeUiApi
@ExperimentalMaterialApi
@ExperimentalAnimationApi
@Composable
fun MainScreen() {
    val vm: MainViewModel = viewModel()
    val context = LocalContext.current

    // Compose函数会被反复重新调用（重组），所以变量要remember
    val updateSelectedEngine = remember {
        object : UpdateSelectedEngine {
            override fun add(engine: TranslationEngine) {
                vm.addSelectedEngines(engine)
            }

            override fun remove(engine: TranslationEngine) {
                vm.removeSelectedEngine(engine)
            }
        }
    }

    // 内置引擎
    val bindEngines by vm.bindEnginesFlow.collectAsState(emptyList())
    // 插件
    val jsEngines by vm.jsEnginesFlow.collectAsState(emptyList())
    val scope = rememberCoroutineScope()
    // 使用 staticCompositionLocal 传递主页面 scaffold 的 snackbarHostState
    // 方便各个页面展示 snackBar
    // CompositionLocal 相关知识可参阅 https://developer.android.google.cn/jetpack/compose/compositionlocal?hl=zh-cn
    val snackbarHostState = LocalSnackbarState.current

    val showSnackbar: (String) -> Unit = remember {
        {
            scope.launch {
                snackbarHostState.showSnackbar(it, withDismissAction = true)
            }
        }
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    val activityVM: ActivityViewModel = LocalActivityVM.current
    val softwareKeyboardController = LocalSoftwareKeyboardController.current

    val updateShowListType: (type: ShowListType) -> Unit = remember {
        {
            vm.showListType = it
        }
    }

    // 使用 BoxWithConstraints 用于适配横竖屏
    BoxWithConstraints(Modifier.fillMaxSize()) {
        if (maxWidth > 720.dp) { // 横屏
            val scrollState = rememberScrollState()
            Row(Modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
                EngineSelect(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(0.3f)
                        .padding(start = 16.dp, top = 6.dp)
                        .verticalScroll(scrollState),
                    bindEngines, jsEngines,
                    updateSelectedEngine
                )
                Box(
                    modifier = Modifier
                        .fillMaxHeight(0.95f)
                        .width(0.16f.dp)
                        .background(MaterialTheme.colorScheme.primaryContainer)
                )
                Column(
                    Modifier
                        .fillMaxSize()
                        .padding(0.dp), horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Spacer(modifier = Modifier.height(8.dp))
                    InputPart(
                        modifier = Modifier.fillMaxWidth(),
                        vm = vm,
                        showSnackbar = showSnackbar,
                        expandEngineSelect = null,
                        updateShowListType = updateShowListType
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    ResultPart(vm = vm, showSnackbar = showSnackbar)
                }
            }
        } else {

            val modalBottomSheetState =
                rememberModalBottomSheetState(initialValue = ModalBottomSheetValue.Hidden)
            ModalBottomSheetLayout(
                sheetShape = RoundedCornerShape(
                    topStart = 12.dp,
                    topEnd = 12.dp
                ), sheetState = modalBottomSheetState, sheetContent = {
                    EngineSelect(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surface)
                            .padding(12.dp),
                        bindEngines,
                        jsEngines,
                        updateSelectedEngine
                    )
                }) {
                if (AppConfig.sTransPageInputBottom.value) { // 如果输入框在下面
                    SubcomposeBottomFirstLayout(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 12.dp, end = 12.dp, top = 12.dp, bottom = 12.dp)
                            .imePadding(),
                        other = {
                            ResultPart(vm = vm, showSnackbar = showSnackbar)
                        },
                        bottom = {
                            InputPart(
                                vm = vm,
                                showSnackbar = showSnackbar,
                                modifier = Modifier.fillMaxWidth(),
                                expandEngineSelect = { scope.launch { modalBottomSheetState.show() } },
                                updateShowListType = updateShowListType
                            )
                        }
                    )
                } else { // 输入框在上面
                    Column(
                        Modifier
                            .fillMaxSize()
                            .padding(start = 12.dp, end = 12.dp, top = 12.dp, bottom = 12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Spacer(modifier = Modifier.height(8.dp))
                        InputPart(
                            modifier = Modifier.fillMaxWidth(),
                            vm = vm,
                            showSnackbar = showSnackbar,
                            expandEngineSelect = { scope.launch { modalBottomSheetState.show() } },
                            updateShowListType = updateShowListType
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        ResultPart(vm = vm, showSnackbar = showSnackbar)
                    }
                }
            }
        }

        var singleLine by remember {
            mutableStateOf(true)
        }
        val notice by activityVM.noticeInfo
        notice?.let {
            NoticeBar(
                modifier = Modifier
                    .fillMaxSize()
                    .wrapContentHeight(Alignment.Bottom)
                    .clickable {
                        if (it.url.isNullOrEmpty()) singleLine = !singleLine
                        else WebViewActivity.start(context, it.url)
                    }
                    .background(
                        MaterialTheme.colorScheme.primaryContainer,
                        RoundedCornerShape(8.dp)
                    )
                    .padding(8.dp)
                    .animateContentSize(),
                text = it.message,
                singleLine = singleLine,
                showClose = true,
            )
        }
    }


    // DisposableEffect 是副作用的一种，相较于其他几个 SideEffect，特点在于可取消
    // 有关更多副作用，可参阅 https://developer.android.google.cn/jetpack/compose/side-effects?hl=zh-cn
    // 此处用于观察生命周期
    DisposableEffect(key1 = lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                val text = activityVM.tempTransConfig.sourceString?.trim() ?: ""
                if (text != "") {
                    vm.translateText = text
                    if (activityVM.tempTransConfig.sourceLanguage != null) {
                        vm.sourceLanguage = activityVM.tempTransConfig.sourceLanguage!!
                    }
                    if (activityVM.tempTransConfig.targetLanguage != null) {
                        vm.targetLanguage = activityVM.tempTransConfig.targetLanguage!!
                    }
                    vm.translate()
                    activityVM.tempTransConfig.clear()
                }
            } else if (event == Lifecycle.Event.ON_STOP) {
                softwareKeyboardController?.hide()
            }
        }

        // Add the observer to the lifecycle
        lifecycleOwner.lifecycle.addObserver(observer)

        // When the effect leaves the Composition, remove the observer
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }
}

@Composable
private fun ResultPart(vm: MainViewModel, showSnackbar: (String) -> Unit) {
    val showHistory by rememberDataSaverState(key = Consts.KEY_SHOW_HISTORY, default = false)
    if (showHistory && vm.showListType == ShowListType.History) {
        TransHistoryList(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            transHistories = vm.transHistories.collectAsLazyPagingItems(),
            onClickHistory = { transHistory ->
                vm.translateText = transHistory.sourceString
                vm.sourceLanguage = findLanguageById(transHistory.sourceLanguageId)
                vm.targetLanguage = findLanguageById(transHistory.targetLanguageId)
                vm.translate()
            },
            onDeleteHistory = { sourceString ->
                vm.deleteTransHistory(sourceString)
            }
        )
    } else {
        TranslationList(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            resultList = vm.resultList,
            showSnackbar = showSnackbar
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun TransHistoryList(
    modifier: Modifier,
    transHistories: LazyPagingItems<TransHistoryBean>,
    onClickHistory: (TransHistoryBean) -> Unit,
    onDeleteHistory: (String) -> Unit,
) {
    val context = LocalContext.current
    LazyColumn(
        modifier = modifier.background(
            MaterialTheme.colorScheme.primaryContainer,
            RoundedCornerShape(4.dp)
        ), reverseLayout = true // 这一条使得最新的历史会在最下面
    ) {
        items(transHistories) { transHistory ->
            transHistory ?: return@items
            Row(
                Modifier
                    .fillMaxWidth()
                    .touchToScale {
                        onClickHistory(transHistory)
                    }
                    .padding(start = 8.dp)
                    .animateItemPlacement(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically) {
                Text(
                    modifier = Modifier.weight(1f),
                    text = transHistory.sourceString,
                    fontWeight = W600,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    fontSize = 16.sp
                )
                Row {
                    IconButton(onClick = {
                        ClipBoardUtil.copy(
                            context,
                            transHistory.sourceString
                        )
                    }) {
                        Icon(
                            painterResource(id = R.drawable.ic_copy_content),
                            stringResource(R.string.copy)
                        )
                    }
                    IconButton(onClick = {
                        onDeleteHistory(transHistory.sourceString)
                    }) {
                        Icon(Icons.Default.Delete, "删除此历史记录")
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun InputPart(
    modifier: Modifier,
    vm: MainViewModel,
    showSnackbar: (String) -> Unit,
    expandEngineSelect: (() -> Unit)?,
    updateShowListType: (ShowListType) -> Unit,
) {
    val transText = vm.translateText
    val sourceLanguage = vm.sourceLanguage
    val targetLanguage = vm.targetLanguage
    val context = LocalContext.current

    val animateProgress = remember {
        Animatable(100f)
    }

    val enabledLanguages = remember {
        allLanguages.filter {
            DataSaverUtils.readData(it.selectedKey, true)
        }
    }

    LaunchedEffect(vm.progress) {
        animateProgress.animateTo(vm.progress)
    }

    var shouldRequestFocus by remember { mutableStateOf(AppConfig.sAutoFocus.value) }

    DisposableEffect(Unit){
        onDispose { shouldRequestFocus = false }
    }

    fun startTranslate() {
        val selectedEngines = vm.selectedEngines
        if (selectedEngines.isEmpty()) {
            showSnackbar(FunnyApplication.resources.getString(R.string.snack_no_engine_selected))
            return
        }
        val selectedSize = selectedEngines.size
        if (selectedSize > Consts.MAX_SELECT_ENGINES) {
            showSnackbar(
                FunnyApplication.resources.getString(R.string.message_out_of_max_engine_limit)
                    .format(Consts.MAX_SELECT_ENGINES, selectedSize)
            )
            return
        }
        if (!vm.isTranslating()) {
            vm.translate()
            updateShowListType(ShowListType.Result)
            shouldRequestFocus = false
        } else {
            vm.cancel()
            context.toastOnUi(FunnyApplication.resources.getString(R.string.message_stop_translate))
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.primaryContainer)
            .clip(RoundedCornerShape(4.dp))
            .padding(8.dp)
    ) {
        InputText(
            text = transText,
            updateText = {
                vm.translateText = it
                if (it == "") updateShowListType(ShowListType.History)
            },
            shouldRequest = shouldRequestFocus,
            updateFocusRequest = {
                if (it != shouldRequestFocus) shouldRequestFocus = it
            },
            modifier = Modifier
                .fillMaxWidth(),
            translateAction = ::startTranslate
        ) // 输入框
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Row(Modifier.horizontalScroll(rememberScrollState())) {
                LanguageSelect(
                    Modifier.semantics {
                        contentDescription = appCtx.getString(R.string.des_current_source_lang)
                    },
                    language = sourceLanguage,
                    languages = enabledLanguages,
                    updateLanguage = {
                        vm.sourceLanguage = it
                        DataSaverUtils.saveData(Consts.KEY_SOURCE_LANGUAGE, it.id)
                    }
                )
                ExchangeButton {
                    Log.d(TAG, "MainScreen: clicked")
                    val temp = sourceLanguage
                    vm.sourceLanguage = targetLanguage
                    vm.targetLanguage = temp

                    DataSaverUtils.saveData(Consts.KEY_SOURCE_LANGUAGE, vm.sourceLanguage.id)
                    DataSaverUtils.saveData(Consts.KEY_TARGET_LANGUAGE, vm.targetLanguage.id)
                }
                LanguageSelect(
                    Modifier.semantics {
                        contentDescription = appCtx.getString(R.string.des_current_target_lang)
                    },
                    language = targetLanguage,
                    languages = enabledLanguages,
                    updateLanguage = {
                        vm.targetLanguage = it
                        DataSaverUtils.saveData(Consts.KEY_TARGET_LANGUAGE, it.id)
                    }
                )
            }
            Row {
                expandEngineSelect?.let {
                    IconButton(onClick = it) {
                        Icon(
                            Icons.Default.ArrowDropDown,
                            contentDescription = "expand",
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }

                IconButton(
                    onClick = {
                        updateShowListType(ShowListType.History)
                        vm.translateText = ""
                        shouldRequestFocus = true
                        Log.d(TAG, "InputText: 手动展示软键盘")
                    }
                ) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "clear",
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
                TranslateButton(
                    animateProgress.value.roundToInt(),
                    vm.isTranslating(),
                    onClick = ::startTranslate
                )
            }
        }
    }
}

@ExperimentalAnimationApi
@Composable
private fun EngineSelect(
    modifier: Modifier,
    bindEngines: List<TranslationEngine> = arrayListOf(),
    jsEngines: List<TranslationEngine> = arrayListOf(),
    updateSelectEngine: UpdateSelectedEngine
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.Start,
    ) {
        Text(
            text = stringResource(id = R.string.bind_engine),
            fontWeight = W600
        )
        FlowRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            mainAxisSpacing = 8.dp,
            crossAxisSpacing = 0.dp
        ) {
            bindEngines.forEachIndexed { index, task ->
                var taskSelected by rememberDataSaverState(
                    key = task.selectKey,
                    default = task.selected
                )
                FilterChip(selected = taskSelected, onClick = {
                    if (!taskSelected) { // 选中了
                        updateSelectEngine.add(task)
                    } else updateSelectEngine.remove(task)
                    taskSelected = !taskSelected
                }, label = {
                    Text(text = task.name)
                })
            }
        }

        if (jsEngines.isNotEmpty()) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = stringResource(id = R.string.plugin_engine),
                fontWeight = W600
            )
            FlowRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                mainAxisSpacing = 8.dp,
                crossAxisSpacing = 0.dp
            ) {
                jsEngines.forEachIndexed { index, task ->
                    var taskSelected by rememberDataSaverState(
                        key = task.selectKey,
                        default = task.selected
                    )
                    FilterChip(selected = taskSelected, onClick = {
                        if (!taskSelected) { // 选中了
                            updateSelectEngine.add(task)
                        } else updateSelectEngine.remove(task)
                        taskSelected = !taskSelected
                    }, label = {
                        Text(text = task.name)
                    })
                }
            }
        }
    }
}

@Composable
private fun LanguageSelect(
    modifier: Modifier = Modifier,
    language: Language,
    languages: List<Language>,
    updateLanguage: (Language) -> Unit,
) {
    var expanded by remember {
        mutableStateOf(false)
    }
    TextButton(modifier = modifier, onClick = {
        expanded = true
    }) {
        Text(text = language.displayText)
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            languages.forEach {
                DropdownMenuItem(onClick = {
                    updateLanguage(it)
                    expanded = false
                }, text = {
                    Text(it.displayText)
                })
            }
        }
    }
}

@Composable
private fun TranslationList(
    modifier: Modifier,
    resultList: List<TranslationResult>,
    showSnackbar: (String) -> Unit,
) {
    val size = resultList.size
    LazyColumn(
        modifier = modifier,
        verticalArrangement = spacedBy(4.dp),
    ) {
        itemsIndexed(resultList, key = { _, r -> r.engineName }) { index, result ->
//            Log.d(TAG, "TranslationList: $result")
            TranslationItem(
                modifier = Modifier.fillMaxWidth(),
                result = result, roundCornerConfig = when (index) {
                    0 -> if (size == 1) RoundCornerConfig.All else RoundCornerConfig.Top
                    size - 1 -> RoundCornerConfig.Bottom
                    else -> RoundCornerConfig.None
                }, showSnackbar = showSnackbar
            )
        }
    }
}

@Composable
private fun TranslateButton(
    progress: Int = 100,
    isTranslating: Boolean = false,
    onClick: () -> Unit
) {
    val borderColor = MaterialTheme.colorScheme.secondary
    val density = LocalDensity.current
    val size48dp = remember { with(density) { 48.dp.toPx() } }
    val size12dp = remember { with(density) { 12.dp.toPx() } }

    IconButton(
        modifier =
        Modifier.drawBehind {
            if (progress < 100) drawArc(
                borderColor,
                startAngle = -90f,
                360f * progress / 100,
                false,
                style = Stroke(width = 4f),
                topLeft = Offset(size12dp / 2, size12dp / 2),
                size = size.copy(size48dp - size12dp, size48dp - size12dp)
            )
        }, onClick = onClick
    ) {
        if (!isTranslating) Icon(
            Icons.Default.Done,
            contentDescription = "开始翻译",
            tint = MaterialTheme.colorScheme.onPrimaryContainer
        )
        else Icon(
            painter = painterResource(id = R.drawable.ic_pause),
            contentDescription = "停止翻译",
            modifier = Modifier.size(24.dp),
            tint = MaterialTheme.colorScheme.onPrimaryContainer
        )
    }

}


@Composable
private fun TranslationItem(
    modifier: Modifier,
    result: TranslationResult,
    roundCornerConfig: RoundCornerConfig,
    showSnackbar: (String) -> Unit
) {
    val cornerSize = 16.dp
    val shape = when (roundCornerConfig) {
        is RoundCornerConfig.Top -> RoundedCornerShape(topStart = cornerSize, topEnd = cornerSize)
        is RoundCornerConfig.Bottom -> RoundedCornerShape(
            bottomEnd = cornerSize,
            bottomStart = cornerSize
        )
        is RoundCornerConfig.All -> RoundedCornerShape(cornerSize)
        is RoundCornerConfig.None -> RectangleShape
    }
    val offsetAnim = remember { Animatable(100f) }
    LaunchedEffect(Unit) {
        offsetAnim.animateTo(0f)
    }
    Box(
        modifier = modifier
            .touchToScale()
            .fillMaxWidth()
            .background(color = MaterialTheme.colorScheme.primaryContainer, shape = shape)
            .padding(12.dp)
            .offset { IntOffset(offsetAnim.value.roundToInt(), 0) }
            .animateContentSize()

    ) {
        var expandDetail by remember {
            mutableStateOf(false)
        }
        Column(
            horizontalAlignment = Alignment.Start
        ) {
            Text(text = result.engineName, color = MaterialColors.Grey600, fontSize = 12.sp)
            Spacer(modifier = Modifier.height(4.dp))
            val fontSize = when (result.basicResult.trans.length) {
                in 0..25 -> 24
                in 26..50 -> 20
                in 50..70 -> 18
                in 70..90 -> 16
                else -> 14
            }
            SelectionContainer {
                Text(
                    text = result.basicResult.trans,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    fontSize = fontSize.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.Start, modifier = Modifier.fillMaxWidth()) {
                IconButton(
                    onClick = {
                        ClipBoardUtil.copy(FunnyApplication.ctx, result.basicResult.trans)
                        showSnackbar(FunnyApplication.resources.getString(R.string.snack_finish_copy))
                    }, modifier = Modifier
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.secondary)
                ) {
                    Icon(
                        painterResource(id = R.drawable.ic_copy_content),
                        contentDescription = stringResource(id = R.string.copy_content),
                        tint = Color.White
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                val speakerState = rememberFrameAnimIconState(
                    listOf(R.drawable.ic_speaker_2, R.drawable.ic_speaker_1),
                )
                val text = result.basicResult.trans.trim()
                LaunchedEffect(AudioPlayer.currentPlayingText) {
                    // 修正：当列表划出屏幕后state与实际播放不匹配的情况
                    if (AudioPlayer.currentPlayingText != text && speakerState.isPlaying) {
                        speakerState.reset()
                    }
                }
                IconButton(
                    onClick = {
                        if (text == AudioPlayer.currentPlayingText) {
                            speakerState.reset()
                            AudioPlayer.pause()
                        } else {
                            speakerState.play()
                            AudioPlayer.play(
                                text,
                                result.targetLanguage!!,
                                onError = {
                                    showSnackbar(FunnyApplication.resources.getString(R.string.snack_speak_error))
                                },
                                onComplete = {
                                    speakerState.reset()
                                }
                            )
                        }
                    }, modifier = Modifier
//                        .then(Modifier.size(36.dp))
                        .clip(CircleShape)
                        .size(48.dp)
                        .background(MaterialTheme.colorScheme.secondary)
                ) {
                    FrameAnimationIcon(
                        state = speakerState,
                        contentDescription = stringResource(id = R.string.speak),
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
                if (!result.detailText.isNullOrEmpty()) {
                    Row(Modifier.weight(1f), horizontalArrangement = Arrangement.End) {
                        ExpandMoreButton(expand = expandDetail) {
                            expandDetail = it
                        }
                    }
                }
            }
            if (expandDetail) {
                MarkdownText(
                    markdown = result.detailText!!,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(4.dp),
                    selectable = false
                )
            }
        }
    }
}