package com.example.brainracer.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.brainracer.data.repositories.QuizRepositoryImpl
import com.example.brainracer.domain.entities.QuizDifficulty
import com.example.brainracer.ui.utils.QuizItem
import kotlinx.coroutines.delay

// ─── Палитра ───────────────────────────────────────────────────────────────
private val SBg       = Color(0xFF0F0F1A)
private val SCard     = Color(0xFF1A1A2E)
private val SBorder   = Color(0xFF2A2A3E)
private val SPurple   = Color(0xFF667EEA)
private val STextPri  = Color(0xFFFFFFFF)
private val STextSec  = Color(0xFF8B8AAE)

private val sCardGradients = listOf(
    listOf(Color(0xFF667EEA), Color(0xFF764BA2)),
    listOf(Color(0xFFf093fb), Color(0xFFf5576c)),
    listOf(Color(0xFF4facfe), Color(0xFF00f2fe)),
    listOf(Color(0xFF43e97b), Color(0xFF38f9d7)),
    listOf(Color(0xFFfa709a), Color(0xFFfee140)),
    listOf(Color(0xFFa18cd1), Color(0xFFfbc2eb)),
)

// Сложность → метка + цвет
private val difficultyOptions = listOf(
    Triple("Все уровни", null, STextSec),
    Triple("Лёгкий",    QuizDifficulty.EASY,   Color(0xFF43e97b)),
    Triple("Средний",   QuizDifficulty.MEDIUM,  Color(0xFF4facfe)),
    Triple("Сложный",   QuizDifficulty.HARD,    Color(0xFFf5576c)),
    Triple("Эксперт",   QuizDifficulty.EXPERT,  Color(0xFFFFD700)),
)

private val allCategories = listOf(
    "Все", "География", "История", "Математика",
    "Фильмы и музыка", "Наука", "Спорт"
)

// ══════════════════════════════════════════════════════════════════════════════

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    navController: NavController,
    initialCategory: String = "Все"   // передаётся при переходе «Смотреть все»
) {
    val repo          = remember { QuizRepositoryImpl() }
    val scope         = rememberCoroutineScope()
    val focusRequester = remember { FocusRequester() }
    val focusManager  = LocalFocusManager.current
    val keyboard      = LocalSoftwareKeyboardController.current

    var query            by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf(initialCategory) }
    var selectedDifficulty by remember { mutableStateOf<QuizDifficulty?>(null) }
    var results          by remember { mutableStateOf<List<QuizItem>>(emptyList()) }
    var isLoading        by remember { mutableStateOf(false) }
    var hasSearched      by remember { mutableStateOf(false) }

    // Фокус на поле ввода при открытии
    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
        // Сразу загружаем, если передана конкретная категория
        if (initialCategory != "Все") {
            isLoading = true
            hasSearched = true
            try {
                val raw = repo.getQuizzesByCategory(initialCategory, 50)
                val list = (raw as? com.example.brainracer.data.utils.Result.Success)
                    ?.data?.map { quiz ->
                        QuizItem(
                            id            = quiz.id,
                            title         = quiz.title,
                            category      = quiz.categoryId,
                            questionCount = quiz.questions.size,
                            difficulty    = quiz.difficulty.name,
                            description   = quiz.description,
                            rating        = quiz.stats.averageRating,
                            playCount     = quiz.stats.timesTaken
                        )
                    } ?: emptyList()
                results = list
            } catch (_: Exception) {}
            isLoading = false
        }
    }

    // Debounce-поиск при вводе текста
    LaunchedEffect(query, selectedCategory, selectedDifficulty) {
        if (query.isBlank() && selectedCategory == "Все" && selectedDifficulty == null) {
            if (!hasSearched) return@LaunchedEffect
        }
        delay(400)
        isLoading = true
        hasSearched = true
        try {
            val categoryArg = if (selectedCategory == "Все") null else selectedCategory
            val raw = if (query.isBlank()) {
                if (categoryArg != null)
                    repo.getQuizzesByCategory(categoryArg, 50)
                else
                    repo.getPopularQuizzes(50)
            } else {
                repo.searchQuizzes(query, categoryArg)
            }
            var list = (raw as? com.example.brainracer.data.utils.Result.Success)
                ?.data?.map { quiz ->
                    QuizItem(
                        id            = quiz.id,
                        title         = quiz.title,
                        category      = quiz.categoryId,
                        questionCount = quiz.questions.size,
                        difficulty    = quiz.difficulty.name,
                        description   = quiz.description,
                        rating        = quiz.stats.averageRating,
                        playCount     = quiz.stats.timesTaken
                    )
                } ?: emptyList()

            // Фильтр по сложности на клиенте
            selectedDifficulty?.let { diff ->
                list = list.filter { it.difficulty == diff.name }
            }
            results = list
        } catch (_: Exception) {}
        isLoading = false
    }

    Scaffold(
        containerColor = SBg,
        topBar = {
            Surface(color = SBg) {
                Column {
                    // ── Строка поиска ──────────────────────────────────────
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .windowInsetsPadding(WindowInsets.statusBars)
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        IconButton(onClick = { navController.popBackStack() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = STextPri)
                        }

                        OutlinedTextField(
                            value         = query,
                            onValueChange = { query = it },
                            placeholder   = { Text("Найти викторину…", color = STextSec, fontSize = 14.sp) },
                            leadingIcon   = {
                                Icon(Icons.Default.Search, null, tint = STextSec, modifier = Modifier.size(18.dp))
                            },
                            trailingIcon  = {
                                AnimatedVisibility(
                                    visible = query.isNotEmpty(),
                                    enter   = fadeIn(),
                                    exit    = fadeOut()
                                ) {
                                    IconButton(onClick = { query = "" }) {
                                        Icon(Icons.Default.Clear, null, tint = STextSec, modifier = Modifier.size(16.dp))
                                    }
                                }
                            },
                            modifier = Modifier
                                .weight(1f)
                                .focusRequester(focusRequester),
                            singleLine    = true,
                            shape         = RoundedCornerShape(14.dp),
                            colors        = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor   = SPurple,
                                unfocusedBorderColor = SBorder,
                                focusedTextColor     = STextPri,
                                unfocusedTextColor   = STextPri,
                                cursorColor          = SPurple,
                                focusedContainerColor   = SCard,
                                unfocusedContainerColor = SCard
                            ),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                            keyboardActions = KeyboardActions(onSearch = {
                                keyboard?.hide()
                                focusManager.clearFocus()
                            }),
                            textStyle = LocalTextStyle.current.copy(fontSize = 14.sp)
                        )
                    }

                    // ── Фильтр по категориям ───────────────────────────────
                    LazyRow(
                        contentPadding        = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(allCategories) { cat ->
                            val isSelected = cat == selectedCategory
                            Surface(
                                shape  = RoundedCornerShape(20.dp),
                                color  = if (isSelected) SPurple else SCard,
                                border = if (!isSelected) BorderStroke(1.dp, SBorder) else null,
                                modifier = Modifier.clickable { selectedCategory = cat }
                            ) {
                                Text(
                                    cat,
                                    modifier   = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                                    fontSize   = 13.sp,
                                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                    color      = if (isSelected) Color.White else STextSec
                                )
                            }
                        }
                    }

                    // ── Фильтр по сложности ────────────────────────────────
                    LazyRow(
                        contentPadding        = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(difficultyOptions) { (label, diff, color) ->
                            val isSelected = selectedDifficulty == diff
                            Surface(
                                shape  = RoundedCornerShape(20.dp),
                                color  = if (isSelected) color.copy(alpha = 0.25f) else SCard,
                                border = BorderStroke(
                                    1.dp,
                                    if (isSelected) color else SBorder
                                ),
                                modifier = Modifier.clickable {
                                    selectedDifficulty = if (isSelected) null else diff
                                }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                                ) {
                                    if (diff != null) {
                                        Box(
                                            modifier = Modifier.size(7.dp).clip(CircleShape)
                                                .background(color)
                                        )
                                    }
                                    Text(
                                        label,
                                        fontSize   = 12.sp,
                                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                        color      = if (isSelected) color else STextSec
                                    )
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(4.dp))
                    HorizontalDivider(color = SBorder, thickness = 1.dp)
                }
            }
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            when {
                // ── Загрузка ──────────────────────────────────────────────
                isLoading -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = SPurple)
                    }
                }

                // ── Начальное состояние ────────────────────────────────────
                !hasSearched -> {
                    SearchEmptyState(
                        icon    = Icons.Default.Search,
                        title   = "Найдите викторину",
                        subtitle = "Введите название или выберите категорию"
                    )
                }

                // ── Нет результатов ────────────────────────────────────────
                results.isEmpty() -> {
                    SearchEmptyState(
                        icon    = Icons.Default.SearchOff,
                        title   = "Ничего не найдено",
                        subtitle = "Попробуйте изменить запрос или фильтры"
                    )
                }

                // ── Список результатов ─────────────────────────────────────
                else -> {
                    LazyColumn(
                        contentPadding      = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        item {
                            Text(
                                "Найдено: ${results.size}",
                                color    = STextSec,
                                fontSize = 13.sp,
                                modifier = Modifier.padding(bottom = 4.dp)
                            )
                        }
                        itemsIndexed(results) { index, quiz ->
                            SearchResultCard(
                                quiz       = quiz,
                                colorIndex = index,
                                highlight  = query,
                                onClick    = { navController.navigate("quiz_detail/${quiz.id}") }
                            )
                        }
                    }
                }
            }
        }
    }
}

// ── Карточка результата ────────────────────────────────────────────────────

@Composable
private fun SearchResultCard(
    quiz: QuizItem,
    colorIndex: Int,
    highlight: String,
    onClick: () -> Unit
) {
    val gradient = sCardGradients[colorIndex % sCardGradients.size]

    val diffColor = when (quiz.difficulty) {
        "EASY"   -> Color(0xFF43e97b)
        "MEDIUM" -> Color(0xFF4facfe)
        "HARD"   -> Color(0xFFf5576c)
        "EXPERT" -> Color(0xFFFFD700)
        else     -> STextSec
    }
    val diffLabel = when (quiz.difficulty) {
        "EASY"   -> "Лёгкий"
        "MEDIUM" -> "Средний"
        "HARD"   -> "Сложный"
        "EXPERT" -> "Эксперт"
        else     -> quiz.difficulty
    }

    Box(
        modifier = Modifier.fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(SCard)
            .border(1.dp, SBorder, RoundedCornerShape(16.dp))
            .clickable { onClick() }
    ) {
        Row(
            modifier          = Modifier.fillMaxWidth().padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Цветная иконка
            Box(
                modifier = Modifier.size(56.dp).clip(RoundedCornerShape(14.dp))
                    .background(Brush.linearGradient(gradient, Offset.Zero, Offset(400f, 400f))),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Quiz, null, tint = Color.White, modifier = Modifier.size(28.dp))
            }

            Spacer(Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    quiz.title,
                    fontWeight = FontWeight.SemiBold,
                    fontSize   = 14.sp,
                    color      = STextPri,
                    maxLines   = 2,
                    overflow   = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(5.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment     = Alignment.CenterVertically
                ) {
                    // Категория
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = SPurple.copy(alpha = 0.15f)
                    ) {
                        Text(
                            quiz.category,
                            modifier   = Modifier.padding(horizontal = 7.dp, vertical = 2.dp),
                            fontSize   = 11.sp,
                            color      = SPurple,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    // Сложность
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = diffColor.copy(alpha = 0.12f)
                    ) {
                        Text(
                            diffLabel,
                            modifier   = Modifier.padding(horizontal = 7.dp, vertical = 2.dp),
                            fontSize   = 11.sp,
                            color      = diffColor,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    // Вопросы
                    Text("${quiz.questionCount} вопр.", fontSize = 11.sp, color = STextSec)
                }
            }

            Icon(Icons.Default.ChevronRight, null, tint = STextSec, modifier = Modifier.size(20.dp))
        }
    }
}

// ── Пустое состояние ────────────────────────────────────────────────────────

@Composable
private fun SearchEmptyState(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String
) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier            = Modifier.padding(32.dp)
        ) {
            Box(
                modifier = Modifier.size(72.dp).clip(CircleShape)
                    .background(SCard),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, null, tint = STextSec, modifier = Modifier.size(32.dp))
            }
            Text(title, color = STextPri, fontWeight = FontWeight.SemiBold, fontSize = 17.sp)
            Text(
                subtitle,
                color     = STextSec,
                fontSize  = 14.sp,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    }
}
