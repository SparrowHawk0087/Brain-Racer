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
import com.example.brainracer.ui.theme.LocalBrainRacerExtendedColors
import com.example.brainracer.ui.utils.HOME_CATEGORY_CUSTOM
import com.example.brainracer.ui.utils.QuizItem
import com.example.brainracer.ui.utils.customAuthorCaption
import com.example.brainracer.ui.utils.toQuizItem
import kotlinx.coroutines.delay

private val allCategories = listOf(
    "Все", "География", "История", "Математика",
    "Фильмы и музыка", "Наука", "Спорт", HOME_CATEGORY_CUSTOM
)

// ══════════════════════════════════════════════════════════════════════════════

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    navController: NavController,
    initialCategory: String = "Все",   // передаётся при переходе «Смотреть все»
    initialCustomOnly: Boolean = false // только викторины id quiz_custom_*
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

    val cs = MaterialTheme.colorScheme
    val ext = LocalBrainRacerExtendedColors.current
    val difficultyOptions = remember(cs, ext) {
        listOf(
            Triple("Все уровни", null, cs.onSurfaceVariant),
            Triple("Лёгкий", QuizDifficulty.EASY, ext.difficultyEasy),
            Triple("Средний", QuizDifficulty.MEDIUM, ext.difficultyMedium),
            Triple("Сложный", QuizDifficulty.HARD, ext.difficultyHard),
            Triple("Эксперт", QuizDifficulty.EXPERT, ext.difficultyExpert),
        )
    }

    fun mapQuizRows(data: List<com.example.brainracer.domain.entities.Quiz>): List<QuizItem> =
        data.map { it.toQuizItem() }

    /**
     * Режим только кастомных: вкладка «Кастомные» или вход с `customOnly=true` пока выбрано «Все».
     * Смена категории (напр. «География») выключает принудительный кастомный режим с deep link.
     */
    fun customCatalogActive(): Boolean = when {
        selectedCategory == HOME_CATEGORY_CUSTOM -> true
        initialCustomOnly && selectedCategory == "Все" -> true
        else -> false
    }

    /** Категория предмета для Firestore; «Кастомные» — не поле categoryId у квиза. */
    fun subjectCategoryArg(): String? = when (selectedCategory) {
        "Все", HOME_CATEGORY_CUSTOM -> null
        else -> selectedCategory
    }

    // Фокус на поле ввода при открытии; стартовая загрузка по режиму
    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
        when {
            initialCustomOnly || initialCategory == HOME_CATEGORY_CUSTOM -> {
                isLoading = true
                hasSearched = true
                try {
                    val raw = repo.getPublicCustomQuizzes(120)
                    results = (raw as? com.example.brainracer.data.utils.Result.Success)?.data
                        ?.let { mapQuizRows(it) } ?: emptyList()
                } catch (_: Exception) {}
                isLoading = false
            }
            initialCategory != "Все" -> {
                isLoading = true
                hasSearched = true
                try {
                    val raw = repo.getQuizzesByCategory(initialCategory, 50)
                    results = (raw as? com.example.brainracer.data.utils.Result.Success)
                        ?.data?.let { mapQuizRows(it) } ?: emptyList()
                } catch (_: Exception) {}
                isLoading = false
            }
        }
    }

    // Debounce-поиск при вводе текста
    LaunchedEffect(query, selectedCategory, selectedDifficulty, initialCustomOnly) {
        if (query.isBlank() && selectedCategory == "Все" && selectedDifficulty == null) {
            if (!hasSearched) return@LaunchedEffect
        }
        delay(400)
        isLoading = true
        hasSearched = true
        try {
            val subjectCat = subjectCategoryArg()
            val customMode = customCatalogActive()
            val raw: com.example.brainracer.data.utils.Result<List<com.example.brainracer.domain.entities.Quiz>> =
                when {
                    customMode && query.isBlank() ->
                        repo.getPublicCustomQuizzes(120)
                    customMode && query.isNotBlank() -> {
                        val needle = query.trim().lowercase()
                        when (val r = repo.getPublicCustomQuizzes(200)) {
                            is com.example.brainracer.data.utils.Result.Success -> {
                                val filtered = r.data.filter { q ->
                                    q.title.lowercase().contains(needle) ||
                                        q.description.lowercase().contains(needle)
                                }
                                com.example.brainracer.data.utils.Result.success(filtered)
                            }
                            is com.example.brainracer.data.utils.Result.Error -> r
                        }
                    }
                    query.isBlank() -> {
                        if (subjectCat != null)
                            repo.getQuizzesByCategory(subjectCat, 50)
                        else
                            repo.getPopularQuizzes(50)
                    }
                    else -> repo.searchQuizzes(query, subjectCat)
                }
            var list = (raw as? com.example.brainracer.data.utils.Result.Success)
                ?.data?.let { mapQuizRows(it) } ?: emptyList()

            if (customMode) {
                list = list.filter { it.id.startsWith("quiz_custom_") }
            }

            // Фильтр по сложности на клиенте
            selectedDifficulty?.let { diff ->
                list = list.filter { it.difficulty == diff.name }
            }
            results = list
        } catch (_: Exception) {}
        isLoading = false
    }

    Scaffold(
        containerColor = cs.background,
        topBar = {
            Surface(color = cs.background) {
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
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = cs.onBackground)
                        }

                        OutlinedTextField(
                            value         = query,
                            onValueChange = { query = it },
                            placeholder   = {
                                Text(
                                    if (customCatalogActive()) "Поиск среди кастомных…" else "Найти викторину…",
                                    color = cs.onSurfaceVariant,
                                    fontSize = 14.sp
                                )
                            },
                            leadingIcon   = {
                                Icon(Icons.Default.Search, null, tint = cs.onSurfaceVariant, modifier = Modifier.size(18.dp))
                            },
                            trailingIcon  = {
                                AnimatedVisibility(
                                    visible = query.isNotEmpty(),
                                    enter   = fadeIn(),
                                    exit    = fadeOut()
                                ) {
                                    IconButton(onClick = { query = "" }) {
                                        Icon(Icons.Default.Clear, null, tint = cs.onSurfaceVariant, modifier = Modifier.size(16.dp))
                                    }
                                }
                            },
                            modifier = Modifier
                                .weight(1f)
                                .focusRequester(focusRequester),
                            singleLine    = true,
                            shape         = RoundedCornerShape(14.dp),
                            colors        = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor   = cs.primary,
                                unfocusedBorderColor = cs.outline,
                                focusedTextColor     = cs.onSurface,
                                unfocusedTextColor   = cs.onSurface,
                                cursorColor          = cs.primary,
                                focusedContainerColor   = cs.surface,
                                unfocusedContainerColor = cs.surface
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
                                color  = if (isSelected) cs.primary else cs.surface,
                                border = if (!isSelected) BorderStroke(1.dp, cs.outline) else null,
                                modifier = Modifier.clickable { selectedCategory = cat }
                            ) {
                                Text(
                                    cat,
                                    modifier   = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                                    fontSize   = 13.sp,
                                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                    color      = if (isSelected) cs.onPrimary else cs.onSurfaceVariant
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
                                color  = if (isSelected) color.copy(alpha = 0.25f) else cs.surface,
                                border = BorderStroke(
                                    1.dp,
                                    if (isSelected) color else cs.outline
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
                                        color      = if (isSelected) color else cs.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(4.dp))
                    HorizontalDivider(color = cs.outline, thickness = 1.dp)
                }
            }
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            when {
                // ── Загрузка ──────────────────────────────────────────────
                isLoading -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = cs.primary)
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
                                color    = cs.onSurfaceVariant,
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
    val cs = MaterialTheme.colorScheme
    val ext = LocalBrainRacerExtendedColors.current
    val gradients = ext.cardGradients
    val gradient = gradients[colorIndex % gradients.size]

    val diffColor = when (quiz.difficulty) {
        "EASY"   -> ext.difficultyEasy
        "MEDIUM" -> ext.difficultyMedium
        "HARD"   -> ext.difficultyHard
        "EXPERT" -> ext.difficultyExpert
        else     -> cs.onSurfaceVariant
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
            .background(cs.surface)
            .border(1.dp, cs.outline, RoundedCornerShape(16.dp))
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
                Icon(Icons.Default.Quiz, null, tint = cs.onPrimary, modifier = Modifier.size(28.dp))
            }

            Spacer(Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    quiz.title,
                    fontWeight = FontWeight.SemiBold,
                    fontSize   = 14.sp,
                    color      = cs.onSurface,
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
                        color = cs.primary.copy(alpha = 0.15f)
                    ) {
                        Text(
                            quiz.category,
                            modifier   = Modifier.padding(horizontal = 7.dp, vertical = 2.dp),
                            fontSize   = 11.sp,
                            color      = cs.primary,
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
                    Text("${quiz.questionCount} вопр.", fontSize = 11.sp, color = cs.onSurfaceVariant)
                }
                quiz.customAuthorCaption()?.let { cap ->
                    Spacer(Modifier.height(4.dp))
                    Text(
                        cap,
                        fontSize = 11.sp,
                        color = cs.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Icon(Icons.Default.ChevronRight, null, tint = cs.onSurfaceVariant, modifier = Modifier.size(20.dp))
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
    val cs = MaterialTheme.colorScheme
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier            = Modifier.padding(32.dp)
        ) {
            Box(
                modifier = Modifier.size(72.dp).clip(CircleShape)
                    .background(cs.surface),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, null, tint = cs.onSurfaceVariant, modifier = Modifier.size(32.dp))
            }
            Text(title, color = cs.onSurface, fontWeight = FontWeight.SemiBold, fontSize = 17.sp)
            Text(
                subtitle,
                color     = cs.onSurfaceVariant,
                fontSize  = 14.sp,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    }
}
