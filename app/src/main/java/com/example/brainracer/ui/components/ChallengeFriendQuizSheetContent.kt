package com.example.brainracer.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Quiz
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.brainracer.R
import com.example.brainracer.domain.entities.User
import com.example.brainracer.ui.theme.LocalBrainRacerExtendedColors
import com.example.brainracer.ui.utils.HOME_CATEGORY_CUSTOM
import com.example.brainracer.ui.utils.QuizItem
import com.example.brainracer.ui.utils.customAuthorCaption
import kotlin.math.abs

private val challengePickerCategories = listOf(
    "Все", "География", "История", "Математика",
    "Фильмы и музыка", "Наука", "Спорт", HOME_CATEGORY_CUSTOM
)

// Верх sheet (заголовок и подсказка) в режиме выбора друга
private val friendSheetChromeDp = 168.dp

// Верх sheet в режиме выбора викторины: заголовок, ник, поиск, чипы, отступы
private val quizSheetChromeDp = 292.dp

// Доля высоты экрана: максимум для всего контента bottom sheet
private const val SHEET_MAX_HEIGHT_FRACTION = 0.88f

// Контент bottom sheet: выбор друга (если не зафиксирован) и викторины для вызова
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ChallengeFriendQuizSheetContent(
    fixedFriend: User?,
    friends: List<User>,
    quizzes: List<QuizItem>,
    isLoading: Boolean,
    onDismiss: () -> Unit,
    onSendChallenge: (friendId: String, quizId: String, quizTitle: String) -> Unit
) {
    var selectedFriend by remember(fixedFriend?.id) { mutableStateOf(fixedFriend) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("Все") }

    val configuration = LocalConfiguration.current
    val maxSheetHeight = remember(configuration.screenHeightDp, configuration.screenWidthDp) {
        (configuration.screenHeightDp * SHEET_MAX_HEIGHT_FRACTION).dp
    }
    val friendListMaxHeight = remember(maxSheetHeight) {
        (maxSheetHeight - friendSheetChromeDp).coerceAtLeast(140.dp)
    }
    val quizListMaxHeight = remember(maxSheetHeight) {
        (maxSheetHeight - quizSheetChromeDp).coerceIn(200.dp, 480.dp)
    }

    Column(
        Modifier
            .fillMaxWidth()
            .heightIn(max = maxSheetHeight)
            .padding(horizontal = 16.dp)
            .padding(bottom = 20.dp)
            .navigationBarsPadding()
    ) {
        if (fixedFriend == null && selectedFriend == null) {
            ChallengeSheetHeader(
                title = "Бросить вызов",
                subtitle = null,
                onDismiss = onDismiss
            )
            Text(
                "Выберите друга",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp, bottom = 8.dp)
            )
            AnimatedContent(
                targetState = isLoading to friends.isEmpty(),
                transitionSpec = {
                    fadeIn(tween(240, delayMillis = 40)) togetherWith fadeOut(tween(160))
                },
                label = "friendPickerState"
            ) { targetState ->
                val loading = targetState.first
                val emptyFriends = targetState.second
                when {
                    loading -> Box(Modifier
                        .fillMaxWidth()
                        .height(120.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    }
                    emptyFriends -> Text(
                        "Сначала добавьте друзей на вкладке «Друзья».",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    else -> LazyColumn(
                        modifier = Modifier.heightIn(max = friendListMaxHeight),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(friends, key = { it.id }) { u ->
                            FriendPickRow(
                                user = u,
                                onClick = { selectedFriend = u },
                                modifier = Modifier.animateItem(
                                    fadeInSpec = tween(220, delayMillis = 30),
                                    fadeOutSpec = tween(160)
                                )
                            )
                        }
                    }
                }
            }
            return
        }

        val friend = selectedFriend ?: fixedFriend!!

        ChallengeSheetHeader(
            title = "Выберите викторину",
            subtitle = "",
            onDismiss = onDismiss
        )

        Spacer(Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (fixedFriend == null) {
                IconButton(onClick = { selectedFriend = null }, modifier = Modifier.pressScale()) {
                    Icon(
                        painter = painterResource(id = R.drawable.arrow_back_btn),
                        contentDescription = "Назад",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
            Column(Modifier.weight(1f)) {
                Text(
                    friend.nickname,
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        Spacer(Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier.weight(1f),
                singleLine = true,
                placeholder = {
                    Text("Поиск викторины…", color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f))
                },
                leadingIcon = {
                    Icon(painter = painterResource(id = R.drawable.search_btn), contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                },
                shape = RoundedCornerShape(14.dp),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { }),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.85f),
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.45f),
                    focusedTextColor = MaterialTheme.colorScheme.onSurface,
                    unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                    cursorColor = MaterialTheme.colorScheme.primary,
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.28f)
                )
            )
            IconButton(
                onClick = {
                    searchQuery = ""
                    selectedCategory = "Все"
                },
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                    .pressScale()
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.filter_alt_off),
                    contentDescription = "Сбросить фильтры",
                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f)
                )
            }
        }

        Spacer(Modifier.height(10.dp))

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(vertical = 2.dp)
        ) {
            items(challengePickerCategories, key = { it }) { cat ->
                val selected = selectedCategory == cat
                FilterChip(
                    selected = selected,
                    onClick = { selectedCategory = cat },
                    label = {
                        Text(
                            cat,
                            fontSize = 12.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    shape = RoundedCornerShape(20.dp),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                        labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    border = if (selected) null else FilterChipDefaults.filterChipBorder(
                        enabled = true,
                        selected = false,
                        borderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)
                    )
                )
            }
        }

        Spacer(Modifier.height(10.dp))

        val filteredQuizzes = remember(quizzes, searchQuery, selectedCategory) {
            quizzes.filter { q ->
                val catOk = when (selectedCategory) {
                    "Все" -> true
                    HOME_CATEGORY_CUSTOM -> q.id.startsWith("quiz_custom_")
                    else -> q.category == selectedCategory
                }
                val qOk = searchQuery.isBlank() ||
                        q.title.contains(searchQuery, ignoreCase = true) ||
                        q.category.contains(searchQuery, ignoreCase = true) ||
                        q.authorNickname.contains(searchQuery, ignoreCase = true)
                catOk && qOk
            }
        }

        val bodyKey = when {
            isLoading -> 0
            filteredQuizzes.isEmpty() -> 1
            else -> 2
        }

        AnimatedContent(
            targetState = bodyKey,
            transitionSpec = {
                fadeIn(tween(260, delayMillis = 40)) togetherWith fadeOut(tween(180))
            },
            label = "quizPickerBody"
        ) { state ->
            when (state) {
                0 -> Box(
                    Modifier
                        .fillMaxWidth()
                        .height(220.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
                1 -> Text(
                    if (quizzes.isEmpty()) "Нет доступных викторин." else "Ничего не найдено по фильтру.",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 20.dp)
                )
                else -> LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = quizListMaxHeight),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(bottom = 12.dp)
                ) {
                    items(
                        items = filteredQuizzes,
                        key = { it.id }
                    ) { q ->
                        QuizPickCard(
                            quiz = q,
                            colorIndex = abs(q.id.hashCode()),
                            onSelect = { onSendChallenge(friend.id, q.id, q.title) },
                            modifier = Modifier.animateItem(
                                fadeInSpec = tween(220, delayMillis = 30),
                                fadeOutSpec = tween(160)
                            )
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ChallengeSheetHeader(
    title: String,
    subtitle: String?,
    onDismiss: () -> Unit
) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                title,
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
                color = MaterialTheme.colorScheme.onSurface,
                lineHeight = 24.sp
            )
            if (!subtitle.isNullOrBlank()) {
                Spacer(Modifier.height(4.dp))
                Text(
                    subtitle,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        TextButton(
            onClick = onDismiss,
            modifier = Modifier.pressScale(),
            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp)
        ) {
            Text(
                "Закрыть",
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp
            )
        }
    }
}

@Composable
private fun FriendPickRow(
    user: User,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        tonalElevation = 2.dp,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
        modifier = modifier
            .fillMaxWidth()
            .pressClickable(onClick = onClick)
    ) {
        Row(
            Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                painter = painterResource(id = R.drawable.cognition),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
            Column(Modifier.weight(1f)) {
                Text(user.nickname, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(user.rank.displayName, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun QuizPickCard(
    quiz: QuizItem,
    colorIndex: Int,
    onSelect: () -> Unit,
    modifier: Modifier = Modifier
) {
    val cardGradients = LocalBrainRacerExtendedColors.current.cardGradients
    val gradient = cardGradients[colorIndex % cardGradients.size]
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        tonalElevation = 2.dp,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.52f)),
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .heightIn(min = 100.dp)
                .padding(horizontal = 10.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(54.dp)
                    .clip(RoundedCornerShape(13.dp))
                    .background(Brush.linearGradient(gradient, Offset.Zero, Offset(400f, 400f))),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Quiz,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(26.dp)
                )
            }
            Column(
                Modifier
                    .weight(1f)
                    .heightIn(min = 80.dp)
            ) {
                Text(
                    quiz.title,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 17.sp
                )
                Spacer(Modifier.height(5.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        "${quiz.questionCount} вопросов",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Text(
                    quiz.category,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 2.dp)
                )
                quiz.customAuthorCaption()?.let { cap ->
                    Text(
                        cap,
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.88f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = 3.dp)
                    )
                }
            }
            Button(
                onClick = onSelect,
                modifier = Modifier
                    .height(36.dp)
                    .pressScale(),
                shape = RoundedCornerShape(11.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp, pressedElevation = 4.dp)
            ) {
                Text("Выбрать", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}
