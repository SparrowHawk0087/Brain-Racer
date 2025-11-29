package com.example.brainracer.ui.utils
import androidx.compose.runtime.Immutable

@Immutable
data class QuizUIState(
    //викторина и прогресс
    val currentQuestionIndex: Int = 0,
    val totalQuestions: Int = 0,
    val score: Int = 0,

    //вопрос
    val question: String = "",   /*текст текущего вопроса*/
    val options: List<String> = listOf("", "", "", ""), /*текущий вопрос будет ?*/

    //ответ
    val selectedAnswerIndex: Int? = null,
    val isAnswerSubmitted: Boolean = false,

    val attachedImageUrl: String? = null, // картинка

    val isLoading: Boolean = true,      // загрузка
    val isQuizCompleted: Boolean = false,    //завершена викторина или нет
    val errorMessage: String? = null   // сообщение об ошибке
)