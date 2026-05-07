package com.example.brainracer.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.ui.Alignment
import androidx.compose.ui.zIndex
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.brainracer.data.storage.StorageConfig
import com.example.brainracer.domain.entities.QuizDifficulty
import com.example.brainracer.ui.theme.LocalBrainRacerExtendedColors
import com.example.brainracer.ui.viewmodels.DraftQuestion
import com.example.brainracer.ui.viewmodels.QuizCreatorViewModel
import com.example.brainracer.ui.viewmodels.QuizDraft
import com.example.brainracer.ui.viewmodels.quizTemplates
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
private fun difficultyColorsMap(): Map<QuizDifficulty, Color> {
    val ext = LocalBrainRacerExtendedColors.current
    return remember(ext) {
        mapOf(
            QuizDifficulty.EASY to ext.difficultyEasy,
            QuizDifficulty.MEDIUM to ext.difficultyMedium,
            QuizDifficulty.HARD to ext.difficultyHard,
            QuizDifficulty.EXPERT to ext.difficultyExpert,
        )
    }
}

private val diffLabels = mapOf(
    QuizDifficulty.EASY   to "Лёгкий",
    QuizDifficulty.MEDIUM to "Средний",
    QuizDifficulty.HARD   to "Сложный",
    QuizDifficulty.EXPERT to "Эксперт",
)

private val allCategories = listOf(
    "Кастомные", "География", "История", "Математика",
    "Фильмы и музыка", "Наука", "Спорт"
)

private val quizCreatorExpandIntSpec = tween<IntSize>(durationMillis = 200, easing = FastOutSlowInEasing)

// ══════════════════════════════════════════════════════════════════════════════
//  ГЛАВНЫЙ ЭКРАН КОНСТРУКТОРА
// ══════════════════════════════════════════════════════════════════════════════

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun QuizCreatorScreen(
    navController: NavController,
    editQuizIdArg: String? = null,
    vm: QuizCreatorViewModel = viewModel()
) {
    val uiState by vm.uiState.collectAsState()
    val context = LocalContext.current
    val scope   = rememberCoroutineScope()

    val pagerState  = rememberPagerState { 3 }
    val tabTitles   = listOf("Конструктор", "Черновики", "Шаблоны")
    val tabIcons    = listOf(Icons.Default.Build, Icons.Default.Folder, Icons.Default.AutoAwesome)
    val isEditMode = uiState.editingQuizId != null

    LaunchedEffect(editQuizIdArg) {
        if (!editQuizIdArg.isNullOrBlank()) {
            vm.loadQuizForEdit(editQuizIdArg)
        } else if (uiState.editingQuizId != null) {
            vm.newDraft()
        }
    }

    // Глобальное сообщение об ошибке
    val snackbarHost = remember { SnackbarHostState() }
    LaunchedEffect(uiState.error) {
        uiState.error?.let {
            snackbarHost.showSnackbar(it)
            vm.clearError()
        }
    }
    LaunchedEffect(uiState.publishSuccess) {
        if (uiState.publishSuccess) {
            snackbarHost.showSnackbar("Викторина опубликована!")
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost   = { SnackbarHost(snackbarHost) },
        topBar = {
            Surface(color = MaterialTheme.colorScheme.background) {
                Column {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .windowInsetsPadding(WindowInsets.statusBars)
                            .padding(horizontal = 8.dp, vertical = 5.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        IconButton(
                            onClick = { navController.popBackStack() },
                            modifier = Modifier.align(Alignment.CenterStart)
                        ) {
                            Icon(
                                painter = androidx.compose.ui.res.painterResource(id = com.example.brainracer.R.drawable.arrow_back_btn),
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Text(
                            if (isEditMode) "Редактирование" else "Конструктор",
                            fontWeight = FontWeight.Bold,
                            fontSize   = 18.sp,
                            color      = MaterialTheme.colorScheme.onSurface
                        )
                        if (pagerState.currentPage == 0) {
                            TextButton(
                                onClick  = { vm.saveDraft() },
                                enabled  = !uiState.isSaving,
                                modifier = Modifier.align(Alignment.CenterEnd)
                            ) {
                                if (uiState.isSaving)
                                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.primary)
                                else
                                    Text("Сохранить", color = MaterialTheme.colorScheme.primary, fontSize = 14.sp)
                            }
                        }
                    }

                    // Вкладки (компактные — чтобы помещались бейджи-счётчики)
                    TabRow(
                        selectedTabIndex = pagerState.currentPage,
                        containerColor   = MaterialTheme.colorScheme.background,
                        contentColor     = MaterialTheme.colorScheme.primary,
                        indicator        = { positions ->
                            if (pagerState.currentPage < positions.size) {
                                TabRowDefaults.Indicator(
                                    modifier = Modifier.tabIndicatorOffset(positions[pagerState.currentPage])
                                        .clip(RoundedCornerShape(topStart = 3.dp, topEnd = 3.dp)),
                                    color    = MaterialTheme.colorScheme.primary, height = 2.5.dp
                                )
                            }
                        },
                        divider = { HorizontalDivider(color = MaterialTheme.colorScheme.outline, thickness = 1.dp) }
                    ) {
                        tabTitles.forEachIndexed { i, title ->
                            Tab(
                                selected               = pagerState.currentPage == i,
                                onClick                = { scope.launch { pagerState.animateScrollToPage(i) } },
                                selectedContentColor   = MaterialTheme.colorScheme.primary,
                                unselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                            ) {
                                Row(
                                    verticalAlignment     = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    modifier = Modifier.padding(horizontal = 2.dp, vertical = 7.dp)
                                ) {
                                    Icon(tabIcons[i], null, modifier = Modifier.size(13.dp))
                                    Text(
                                        title,
                                        fontSize   = 11.5.sp,
                                        maxLines   = 1,
                                        overflow   = TextOverflow.Ellipsis,
                                        fontWeight = if (pagerState.currentPage == i) FontWeight.SemiBold else FontWeight.Normal
                                    )
                                    // Бейдж черновиков (компактный)
                                    if (i == 1 && uiState.drafts.isNotEmpty()) {
                                        Badge(
                                            containerColor = MaterialTheme.colorScheme.primary,
                                            contentColor = MaterialTheme.colorScheme.onPrimary,
                                            modifier = Modifier.defaultMinSize(minWidth = 16.dp, minHeight = 16.dp)
                                        ) {
                                            Text(
                                                "${uiState.drafts.size}",
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    ) { padding ->
        HorizontalPager(
            state    = pagerState,
            modifier = Modifier.fillMaxSize().padding(padding)
        ) { page ->
            when (page) {
                0 -> ConstructorTab(vm = vm, uiState = uiState)
                1 -> DraftsTab(
                    drafts    = uiState.drafts,
                    onLoad    = { draft ->
                        vm.loadDraft(draft)
                        scope.launch { pagerState.animateScrollToPage(0) }
                    },
                    onDelete  = { vm.deleteDraft(it) },
                    onNewDraft = {
                        vm.newDraft()
                        scope.launch { pagerState.animateScrollToPage(0) }
                    }
                )
                2 -> TemplatesTab(onApply = { template ->
                    vm.applyTemplate(template)
                    scope.launch { pagerState.animateScrollToPage(0) }
                })
            }
        }
    }
}

// ══════════════════════════════════════════════════════════════════════════════
//  ВКЛАДКА: КОНСТРУКТОР
// ══════════════════════════════════════════════════════════════════════════════

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ConstructorTab(
    vm: QuizCreatorViewModel,
    uiState: com.example.brainracer.ui.viewmodels.QuizCreatorUiState
) {
    val draft   = uiState.currentDraft
    val context = LocalContext.current
    val listState = rememberLazyListState()
    var listScrollLocked by remember { mutableStateOf(false) }

    val coverPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            vm.setCoverUri(it)
            vm.uploadCover(context)
        }
    }

    LazyColumn(
        state               = listState,
        userScrollEnabled   = !listScrollLocked,
        contentPadding      = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier            = Modifier.fillMaxSize()
    ) {
        // ── Обложка ──────────────────────────────────────────────────────
        item {
            SectionLabel("Обложка викторины")
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(148.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(16.dp))
                    .clickable { coverPicker.launch("image/*") },
                contentAlignment = Alignment.Center
            ) {
                val coverSrc: Any? = draft.coverUrl?.let { StorageConfig.resolvePublicUrlForCoil(it) }
                    ?: draft.coverUri
                if (coverSrc != null) {
                    AsyncImage(
                        model             = coverSrc,
                        contentDescription = "Обложка",
                        contentScale      = ContentScale.Crop,
                        modifier          = Modifier.fillMaxSize().clip(RoundedCornerShape(16.dp))
                    )
                    // Оверлей с кнопкой замены
                    Box(
                        modifier = Modifier.fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.35f))
                            .clip(RoundedCornerShape(16.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment     = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(Icons.Default.Edit, null, tint = Color.White, modifier = Modifier.size(16.dp))
                            Text("Изменить", color = Color.White, fontSize = 13.sp)
                        }
                    }
                } else {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        if (uiState.uploadingCover) {
                            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary, modifier = Modifier.size(32.dp))
                            Spacer(Modifier.height(8.dp))
                            Text("Загружаем…", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
                        } else {
                            Icon(Icons.Default.AddPhotoAlternate, null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(36.dp))
                            Spacer(Modifier.height(8.dp))
                            Text("Добавить обложку", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
                            Text("JPEG / PNG / GIF", color = MaterialTheme.colorScheme.onSurfaceVariant.copy(0.6f), fontSize = 11.sp)
                        }
                    }
                }
            }
        }

        // ── Название ─────────────────────────────────────────────────────
        item {
            SectionLabel("Название *")
            CreatorTextField(
                value         = draft.title,
                onValueChange = { vm.updateTitle(it) },
                placeholder   = "Например: «Столицы мира»",
                maxLength     = 80
            )
        }

        // ── Описание ─────────────────────────────────────────────────────
        item {
            SectionLabel("Описание")
            CreatorTextField(
                value         = draft.description,
                onValueChange = { vm.updateDescription(it) },
                placeholder   = "О чём эта викторина?",
                maxLines      = 3,
                maxLength     = 300
            )
        }

        // ── Категория + сложность в строку ────────────────────────────────
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Column(modifier = Modifier.weight(1f)) {
                    SectionLabel("Категория")
                    CategoryDropdown(
                        selected  = draft.categoryId,
                        onSelect  = { vm.updateCategory(it) }
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    SectionLabel("Сложность")
                    DifficultyDropdown(
                        selected  = draft.difficulty,
                        onSelect  = { vm.updateDifficulty(it) }
                    )
                }
            }
        }

        // ── Время по умолчанию ────────────────────────────────────────────
        item {
            SectionLabel("Время на ответ (сек) по умолчанию")
            TimeSlider(
                value    = draft.timePerQuestion,
                min      = 5,
                max      = 120,
                step     = 5,
                onChange = { vm.updateTimePerQuestion(it) }
            )
        }

        // ── Разделитель ───────────────────────────────────────────────────
        item {
            HorizontalDivider(color = MaterialTheme.colorScheme.outline)
            Spacer(Modifier.height(4.dp))
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically
            ) {
                Text(
                    "Вопросы (${draft.questions.size})",
                    fontWeight = FontWeight.Bold,
                    fontSize   = 15.sp,
                    color      = MaterialTheme.colorScheme.onSurface
                )
                TextButton(onClick = { vm.addQuestion() }) {
                    Icon(Icons.Default.Add, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Добавить", color = MaterialTheme.colorScheme.primary, fontSize = 13.sp)
                }
            }
        }

        // ── Список вопросов ───────────────────────────────────────────────
        itemsIndexed(
            items = draft.questions,
            key = { _, q -> q.id }
        ) { index, question ->
            key(question.id) {
                QuestionCard(
                    index        = index,
                    question     = question,
                    isUploading  = uiState.uploadingImageForQuestion == index,
                    vm           = vm,
                    canDelete    = draft.questions.size > 1,
                    questionCount = draft.questions.size,
                    onMoveQuestion = { from, to -> vm.moveQuestion(from, to) },
                    onDragStateChange = { listScrollLocked = it },
                    modifier = Modifier.animateItem(
                        placementSpec = tween(200, easing = FastOutSlowInEasing)
                    )
                )
            }
        }

        // ── Кнопка публикации ─────────────────────────────────────────────
        item {
            Spacer(Modifier.height(6.dp))
            Text(
                text = "Перед публикацией выполняется авто-модерация контента.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 12.sp,
                lineHeight = 16.sp,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 6.dp)
            )
            Button(
                onClick  = { vm.publish(onSuccess = {}) },
                enabled  = !uiState.isPublishing && draft.title.isNotBlank(),
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape    = RoundedCornerShape(14.dp),
                colors   = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    disabledContainerColor = MaterialTheme.colorScheme.outline
                )
            ) {
                if (uiState.isPublishing) {
                    CircularProgressIndicator(modifier = Modifier.size(22.dp), strokeWidth = 2.dp, color = Color.White)
                    Spacer(Modifier.width(12.dp))
                    Text(if (uiState.editingQuizId != null) "Сохраняем…" else "Публикуем…", fontSize = 15.sp)
                } else {
                    Icon(
                        if (uiState.editingQuizId != null) Icons.Default.Edit else Icons.Default.Publish,
                        null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        if (uiState.editingQuizId != null) "Сохранить изменения" else "Опубликовать",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 15.sp
                    )
                }
            }
            Spacer(Modifier.height(28.dp))
        }
    }
}

// ── Карточка вопроса ───────────────────────────────────────────────────────

@Composable
private fun QuestionCard(
    index: Int,
    question: DraftQuestion,
    isUploading: Boolean,
    vm: QuizCreatorViewModel,
    canDelete: Boolean,
    questionCount: Int,
    onMoveQuestion: (Int, Int) -> Unit,
    onDragStateChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var expanded by remember { mutableStateOf(index == 0) }

    val indexState = rememberUpdatedState(index)

    val imagePicker = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            val i = indexState.value
            vm.setQuestionImageUri(i, it)
            vm.uploadQuestionImage(context, i)
        }
    }

    val density = LocalDensity.current
    val pxPerSlot = remember(density) { with(density) { 84.dp.toPx() } }

    var offsetY by remember { mutableFloatStateOf(0f) }
    var isDragging by remember { mutableStateOf(false) }

    // Плавный «отскок» в исходное положение на отпускании, когда индекс не меняется.
    val animatedOffsetY by animateFloatAsState(
        targetValue = if (isDragging) offsetY else 0f,
        animationSpec = if (isDragging) snap() else tween(180, easing = FastOutSlowInEasing),
        label = "questionDragOffsetY"
    )
    val animatedScale by animateFloatAsState(
        targetValue = if (isDragging) 1.03f else 1f,
        animationSpec = tween(160, easing = FastOutSlowInEasing),
        label = "questionDragScale"
    )
    val animatedElevation by animateFloatAsState(
        targetValue = if (isDragging) 12f else 0f,
        animationSpec = tween(160, easing = FastOutSlowInEasing),
        label = "questionDragElevation"
    )

    val cardShape = RoundedCornerShape(16.dp)
    val canReorder = questionCount > 1

    Surface(
        shape  = cardShape,
        color  = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        modifier = modifier
            .fillMaxWidth()
            .zIndex(if (isDragging) 1f else 0f)
            .shadow(elevation = animatedElevation.dp, shape = cardShape, clip = false)
            .graphicsLayer {
                translationY = animatedOffsetY
                scaleX = animatedScale
                scaleY = animatedScale
            }
            // Перетаскивание ВСЕЙ карточкой по long-press. Долгое нажатие на любую её часть
            // переводит карточку в режим drag — обычные тапы (на иконки/строки) при этом
            // продолжают работать, потому что drag запускается только после задержки нажатия.
            .then(
                if (canReorder) Modifier.pointerInput(index, questionCount, pxPerSlot) {
                    detectDragGesturesAfterLongPress(
                        onDragStart = {
                            onDragStateChange(true)
                            isDragging = true
                            offsetY = 0f
                        },
                        onDrag = { change, dragAmount ->
                            change.consume()
                            offsetY += dragAmount.y
                        },
                        onDragEnd = {
                            val delta = (offsetY / pxPerSlot).roundToInt()
                            val target = (index + delta).coerceIn(0, questionCount - 1)
                            if (target != index) onMoveQuestion(index, target)
                            offsetY = 0f
                            isDragging = false
                            onDragStateChange(false)
                        },
                        onDragCancel = {
                            offsetY = 0f
                            isDragging = false
                            onDragStateChange(false)
                        }
                    )
                } else Modifier
            )
            .animateContentSize(animationSpec = quizCreatorExpandIntSpec)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // Заголовок карточки
            Row(
                modifier              = Modifier.fillMaxWidth(),
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment     = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f).clickable { expanded = !expanded }
                ) {
                    Box(
                        modifier = Modifier.size(28.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary.copy(0.18f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("${index + 1}", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                    Text(
                        question.text.ifBlank { "Вопрос ${index + 1}" },
                        color      = if (question.text.isBlank()) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                        fontSize   = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines   = 1,
                        overflow   = TextOverflow.Ellipsis,
                        modifier   = Modifier.weight(1f)
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (canReorder) {
                        Icon(
                            Icons.Default.DragHandle,
                            contentDescription = "Удерживайте карточку, чтобы перетащить",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = if (isDragging) 1f else 0.55f),
                            modifier = Modifier.padding(end = 2.dp).size(18.dp)
                        )
                    }
                    IconButton(onClick = { expanded = !expanded }, modifier = Modifier.size(28.dp)) {
                        Icon(
                            if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp)
                        )
                    }
                    if (canDelete) {
                        IconButton(onClick = { vm.removeQuestion(indexState.value) }, modifier = Modifier.size(28.dp)) {
                            Icon(Icons.Default.DeleteOutline, null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }

            // Раскрытое содержимое
            AnimatedVisibility(
                visible = expanded,
                enter = fadeIn(animationSpec = tween(180)) + expandVertically(animationSpec = quizCreatorExpandIntSpec),
                exit = fadeOut(animationSpec = tween(140)) + shrinkVertically(animationSpec = quizCreatorExpandIntSpec)
            ) {
                Column(
                    modifier            = Modifier.padding(top = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Текст вопроса
                    CreatorTextField(
                        value         = question.text,
                        onValueChange = { vm.updateQuestionText(index, it) },
                        placeholder   = "Текст вопроса",
                        maxLines      = 4,
                        maxLength     = 300
                    )

                    // Картинка к вопросу
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(if (question.imageUrl != null) 120.dp else 44.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.background)
                            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp))
                            .clickable { imagePicker.launch("image/*") },
                        contentAlignment = Alignment.Center
                    ) {
                        if (question.imageUrl != null) {
                            AsyncImage(
                                model             = StorageConfig.resolvePublicUrlForCoil(question.imageUrl),
                                contentDescription = null,
                                contentScale      = ContentScale.Crop,
                                modifier          = Modifier.fillMaxSize().clip(RoundedCornerShape(12.dp))
                            )
                            Box(
                                Modifier.fillMaxSize().background(Color.Black.copy(0.3f))
                                    .clip(RoundedCornerShape(12.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Edit, null, tint = Color.White, modifier = Modifier.size(20.dp))
                            }
                        } else if (isUploading) {
                            CircularProgressIndicator(modifier = Modifier.size(22.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.primary)
                        } else {
                            Row(
                                verticalAlignment     = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(Icons.Default.Image, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
                                Text("Добавить фото / GIF (необязательно)", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                            }
                        }
                    }

                    // Варианты ответов
                    Text("Варианты ответов", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                    question.options.forEachIndexed { optIndex, opt ->
                        val isCorrect = question.correctIndex == optIndex
                        Row(
                            verticalAlignment     = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Радиокнопка «правильный»
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(CircleShape)
                                    .background(if (isCorrect) LocalBrainRacerExtendedColors.current.detailGreen.copy(0.2f) else MaterialTheme.colorScheme.surface)
                                    .border(
                                        1.5.dp,
                                        if (isCorrect) LocalBrainRacerExtendedColors.current.detailGreen else MaterialTheme.colorScheme.outline,
                                        CircleShape
                                    )
                                    .clickable { vm.setCorrectAnswer(index, optIndex) },
                                contentAlignment = Alignment.Center
                            ) {
                                if (isCorrect)
                                    Icon(Icons.Default.Check, null,
                                        tint = LocalBrainRacerExtendedColors.current.detailGreen, modifier = Modifier.size(14.dp))
                            }

                            OutlinedTextField(
                                value         = opt,
                                onValueChange = { vm.updateOption(index, optIndex, it) },
                                modifier      = Modifier.weight(1f),
                                placeholder   = { Text("Вариант ${optIndex + 1}", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp) },
                                singleLine    = true,
                                shape         = RoundedCornerShape(10.dp),
                                colors        = creatorFieldColors(),
                                textStyle     = LocalTextStyle.current.copy(fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface)
                            )

                            // Удалить вариант
                            if (question.options.size > 2) {
                                IconButton(
                                    onClick  = { vm.removeOptionFromQuestion(index, optIndex) },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(Icons.Default.Close, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(14.dp))
                                }
                            }
                        }
                    }

                    // Добавить вариант
                    if (question.options.size < 6) {
                        TextButton(
                            onClick  = { vm.addOptionToQuestion(index) },
                            modifier = Modifier.align(Alignment.Start)
                        ) {
                            Icon(Icons.Default.Add, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Добавить вариант", color = MaterialTheme.colorScheme.primary, fontSize = 12.sp)
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(0.5f))

                    // ── Подсказка / объяснение ────────────────────────────
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            Icons.Default.Lightbulb,
                            null,
                            tint = LocalBrainRacerExtendedColors.current.statusOrange,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            "Подсказка (показывается в результатах)",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    CreatorTextField(
                        value         = question.explanation,
                        onValueChange = { vm.updateQuestionExplanation(index, it) },
                        placeholder   = "Например: «Столица Франции — Париж, основан в III веке до н.э.»",
                        maxLines      = 4,
                        maxLength     = 300
                    )

                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(0.5f))

                    // Баллы + время в строку
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Баллы", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                            Spacer(Modifier.height(4.dp))
                            PointsSelector(
                                value    = question.points,
                                onChange = { vm.updatePoints(index, it) }
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Время (сек)", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                            Spacer(Modifier.height(4.dp))
                            TimeSlider(
                                value    = question.timeLimit,
                                min      = 5,
                                max      = 120,
                                step     = 5,
                                onChange = { vm.updateTimeLimit(index, it) }
                            )
                        }
                    }
                }
            }
        }
    }
}

// ══════════════════════════════════════════════════════════════════════════════
//  ВКЛАДКА: ЧЕРНОВИКИ
// ══════════════════════════════════════════════════════════════════════════════

@Composable
private fun DraftsTab(
    drafts: List<QuizDraft>,
    onLoad: (QuizDraft) -> Unit,
    onDelete: (String) -> Unit,
    onNewDraft: () -> Unit
) {
    if (drafts.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.padding(32.dp)
            ) {
                Box(
                    Modifier.size(72.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surface),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Folder, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(32.dp))
                }
                Text("Черновиков пока нет", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.SemiBold, fontSize = 17.sp)
                Text("Сохраните незаконченную викторину,\nчтобы вернуться к ней позже",
                    color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp, textAlign = TextAlign.Center)
                Spacer(Modifier.height(8.dp))
                Button(
                    onClick = onNewDraft,
                    colors  = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    shape   = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Add, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Создать викторину")
                }
            }
        }
        return
    }

    LazyColumn(
        contentPadding      = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier            = Modifier.fillMaxSize()
    ) {
        item {
            Text(
                "Черновики (${drafts.size})",
                color      = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize   = 12.sp,
                modifier   = Modifier.padding(bottom = 2.dp)
            )
        }
        items(drafts, key = { it.id }) { draft ->
            DraftCard(draft = draft, onLoad = { onLoad(draft) }, onDelete = { onDelete(draft.id) })
        }
    }
}

@Composable
private fun DraftCard(
    draft: QuizDraft,
    onLoad: () -> Unit,
    onDelete: () -> Unit
) {
    val fmt = remember { SimpleDateFormat("d MMM, HH:mm", Locale("ru")) }
    val dateStr = remember(draft.updatedAt) { fmt.format(Date(draft.updatedAt)) }

    Surface(
        shape    = RoundedCornerShape(12.dp),
        color    = MaterialTheme.colorScheme.surface,
        border   = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        modifier = Modifier.fillMaxWidth().clickable { onLoad() }
    ) {
        Row(
            modifier          = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                Modifier.size(36.dp).clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.primary.copy(0.18f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.EditNote, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
            }
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    draft.title.ifBlank { "Без названия" },
                    color      = if (draft.title.isBlank()) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.SemiBold,
                    fontSize   = 13.sp,
                    maxLines   = 1,
                    overflow   = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(2.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment     = Alignment.CenterVertically
                ) {
                    Text("${draft.questions.size} вопр.", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
                    Text("·", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
                    Text(
                        draft.categoryId,
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 11.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    Text("·", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
                    Text(dateStr, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp, maxLines = 1)
                }
            }
            Spacer(Modifier.width(4.dp))
            IconButton(onClick = onDelete, modifier = Modifier.size(28.dp)) {
                Icon(Icons.Default.DeleteOutline, null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
            }
        }
    }
}

// ══════════════════════════════════════════════════════════════════════════════
//  ВКЛАДКА: ШАБЛОНЫ
// ══════════════════════════════════════════════════════════════════════════════

@Composable
private fun TemplatesTab(onApply: (com.example.brainracer.ui.viewmodels.QuizTemplate) -> Unit) {
    LazyColumn(
        contentPadding      = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier            = Modifier.fillMaxSize()
    ) {
        item {
            Text(
                "Начните с готового шаблона — заполните только вопросы",
                color    = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 13.sp,
                modifier = Modifier.padding(bottom = 4.dp)
            )
        }
        itemsIndexed(quizTemplates) { i, template ->
            TemplateCard(template = template, colorIndex = i, onApply = { onApply(template) })
        }
    }
}

@Composable
private fun TemplateCard(
    template: com.example.brainracer.ui.viewmodels.QuizTemplate,
    colorIndex: Int,
    onApply: () -> Unit
) {
    val templateGradients = LocalBrainRacerExtendedColors.current.cardGradients
    val diffColors = difficultyColorsMap()
    val gradient = templateGradients[colorIndex % templateGradients.size]
    val diffColor = diffColors[template.difficulty] ?: MaterialTheme.colorScheme.onSurfaceVariant
    val diffLabel = diffLabels[template.difficulty] ?: ""

    Surface(
        shape  = RoundedCornerShape(16.dp),
        color  = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Шапка: эмодзи + заголовок (полная ширина для длинных названий)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Brush.linearGradient(gradient)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(template.emoji, fontSize = 24.sp)
                }
                Text(
                    text = template.title,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    lineHeight = 19.sp,
                    modifier = Modifier.weight(1f)
                )
            }

            // Описание: переносится по высоте, не обрезается двумя строками
            if (template.description.isNotBlank()) {
                Text(
                    text = template.description,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp,
                    lineHeight = 17.sp
                )
            }

            // Метаданные шаблона (chips заворачиваются на следующую строку при переполнении)
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement   = Arrangement.spacedBy(6.dp)
            ) {
                InfoChip("${template.questionCount} вопр.", MaterialTheme.colorScheme.onSurfaceVariant)
                InfoChip("${template.timePerQuestion}с", MaterialTheme.colorScheme.onSurfaceVariant)
                InfoChip(diffLabel, diffColor)
                InfoChip("${template.defaultPoints}★", LocalBrainRacerExtendedColors.current.statusOrange)
            }

            // Кнопка применения шаблона на полной ширине — текст всегда виден целиком.
            Button(
                onClick = onApply,
                modifier = Modifier.fillMaxWidth().height(40.dp),
                colors  = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                shape   = RoundedCornerShape(12.dp),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
            ) {
                Icon(Icons.Default.AutoAwesome, null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text("Выбрать шаблон", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

// ══════════════════════════════════════════════════════════════════════════════
//  ВСПОМОГАТЕЛЬНЫЕ КОМПОНЕНТЫ
// ══════════════════════════════════════════════════════════════════════════════

@Composable
private fun SectionLabel(text: String) {
    Text(
        text,
        color      = MaterialTheme.colorScheme.onSurfaceVariant,
        fontSize   = 12.sp,
        fontWeight = FontWeight.Medium,
        modifier   = Modifier.padding(bottom = 4.dp)
    )
}

@Composable
private fun CreatorTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    maxLines: Int    = 1,
    maxLength: Int   = 200
) {
    OutlinedTextField(
        value         = value,
        onValueChange = { if (it.length <= maxLength) onValueChange(it) },
        modifier      = Modifier.fillMaxWidth(),
        placeholder   = { Text(placeholder, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp) },
        maxLines      = maxLines,
        shape         = RoundedCornerShape(12.dp),
        colors        = creatorFieldColors(),
        textStyle     = LocalTextStyle.current.copy(fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface),
        suffix        = if (maxLines > 1) {
            { Text("${value.length}/$maxLength", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp) }
        } else null,
        keyboardOptions = KeyboardOptions(imeAction = if (maxLines == 1) ImeAction.Next else ImeAction.Default)
    )
}

@Composable
private fun creatorFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor      = MaterialTheme.colorScheme.primary,
    unfocusedBorderColor    = MaterialTheme.colorScheme.outline,
    focusedContainerColor   = MaterialTheme.colorScheme.surface,
    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
    focusedTextColor        = MaterialTheme.colorScheme.onSurface,
    unfocusedTextColor      = MaterialTheme.colorScheme.onSurface,
    cursorColor             = MaterialTheme.colorScheme.primary
)

@Composable
private fun CategoryDropdown(selected: String, onSelect: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        OutlinedButton(
            onClick = { expanded = true },
            shape   = RoundedCornerShape(12.dp),
            border  = BorderStroke(1.dp, if (expanded) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline),
            colors  = ButtonDefaults.outlinedButtonColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 14.dp)
        ) {
            Text(selected, color = MaterialTheme.colorScheme.onSurface, fontSize = 13.sp, modifier = Modifier.weight(1f),
                overflow = TextOverflow.Ellipsis, maxLines = 1)
            Icon(Icons.Default.ArrowDropDown, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
        }
        DropdownMenu(
            expanded         = expanded,
            onDismissRequest = { expanded = false },
            modifier         = Modifier.background(MaterialTheme.colorScheme.surface)
        ) {
            allCategories.forEach { cat ->
                DropdownMenuItem(
                    text    = { Text(cat, color = if (cat == selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface, fontSize = 14.sp) },
                    onClick = { onSelect(cat); expanded = false }
                )
            }
        }
    }
}

@Composable
private fun DifficultyDropdown(selected: QuizDifficulty, onSelect: (QuizDifficulty) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val diffColors = difficultyColorsMap()
    val color = diffColors[selected] ?: MaterialTheme.colorScheme.onSurfaceVariant
    Box {
        OutlinedButton(
            onClick = { expanded = true },
            shape   = RoundedCornerShape(12.dp),
            border  = BorderStroke(1.dp, if (expanded) color else MaterialTheme.colorScheme.outline),
            colors  = ButtonDefaults.outlinedButtonColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 14.dp)
        ) {
            Box(Modifier.size(8.dp).clip(CircleShape).background(color))
            Spacer(Modifier.width(6.dp))
            Text(diffLabels[selected] ?: "", color = color, fontSize = 13.sp, modifier = Modifier.weight(1f))
            Icon(Icons.Default.ArrowDropDown, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
        }
        DropdownMenu(
            expanded         = expanded,
            onDismissRequest = { expanded = false },
            modifier         = Modifier.background(MaterialTheme.colorScheme.surface)
        ) {
            QuizDifficulty.entries.forEach { diff ->
                val dc = diffColors[diff] ?: MaterialTheme.colorScheme.onSurfaceVariant
                DropdownMenuItem(
                    leadingIcon = {
                        Box(Modifier.size(8.dp).clip(CircleShape).background(dc))
                    },
                    text    = { Text(diffLabels[diff] ?: "", color = dc, fontSize = 14.sp) },
                    onClick = { onSelect(diff); expanded = false }
                )
            }
        }
    }
}

@Composable
private fun TimeSlider(value: Int, min: Int, max: Int, step: Int, onChange: (Int) -> Unit) {
    Column {
        Row(
            modifier              = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("$min с", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
            Text(
                "$value с",
                color      = MaterialTheme.colorScheme.primary,
                fontSize   = 13.sp,
                fontWeight = FontWeight.Bold
            )
            Text("$max с", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
        }
        Slider(
            value         = value.toFloat(),
            onValueChange = { onChange(((it / step).toInt() * step).coerceIn(min, max)) },
            valueRange    = min.toFloat()..max.toFloat(),
            steps         = (max - min) / step - 1,
            colors        = SliderDefaults.colors(
                thumbColor        = MaterialTheme.colorScheme.primary,
                activeTrackColor  = MaterialTheme.colorScheme.primary,
                inactiveTrackColor = MaterialTheme.colorScheme.outline
            )
        )
    }
}

@Composable
private fun PointsSelector(value: Int, onChange: (Int) -> Unit) {
    val options = listOf(5, 10, 15, 20, 25, 30)
    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        items(options) { pts ->
            val isSelected = pts == value
            Surface(
                shape    = RoundedCornerShape(10.dp),
                color    = if (isSelected) MaterialTheme.colorScheme.primary.copy(0.2f) else MaterialTheme.colorScheme.surface,
                border   = BorderStroke(1.dp, if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline),
                modifier = Modifier.clickable { onChange(pts) }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    Text("$pts", color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    Text("★", color = LocalBrainRacerExtendedColors.current.statusOrange, fontSize = 11.sp)
                }
            }
        }
    }
}

@Composable
private fun InfoChip(text: String, color: Color) {
    Surface(shape = RoundedCornerShape(6.dp), color = color.copy(0.12f)) {
        Text(
            text,
            modifier     = Modifier.padding(horizontal = 7.dp, vertical = 4.dp),
            fontSize     = 11.sp,
            color        = color,
            fontWeight   = FontWeight.Medium,
            maxLines     = 1,
            softWrap     = false,
            lineHeight   = 13.sp
        )
    }
}