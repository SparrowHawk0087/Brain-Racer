package com.example.brainracer.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview


//@Preview
@Composable
fun QuizListScreen(
    onQuizClick: (String) -> Unit,
    /*TODO:quizViewModel: QuizViewModel = viewModel()*/
) {
    // TODO:val quizzes = quizViewModel.quizzes.collectAsState()

    LaunchedEffect(Unit) {
        /*TODO:quizViewModel.fetchQuizzes()*/
    }
    /*TODO:LazyColumn {
        items(quizzes.value) { quiz ->
            Text(
                text = "Quiz: ${quiz.title}",
                modifier = Modifier.clickable { onQuizClick(quiz.id) }
            )
        }
    }*/
}