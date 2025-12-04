package com.example.brainracer.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

@Composable
fun QuizScreen(
    // 1. Лямбды-заглушки
    onCloseClicked: () -> Unit = {},
    onAnswerSelected: () -> Unit = {},
    onNextClicked: () -> Unit = {}
) {
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        // Верхний бар с кнопкой закрытия
        topBar = {
            QuizTopBar(onCloseClicked = onCloseClicked)
        }
    ) { innerPadding ->

        // Основной контент
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
                .fillMaxSize()
                .verticalScroll(rememberScrollState()), // Чтобы скроллилось на маленьких экранах
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            // 2. Индикатор прогресса (например, вопрос 5 из 10)
            Spacer(modifier = Modifier.height(16.dp))
            LinearProgressIndicator(
                progress = 0.5f, // Статичное значение пока нет логики
                modifier = Modifier.fillMaxWidth().height(8.dp),
                strokeCap = androidx.compose.ui.graphics.StrokeCap.Round
            )
            Text(
                text = "Вопрос 5/10",
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier.padding(top = 8.dp),
                color = MaterialTheme.colorScheme.secondary
            )

            Spacer(modifier = Modifier.height(32.dp))

            // 3. Текст вопроса
            Text(
                text = "Пример вопроса?",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(32.dp))

            // 4. Список ответов
            QuizOptionItem(text = "вар 1", onClick = onAnswerSelected)
            QuizOptionItem(text = "вар 2", onClick = onAnswerSelected) // Правильный типа :)
            QuizOptionItem(text = "вар 3", onClick = onAnswerSelected)
            QuizOptionItem(text = "вар 44", onClick = onAnswerSelected)

            Spacer(modifier = Modifier.weight(1f)) // Толкает кнопку вниз

            // 5. Кнопка "Далее"
            Button(
                onClick = onNextClicked,
                modifier = Modifier.fillMaxWidth().height(50.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Следующий вопрос")
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

//Вспомогательные компоненты

@Composable
fun QuizTopBar(onCloseClicked: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onCloseClicked) {
            Icon(imageVector = Icons.Default.Close, contentDescription = "Закрыть")
        }
        Spacer(modifier = Modifier.weight(1f))
        // Сюда можно добавить таймер или очки
        Text(text = "00:30", style = MaterialTheme.typography.titleMedium)
    }
}

@Composable
fun QuizOptionItem(
    text: String,
    onClick: () -> Unit
) {
    OutlinedCard(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp), // Отступ между карточками
        shape = RoundedCornerShape(12.dp)
    ) {
        Box(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            contentAlignment = Alignment.CenterStart
        ) {
            Text(text = text, style = MaterialTheme.typography.bodyLarge)
        }
    }
}

// --- PREVIEW ---

@Preview(
    showBackground = true,
    showSystemUi = true,
    name = "Quiz Screen Preview"
)
@Composable
fun QuizScreenPreview() {
    MaterialTheme {
        QuizScreen()
    }
}