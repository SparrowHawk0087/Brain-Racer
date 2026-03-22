package com.example.brainracer.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.brainracer.domain.entities.QuizDifficulty
import com.example.brainracer.ui.viewmodels.DraftQuestion
import com.example.brainracer.ui.viewmodels.QuizCreatorViewModel
import com.example.brainracer.ui.viewmodels.QuizDraft
import com.example.brainracer.ui.viewmodels.quizTemplates
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// ─── Палитра ──────────────────────────────────────────────────────────────
private val CBg       = Color(0xFF0F0F1A)
private val CCard     = Color(0xFF1A1A2E)
private val CBorder   = Color(0xFF2A2A3E)
private val CPurple   = Color(0xFF667EEA)
private val CGreen    = Color(0xFF3ECFA3)
private val CRed      = Color(0xFFEA5C7E)
private val CAmber    = Color(0xFFFFA726)
private val CTextPri  = Color(0xFFFFFFFF)
private val CTextSec  = Color(0xFF8B8AAE)

private val diffColors = mapOf(
    QuizDifficulty.EASY   to Color(0xFF3ECFA3),
    QuizDifficulty.MEDIUM to Color(0xFF4facfe),
    QuizDifficulty.HARD   to Color(0xFFf5576c),
    QuizDifficulty.EXPERT to Color(0xFFFFD700),
)
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

private val templateGradients = listOf(
    listOf(Color(0xFF667EEA), Color(0xFF764BA2)),
    listOf(Color(0xFF4facfe), Color(0xFF00f2fe)),
    listOf(Color(0xFF43e97b), Color(0xFF38f9d7)),
    listOf(Color(0xFFfa709a), Color(0xFFfee140)),
    listOf(Color(0xFFf093fb), Color(0xFFf5576c)),
    listOf(Color(0xFFa18cd1), Color(0xFFfbc2eb)),
)

// ══════════════════════════════════════════════════════════════════════════════
//  ГЛАВНЫЙ ЭКРАН КОНСТРУКТОРА
// ══════════════════════════════════════════════════════════════════════════════

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun QuizCreatorScreen(
    navController: NavController,
    vm: QuizCreatorViewModel = viewModel()
) {
    val uiState by vm.uiState.collectAsState()
    val context = LocalContext.current
    val scope   = rememberCoroutineScope()

    val pagerState  = rememberPagerState { 3 }
    val tabTitles   = listOf("Конструктор", "Черновики", "Шаблоны")
    val tabIcons    = listOf(Icons.Default.Build, Icons.Default.Folder, Icons.Default.AutoAwesome)

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
        containerColor = CBg,
        snackbarHost   = { SnackbarHost(snackbarHost) },
        topBar = {
            Surface(color = CBg) {
                Column {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .windowInsetsPadding(WindowInsets.statusBars)
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment     = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        IconButton(onClick = { navController.popBackStack() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = CTextPri)
                        }
                        Text(
                            "Конструктор",
                            fontWeight = FontWeight.Bold,
                            fontSize   = 18.sp,
                            color      = CTextPri
                        )
                        // Кнопка «Сохранить черновик» только на вкладке конструктора
                        if (pagerState.currentPage == 0) {
                            TextButton(
                                onClick  = { vm.saveDraft() },
                                enabled  = !uiState.isSaving
                            ) {
                                if (uiState.isSaving)
                                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = CPurple)
                                else
                                    Text("Сохранить", color = CPurple, fontSize = 14.sp)
                            }
                        } else {
                            Spacer(Modifier.width(72.dp))
                        }
                    }

                    // Вкладки
                    TabRow(
                        selectedTabIndex = pagerState.currentPage,
                        containerColor   = CBg,
                        contentColor     = CPurple,
                        indicator        = { positions ->
                            if (pagerState.currentPage < positions.size) {
                                TabRowDefaults.Indicator(
                                    modifier = Modifier.tabIndicatorOffset(positions[pagerState.currentPage])
                                        .clip(RoundedCornerShape(topStart = 3.dp, topEnd = 3.dp)),
                                    color    = CPurple, height = 3.dp
                                )
                            }
                        },
                        divider = { HorizontalDivider(color = CBorder, thickness = 1.dp) }
                    ) {
                        tabTitles.forEachIndexed { i, title ->
                            Tab(
                                selected               = pagerState.currentPage == i,
                                onClick                = { scope.launch { pagerState.animateScrollToPage(i) } },
                                selectedContentColor   = CPurple,
                                unselectedContentColor = CTextSec
                            ) {
                                Row(
                                    verticalAlignment     = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 12.dp)
                                ) {
                                    Icon(tabIcons[i], null, modifier = Modifier.size(15.dp))
                                    Text(
                                        title,
                                        fontSize   = 13.sp,
                                        fontWeight = if (pagerState.currentPage == i) FontWeight.SemiBold else FontWeight.Normal
                                    )
                                    // Бейдж черновиков
                                    if (i == 1 && uiState.drafts.isNotEmpty()) {
                                        Badge { Text("${uiState.drafts.size}") }
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

@Composable
private fun ConstructorTab(
    vm: QuizCreatorViewModel,
    uiState: com.example.brainracer.ui.viewmodels.QuizCreatorUiState
) {
    val draft   = uiState.currentDraft
    val context = LocalContext.current

    val coverPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            vm.setCoverUri(it)
            vm.uploadCover(context)
        }
    }

    LazyColumn(
        contentPadding      = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier            = Modifier.fillMaxSize()
    ) {
        // ── Обложка ──────────────────────────────────────────────────────
        item {
            SectionLabel("Обложка викторины")
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(CCard)
                    .border(1.dp, CBorder, RoundedCornerShape(18.dp))
                    .clickable { coverPicker.launch("image/*") },
                contentAlignment = Alignment.Center
            ) {
                val coverSrc = draft.coverUrl ?: draft.coverUri
                if (coverSrc != null) {
                    AsyncImage(
                        model             = coverSrc,
                        contentDescription = "Обложка",
                        contentScale      = ContentScale.Crop,
                        modifier          = Modifier.fillMaxSize().clip(RoundedCornerShape(18.dp))
                    )
                    // Оверлей с кнопкой замены
                    Box(
                        modifier = Modifier.fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.35f))
                            .clip(RoundedCornerShape(18.dp)),
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
                            CircularProgressIndicator(color = CPurple, modifier = Modifier.size(32.dp))
                            Spacer(Modifier.height(8.dp))
                            Text("Загружаем…", color = CTextSec, fontSize = 13.sp)
                        } else {
                            Icon(Icons.Default.AddPhotoAlternate, null,
                                tint = CTextSec, modifier = Modifier.size(36.dp))
                            Spacer(Modifier.height(8.dp))
                            Text("Добавить обложку", color = CTextSec, fontSize = 13.sp)
                            Text("JPEG / PNG / GIF", color = CTextSec.copy(0.6f), fontSize = 11.sp)
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
            HorizontalDivider(color = CBorder)
            Spacer(Modifier.height(4.dp))
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically
            ) {
                Text(
                    "Вопросы (${draft.questions.size})",
                    fontWeight = FontWeight.Bold,
                    fontSize   = 16.sp,
                    color      = CTextPri
                )
                TextButton(onClick = { vm.addQuestion() }) {
                    Icon(Icons.Default.Add, null, tint = CPurple, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Добавить", color = CPurple, fontSize = 13.sp)
                }
            }
        }

        // ── Список вопросов ───────────────────────────────────────────────
        itemsIndexed(draft.questions) { index, question ->
            QuestionCard(
                index      = index,
                question   = question,
                isUploading = uiState.uploadingImageForQuestion == index,
                vm         = vm,
                canDelete  = draft.questions.size > 1
            )
        }

        // ── Кнопка публикации ─────────────────────────────────────────────
        item {
            Spacer(Modifier.height(8.dp))
            Button(
                onClick  = { vm.publish(onSuccess = {}) },
                enabled  = !uiState.isPublishing && draft.title.isNotBlank(),
                modifier = Modifier.fillMaxWidth().height(54.dp),
                shape    = RoundedCornerShape(16.dp),
                colors   = ButtonDefaults.buttonColors(
                    containerColor = CPurple,
                    disabledContainerColor = CBorder
                )
            ) {
                if (uiState.isPublishing) {
                    CircularProgressIndicator(modifier = Modifier.size(22.dp), strokeWidth = 2.dp, color = Color.White)
                    Spacer(Modifier.width(12.dp))
                    Text("Публикуем…", fontSize = 15.sp)
                } else {
                    Icon(Icons.Default.Publish, null, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Опубликовать", fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                }
            }
            Spacer(Modifier.height(40.dp))
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
    canDelete: Boolean
) {
    val context = LocalContext.current
    var expanded by remember { mutableStateOf(index == 0) }

    val imagePicker = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            vm.setQuestionImageUri(index, it)
            vm.uploadQuestionImage(context, index)
        }
    }

    Surface(
        shape  = RoundedCornerShape(18.dp),
        color  = CCard,
        border = BorderStroke(1.dp, CBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Заголовок карточки
            Row(
                modifier              = Modifier.fillMaxWidth(),
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment     = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f).clickable { expanded = !expanded }
                ) {
                    Box(
                        modifier = Modifier.size(32.dp).clip(CircleShape).background(CPurple.copy(0.18f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("${index + 1}", color = CPurple, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                    Text(
                        question.text.ifBlank { "Вопрос ${index + 1}" },
                        color      = if (question.text.isBlank()) CTextSec else CTextPri,
                        fontSize   = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines   = 1,
                        overflow   = TextOverflow.Ellipsis,
                        modifier   = Modifier.weight(1f)
                    )
                }
                Row {
                    IconButton(onClick = { expanded = !expanded }, modifier = Modifier.size(32.dp)) {
                        Icon(
                            if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            null, tint = CTextSec, modifier = Modifier.size(18.dp)
                        )
                    }
                    if (canDelete) {
                        IconButton(onClick = { vm.removeQuestion(index) }, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Default.DeleteOutline, null, tint = CRed, modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }

            // Раскрытое содержимое
            AnimatedVisibility(visible = expanded) {
                Column(
                    modifier            = Modifier.padding(top = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
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
                            .height(if (question.imageUrl != null) 140.dp else 52.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(CBg)
                            .border(1.dp, CBorder, RoundedCornerShape(12.dp))
                            .clickable { imagePicker.launch("image/*") },
                        contentAlignment = Alignment.Center
                    ) {
                        if (question.imageUrl != null) {
                            AsyncImage(
                                model             = question.imageUrl,
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
                            CircularProgressIndicator(modifier = Modifier.size(22.dp), strokeWidth = 2.dp, color = CPurple)
                        } else {
                            Row(
                                verticalAlignment     = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(Icons.Default.Image, null, tint = CTextSec, modifier = Modifier.size(16.dp))
                                Text("Добавить фото / GIF (необязательно)", color = CTextSec, fontSize = 12.sp)
                            }
                        }
                    }

                    // Варианты ответов
                    Text("Варианты ответов", color = CTextSec, fontSize = 12.sp, fontWeight = FontWeight.Medium)
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
                                    .background(if (isCorrect) CGreen.copy(0.2f) else CCard)
                                    .border(
                                        1.5.dp,
                                        if (isCorrect) CGreen else CBorder,
                                        CircleShape
                                    )
                                    .clickable { vm.setCorrectAnswer(index, optIndex) },
                                contentAlignment = Alignment.Center
                            ) {
                                if (isCorrect)
                                    Icon(Icons.Default.Check, null,
                                        tint = CGreen, modifier = Modifier.size(14.dp))
                            }

                            OutlinedTextField(
                                value         = opt,
                                onValueChange = { vm.updateOption(index, optIndex, it) },
                                modifier      = Modifier.weight(1f),
                                placeholder   = { Text("Вариант ${optIndex + 1}", color = CTextSec, fontSize = 13.sp) },
                                singleLine    = true,
                                shape         = RoundedCornerShape(10.dp),
                                colors        = creatorFieldColors(),
                                textStyle     = LocalTextStyle.current.copy(fontSize = 13.sp, color = CTextPri)
                            )

                            // Удалить вариант
                            if (question.options.size > 2) {
                                IconButton(
                                    onClick  = { vm.removeOptionFromQuestion(index, optIndex) },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(Icons.Default.Close, null, tint = CTextSec, modifier = Modifier.size(14.dp))
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
                            Icon(Icons.Default.Add, null, tint = CPurple, modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Добавить вариант", color = CPurple, fontSize = 12.sp)
                        }
                    }

                    HorizontalDivider(color = CBorder.copy(0.5f))

                    // Баллы + время в строку
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Баллы", color = CTextSec, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                            Spacer(Modifier.height(4.dp))
                            PointsSelector(
                                value    = question.points,
                                onChange = { vm.updatePoints(index, it) }
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Время (сек)", color = CTextSec, fontSize = 12.sp, fontWeight = FontWeight.Medium)
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
                    Modifier.size(72.dp).clip(CircleShape).background(CCard),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Folder, null, tint = CTextSec, modifier = Modifier.size(32.dp))
                }
                Text("Черновиков пока нет", color = CTextPri, fontWeight = FontWeight.SemiBold, fontSize = 17.sp)
                Text("Сохраните незаконченную викторину,\nчтобы вернуться к ней позже",
                    color = CTextSec, fontSize = 14.sp, textAlign = TextAlign.Center)
                Spacer(Modifier.height(8.dp))
                Button(
                    onClick = onNewDraft,
                    colors  = ButtonDefaults.buttonColors(containerColor = CPurple),
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
        contentPadding      = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier            = Modifier.fillMaxSize()
    ) {
        item {
            Text(
                "Черновики (${drafts.size})",
                color      = CTextSec,
                fontSize   = 13.sp,
                modifier   = Modifier.padding(bottom = 4.dp)
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
        shape    = RoundedCornerShape(16.dp),
        color    = CCard,
        border   = BorderStroke(1.dp, CBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier          = Modifier.fillMaxWidth().padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                Modifier.size(48.dp).clip(RoundedCornerShape(12.dp))
                    .background(CPurple.copy(0.18f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.EditNote, null, tint = CPurple, modifier = Modifier.size(24.dp))
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    draft.title.ifBlank { "Без названия" },
                    color      = if (draft.title.isBlank()) CTextSec else CTextPri,
                    fontWeight = FontWeight.SemiBold,
                    fontSize   = 14.sp,
                    maxLines   = 1,
                    overflow   = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(3.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("${draft.questions.size} вопр.", color = CTextSec, fontSize = 12.sp)
                    Text("·", color = CTextSec, fontSize = 12.sp)
                    Text(draft.categoryId, color = CPurple, fontSize = 12.sp)
                    Text("·", color = CTextSec, fontSize = 12.sp)
                    Text(dateStr, color = CTextSec, fontSize = 12.sp)
                }
            }
            Spacer(Modifier.width(8.dp))
            // Кнопки
            Column(horizontalAlignment = Alignment.End) {
                TextButton(
                    onClick         = onLoad,
                    contentPadding  = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text("Открыть", color = CPurple, fontSize = 13.sp)
                }
                IconButton(onClick = onDelete, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Default.DeleteOutline, null, tint = CRed, modifier = Modifier.size(16.dp))
                }
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
                color    = CTextSec,
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
    val gradient = templateGradients[colorIndex % templateGradients.size]
    val diffColor = diffColors[template.difficulty] ?: CTextSec
    val diffLabel = diffLabels[template.difficulty] ?: ""

    Surface(
        shape  = RoundedCornerShape(18.dp),
        color  = CCard,
        border = BorderStroke(1.dp, CBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier          = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Эмодзи в цветном фоне
            Box(
                modifier = Modifier.size(56.dp).clip(RoundedCornerShape(14.dp))
                    .background(Brush.linearGradient(gradient)),
                contentAlignment = Alignment.Center
            ) {
                Text(template.emoji, fontSize = 26.sp)
            }

            Spacer(Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(template.title, color = CTextPri, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                Spacer(Modifier.height(4.dp))
                Text(template.description, color = CTextSec, fontSize = 12.sp, maxLines = 2)
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    InfoChip("${template.questionCount} вопр.", CTextSec)
                    InfoChip("${template.timePerQuestion}с", CTextSec)
                    InfoChip(diffLabel, diffColor)
                    InfoChip("${template.defaultPoints}★", CAmber)
                }
            }

            Spacer(Modifier.width(10.dp))

            Button(
                onClick = onApply,
                colors  = ButtonDefaults.buttonColors(containerColor = CPurple),
                shape   = RoundedCornerShape(10.dp),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
            ) {
                Text("Выбрать", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
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
        color      = CTextSec,
        fontSize   = 12.sp,
        fontWeight = FontWeight.Medium,
        modifier   = Modifier.padding(bottom = 6.dp)
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
        placeholder   = { Text(placeholder, color = CTextSec, fontSize = 14.sp) },
        maxLines      = maxLines,
        shape         = RoundedCornerShape(12.dp),
        colors        = creatorFieldColors(),
        textStyle     = LocalTextStyle.current.copy(fontSize = 14.sp, color = CTextPri),
        suffix        = if (maxLines > 1) {
            { Text("${value.length}/$maxLength", color = CTextSec, fontSize = 11.sp) }
        } else null,
        keyboardOptions = KeyboardOptions(imeAction = if (maxLines == 1) ImeAction.Next else ImeAction.Default)
    )
}

@Composable
private fun creatorFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor      = CPurple,
    unfocusedBorderColor    = CBorder,
    focusedContainerColor   = CCard,
    unfocusedContainerColor = CCard,
    focusedTextColor        = CTextPri,
    unfocusedTextColor      = CTextPri,
    cursorColor             = CPurple
)

@Composable
private fun CategoryDropdown(selected: String, onSelect: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        OutlinedButton(
            onClick = { expanded = true },
            shape   = RoundedCornerShape(12.dp),
            border  = BorderStroke(1.dp, if (expanded) CPurple else CBorder),
            colors  = ButtonDefaults.outlinedButtonColors(containerColor = CCard),
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 14.dp)
        ) {
            Text(selected, color = CTextPri, fontSize = 13.sp, modifier = Modifier.weight(1f),
                overflow = TextOverflow.Ellipsis, maxLines = 1)
            Icon(Icons.Default.ArrowDropDown, null, tint = CTextSec, modifier = Modifier.size(18.dp))
        }
        DropdownMenu(
            expanded         = expanded,
            onDismissRequest = { expanded = false },
            modifier         = Modifier.background(CCard)
        ) {
            allCategories.forEach { cat ->
                DropdownMenuItem(
                    text    = { Text(cat, color = if (cat == selected) CPurple else CTextPri, fontSize = 14.sp) },
                    onClick = { onSelect(cat); expanded = false }
                )
            }
        }
    }
}

@Composable
private fun DifficultyDropdown(selected: QuizDifficulty, onSelect: (QuizDifficulty) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val color = diffColors[selected] ?: CTextSec
    Box {
        OutlinedButton(
            onClick = { expanded = true },
            shape   = RoundedCornerShape(12.dp),
            border  = BorderStroke(1.dp, if (expanded) color else CBorder),
            colors  = ButtonDefaults.outlinedButtonColors(containerColor = CCard),
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 14.dp)
        ) {
            Box(Modifier.size(8.dp).clip(CircleShape).background(color))
            Spacer(Modifier.width(6.dp))
            Text(diffLabels[selected] ?: "", color = color, fontSize = 13.sp, modifier = Modifier.weight(1f))
            Icon(Icons.Default.ArrowDropDown, null, tint = CTextSec, modifier = Modifier.size(18.dp))
        }
        DropdownMenu(
            expanded         = expanded,
            onDismissRequest = { expanded = false },
            modifier         = Modifier.background(CCard)
        ) {
            QuizDifficulty.entries.forEach { diff ->
                val dc = diffColors[diff] ?: CTextSec
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
            Text("$min с", color = CTextSec, fontSize = 11.sp)
            Text(
                "$value с",
                color      = CPurple,
                fontSize   = 13.sp,
                fontWeight = FontWeight.Bold
            )
            Text("$max с", color = CTextSec, fontSize = 11.sp)
        }
        Slider(
            value         = value.toFloat(),
            onValueChange = { onChange(((it / step).toInt() * step).coerceIn(min, max)) },
            valueRange    = min.toFloat()..max.toFloat(),
            steps         = (max - min) / step - 1,
            colors        = SliderDefaults.colors(
                thumbColor        = CPurple,
                activeTrackColor  = CPurple,
                inactiveTrackColor = CBorder
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
                color    = if (isSelected) CPurple.copy(0.2f) else CCard,
                border   = BorderStroke(1.dp, if (isSelected) CPurple else CBorder),
                modifier = Modifier.clickable { onChange(pts) }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    Text("$pts", color = if (isSelected) CPurple else CTextSec,
                        fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    Text("★", color = CAmber, fontSize = 11.sp)
                }
            }
        }
    }
}

@Composable
private fun InfoChip(text: String, color: Color) {
    Surface(shape = RoundedCornerShape(6.dp), color = color.copy(0.12f)) {
        Text(text, modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp),
            fontSize = 11.sp, color = color, fontWeight = FontWeight.Medium)
    }
}