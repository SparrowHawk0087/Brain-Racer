package com.example.brainracer.data.utils

import android.util.Log
import com.example.brainracer.data.repositories.QuizRepositoryImpl
import com.example.brainracer.domain.entities.Question
import com.example.brainracer.domain.entities.QuestionType
import com.example.brainracer.domain.entities.Quiz
import com.example.brainracer.domain.entities.QuizDifficulty
import com.example.brainracer.domain.entities.QuizStats
import com.google.firebase.Timestamp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.UUID

object QuizDataSeeder {

    private val quizRepository = QuizRepositoryImpl()
    private val scope = CoroutineScope(Dispatchers.IO)

    // Фиксированные ID для каждой викторины
    private const val PLANT_QUIZ_ID = "quiz_plants_12345"
    private const val FOOD_QUIZ_ID = "quiz_food_67890"
    private const val SCIENCE_QUIZ_ID = "quiz_science_24680"
    private const val GEOMETRY_QUIZ_ID = "quiz_geometry_13579"
    private const val HISTORY_QUIZ_ID = "quiz_history_11223"
    private const val GEOGRAPHY_QUIZ_ID = "quiz_geography_44556"

    fun seedQuizzes() {
        scope.launch {
            try {
                Log.d("QuizDataSeeder", "Начинаем проверку и добавление викторин...")

                val quizzes = createSampleQuizzes()
                var addedCount = 0
                var skippedCount = 0

                for (quiz in quizzes) {
                    try {
                        // Проверяем, существует ли викторина
                        val existingQuiz = quizRepository.getQuiz(quiz.id).getOrNull()

                        if (existingQuiz == null) {
                            // Викторины нет, создаем
                            quizRepository.createQuiz(quiz).fold(
                                onSuccess = {
                                    Log.d("QuizDataSeeder", "✅ Викторина создана: ${quiz.title}")
                                    addedCount++
                                },
                                onFailure = { error ->
                                    Log.e("QuizDataSeeder", "❌ Ошибка создания викторины '${quiz.title}': ${error.message}")
                                }
                            )
                        } else {
                            Log.d("QuizDataSeeder", "⏭️ Викторина уже существует: ${quiz.title}")
                            skippedCount++
                        }

                        // Небольшая задержка между запросами
                        kotlinx.coroutines.delay(100)
                    } catch (e: Exception) {
                        Log.e("QuizDataSeeder", "Ошибка при обработке викторины '${quiz.title}': ${e.message}")
                    }
                }

                Log.d("QuizDataSeeder", "Готово! Добавлено: $addedCount, Пропущено: $skippedCount")

            } catch (e: Exception) {
                Log.e("QuizDataSeeder", "Критическая ошибка при добавлении викторин: ${e.message}")
            }
        }
    }

    fun createSampleQuizzes(): List<Quiz> {
        return listOf(
            createScienceQuiz(),
            createGeometryQuiz(),
            createHistoryQuiz(),
            createGeographyQuiz()
        )
    }

    private fun createScienceQuiz(): Quiz {
        return Quiz(
            id = SCIENCE_QUIZ_ID,
            title = "Наука: Физика и химия",
            description = "Интересные вопросы о законах физики, химических элементах и научных явлениях",
            categoryId = "Наука",
            difficulty = QuizDifficulty.HARD,
            questions = listOf(
                Question(
                    id = UUID.randomUUID().toString(),
                    questionText = "Какой газ является самым легким в мире?",
                    questionType = QuestionType.MULTIPLE_CHOICE,
                    options = listOf(
                        "Гелий",
                        "Неон",
                        "Водород",
                        "Азот"
                    ),
                    correctAnswerIndex = 2,
                    explanation = "Водород — самый лёгкий газ во Вселенной.",
                    points = 10,
                    timeLimit = 30
                ),
                Question(
                    id = UUID.randomUUID().toString(),
                    questionText = "Что такое «сублимация»?",
                    questionType = QuestionType.MULTIPLE_CHOICE,
                    options = listOf(
                        "Переход вещества из твердого состояния сразу в газообразное",
                        "Испарение жидкости",
                        "Плавление твердого тела",
                        "Конденсация пара"
                    ),
                    correctAnswerIndex = 0,
                    explanation = "Сублимация — переход вещества из твёрдого состояния сразу в газообразное, минуя жидкую фазу.",
                    points = 10,
                    timeLimit = 30
                ),
                Question(
                    id = UUID.randomUUID().toString(),
                    questionText = "Какое физическое явление объясняет, почему горящая свеча на МКС будет иметь сферическое, а не вытянутое пламя?",
                    questionType = QuestionType.MULTIPLE_CHOICE,
                    options = listOf(
                        "Отсутствие конвекции в невесомости",
                        "Пониженная гравитация",
                        "Высокое содержание кислорода",
                        "Эффект Казимира"
                    ),
                    correctAnswerIndex = 0,
                    explanation = "В невесомости отсутствует конвекция, поэтому пламя становится сферическим.",
                    points = 15,
                    timeLimit = 40
                ),
                Question(
                    id = UUID.randomUUID().toString(),
                    questionText = "Какая из этих сил не является фундаментальной в физике?",
                    questionType = QuestionType.MULTIPLE_CHOICE,
                    options = listOf(
                        "Сильное ядерное взаимодействие",
                        "Гравитация",
                        "Электромагнетизм",
                        "Инерция"
                    ),
                    correctAnswerIndex = 3,
                    explanation = "Инерция — не фундаментальная сила, а свойство массы.",
                    points = 15,
                    timeLimit = 40
                ),
                Question(
                    id = UUID.randomUUID().toString(),
                    questionText = "В чем измеряется электрическая проводимость?",
                    questionType = QuestionType.MULTIPLE_CHOICE,
                    options = listOf(
                        "В вольтах",
                        "В сименсах",
                        "В фарадах",
                        "В теслах"
                    ),
                    correctAnswerIndex = 1,
                    explanation = "Электрическая проводимость измеряется в сименсах (См).",
                    points = 10,
                    timeLimit = 30
                )
            ),
            stats = QuizStats(
                timesTaken = 0,
                averageScore = 0.0,
                totalAttempts = 0,
                completionRate = 0.0,
                ratingsCount = 0,
                averageRating = 0.0
            ),
            createdBy = "demo",
            createdAt = Timestamp.now(),
            timePerQuestion = 35
        )
    }

    private fun createGeometryQuiz(): Quiz {
        return Quiz(
            id = GEOMETRY_QUIZ_ID,
            title = "Геометрия вокруг нас",
            description = "Удивительные геометрические формы в природе, архитектуре и повседневных предметах",
            categoryId = "Математика",
            difficulty = QuizDifficulty.MEDIUM,
            questions = listOf(
                Question(
                    id = UUID.randomUUID().toString(),
                    questionText = "Почему снежинки всегда имеют шестилучевую симметрию?",
                    questionType = QuestionType.MULTIPLE_CHOICE,
                    options = listOf(
                        "Так замерзают молекулы воды",
                        "Из-за гексагональной структуры кристалла льда",
                        "Такова форма кристаллизации при низких температурах",
                        "Влияние магнитного поля Земли"
                    ),
                    correctAnswerIndex = 1,
                    explanation = "Из-за гексагональной структуры кристалла льда.",
                    points = 10,
                    timeLimit = 30
                ),
                Question(
                    id = UUID.randomUUID().toString(),
                    questionText = "Сколько элементов симметрии у куба?",
                    questionType = QuestionType.MULTIPLE_CHOICE,
                    options = listOf(
                        "24",
                        "48",
                        "12",
                        "6"
                    ),
                    correctAnswerIndex = 1,
                    explanation = "Куб имеет 48 элементов симметрии.",
                    points = 15,
                    timeLimit = 40
                ),
                Question(
                    id = UUID.randomUUID().toString(),
                    questionText = "Если рассечь конус плоскостью под разными углами, какие из этих фигур НЕ могут получиться?",
                    questionType = QuestionType.MULTIPLE_CHOICE,
                    options = listOf(
                        "Окружность",
                        "Эллипс",
                        "Парабола",
                        "Гипербола",
                        "Треугольник"
                    ),
                    correctAnswerIndex = 4,
                    explanation = "При сечении конуса плоскостью получаются только конические сечения: окружность, эллипс, парабола и гипербола. Треугольник получиться не может.",
                    points = 15,
                    timeLimit = 40
                ),
                Question(
                    id = UUID.randomUUID().toString(),
                    questionText = "Какой привычный предмет часто имеет форму усечённого икосаэдра?",
                    questionType = QuestionType.MULTIPLE_CHOICE,
                    options = listOf(
                        "Классический футбольный мяч",
                        "Игральный кубик",
                        "Хоккейная шайба",
                        "Бейсбольный мяч"
                    ),
                    correctAnswerIndex = 0,
                    explanation = "Классический футбольный мяч.",
                    points = 10,
                    timeLimit = 30
                ),
                Question(
                    id = UUID.randomUUID().toString(),
                    questionText = "Почему мыльные пузыри соединяются под углом 120°?",
                    questionType = QuestionType.MULTIPLE_CHOICE,
                    options = listOf(
                        "Это минимизирует поверхностное натяжение",
                        "Такова геометрия равновесия трех пленок",
                        "Из-за давления воздуха внутри",
                        "Особенность мыльного раствора"
                    ),
                    correctAnswerIndex = 1,
                    explanation = "Такова геометрия равновесия трех пленок.",
                    points = 10,
                    timeLimit = 30
                )
            ),
            stats = QuizStats(
                timesTaken = 0,
                averageScore = 0.0,
                totalAttempts = 0,
                completionRate = 0.0,
                ratingsCount = 0,
                averageRating = 0.0
            ),
            createdBy = "demo",
            createdAt = Timestamp.now(),
            timePerQuestion = 35
        )
    }

    private fun createHistoryQuiz(): Quiz {
        return Quiz(
            id = HISTORY_QUIZ_ID,
            title = "Исторические факты",
            description = "Увлекательные вопросы об исторических событиях, личностях и цивилизациях",
            categoryId = "История",
            difficulty = QuizDifficulty.MEDIUM,
            questions = listOf(
                Question(
                    id = UUID.randomUUID().toString(),
                    questionText = "В каком году человек впервые ступил на Луну?",
                    questionType = QuestionType.MULTIPLE_CHOICE,
                    options = listOf(
                        "1965",
                        "1969",
                        "1972",
                        "1975"
                    ),
                    correctAnswerIndex = 1,
                    explanation = "Первый человек ступил на Луну 20 июля 1969 года.",
                    points = 10,
                    timeLimit = 30
                ),
                Question(
                    id = UUID.randomUUID().toString(),
                    questionText = "Какая империя была самой большой в истории по территории?",
                    questionType = QuestionType.MULTIPLE_CHOICE,
                    options = listOf(
                        "Римская империя",
                        "Британская империя",
                        "Монгольская империя",
                        "Российская империя"
                    ),
                    correctAnswerIndex = 2,
                    explanation = "Монгольская империя была самой большой по территории.",
                    points = 10,
                    timeLimit = 30
                ),
                Question(
                    id = UUID.randomUUID().toString(),
                    questionText = "Кто открыл Америку?",
                    questionType = QuestionType.MULTIPLE_CHOICE,
                    options = listOf(
                        "Христофор Колумб",
                        "Васко да Гама",
                        "Фернан Магеллан",
                        "Америго Веспуччи"
                    ),
                    correctAnswerIndex = 0,
                    explanation = "Христофор Колумб открыл Америку в 1492 году.",
                    points = 10,
                    timeLimit = 30
                ),
                Question(
                    id = UUID.randomUUID().toString(),
                    questionText = "В каком веке началась Первая мировая война?",
                    questionType = QuestionType.MULTIPLE_CHOICE,
                    options = listOf(
                        "XIX век",
                        "XX век",
                        "XVIII век",
                        "XXI век"
                    ),
                    correctAnswerIndex = 1,
                    explanation = "Первая мировая война началась в XX веке, в 1914 году.",
                    points = 10,
                    timeLimit = 30
                ),
                Question(
                    id = UUID.randomUUID().toString(),
                    questionText = "Какой город был столицей Византийской империи?",
                    questionType = QuestionType.MULTIPLE_CHOICE,
                    options = listOf(
                        "Рим",
                        "Афины",
                        "Константинополь",
                        "Александрия"
                    ),
                    correctAnswerIndex = 2,
                    explanation = "Константинополь (современный Стамбул) был столицей Византийской империи.",
                    points = 10,
                    timeLimit = 30
                )
            ),
            stats = QuizStats(
                timesTaken = 0,
                averageScore = 0.0,
                totalAttempts = 0,
                completionRate = 0.0,
                ratingsCount = 0,
                averageRating = 0.0
            ),
            createdBy = "demo",
            createdAt = Timestamp.now(),
            timePerQuestion = 30
        )
    }

    private fun createGeographyQuiz(): Quiz {
        return Quiz(
            id = GEOGRAPHY_QUIZ_ID,
            title = "География мира",
            description = "Захватывающая викторина о странах, столицах, реках и горах нашей планеты",
            categoryId = "География",
            difficulty = QuizDifficulty.MEDIUM,
            questions = listOf(
                Question(
                    id = UUID.randomUUID().toString(),
                    questionText = "Какая самая длинная река в мире?",
                    questionType = QuestionType.MULTIPLE_CHOICE,
                    options = listOf(
                        "Амазонка",
                        "Нил",
                        "Янцзы",
                        "Миссисипи"
                    ),
                    correctAnswerIndex = 0,
                    explanation = "Амазонка — самая длинная река в мире.",
                    points = 10,
                    timeLimit = 30
                ),
                Question(
                    id = UUID.randomUUID().toString(),
                    questionText = "В какой стране находится самый высокий водопад в мире?",
                    questionType = QuestionType.MULTIPLE_CHOICE,
                    options = listOf(
                        "Бразилия",
                        "США",
                        "Венесуэла",
                        "Замбия"
                    ),
                    correctAnswerIndex = 2,
                    explanation = "Анхель, самый высокий водопад в мире, находится в Венесуэле.",
                    points = 10,
                    timeLimit = 30
                ),
                Question(
                    id = UUID.randomUUID().toString(),
                    questionText = "Какая самая большая страна по площади?",
                    questionType = QuestionType.MULTIPLE_CHOICE,
                    options = listOf(
                        "Канада",
                        "США",
                        "Китай",
                        "Россия"
                    ),
                    correctAnswerIndex = 3,
                    explanation = "Россия — самая большая страна по площади.",
                    points = 10,
                    timeLimit = 30
                ),
                Question(
                    id = UUID.randomUUID().toString(),
                    questionText = "Сколько океанов на Земле?",
                    questionType = QuestionType.MULTIPLE_CHOICE,
                    options = listOf(
                        "4",
                        "5",
                        "6",
                        "7"
                    ),
                    correctAnswerIndex = 1,
                    explanation = "На Земле 5 океанов: Тихий, Атлантический, Индийский, Южный и Северный Ледовитый.",
                    points = 10,
                    timeLimit = 30
                ),
                Question(
                    id = UUID.randomUUID().toString(),
                    questionText = "Как называется самая высокая гора в мире?",
                    questionType = QuestionType.MULTIPLE_CHOICE,
                    options = listOf(
                        "К2",
                        "Эверест",
                        "Макалу",
                        "Канченджанга"
                    ),
                    correctAnswerIndex = 1,
                    explanation = "Эверест (Джомолунгма) — самая высокая гора в мире.",
                    points = 10,
                    timeLimit = 30
                )
            ),
            stats = QuizStats(
                timesTaken = 0,
                averageScore = 0.0,
                totalAttempts = 0,
                completionRate = 0.0,
                ratingsCount = 0,
                averageRating = 0.0
            ),
            createdBy = "demo",
            createdAt = Timestamp.now(),
            timePerQuestion = 30
        )
    }

    // Функция для проверки состояния викторин
    fun checkQuizzesStatus() {
        scope.launch {
            try {
                val quizIds = listOf(
                    PLANT_QUIZ_ID,
                    FOOD_QUIZ_ID,
                    SCIENCE_QUIZ_ID,
                    GEOMETRY_QUIZ_ID,
                    HISTORY_QUIZ_ID,
                    GEOGRAPHY_QUIZ_ID
                )

                for (quizId in quizIds) {
                    val quiz = quizRepository.getQuiz(quizId).getOrNull()
                    if (quiz != null) {
                        Log.d("QuizDataSeeder", "✅ Викторина доступна: $quizId - ${quiz.title}")
                    } else {
                        Log.d("QuizDataSeeder", "❌ Викторина отсутствует: $quizId")
                    }
                }
            } catch (e: Exception) {
                Log.e("QuizDataSeeder", "Ошибка проверки статуса: ${e.message}")
            }
        }
    }

    // Функция для принудительного обновления викторин
    fun forceUpdateQuizzes() {
        scope.launch {
            try {
                val quizzes = createSampleQuizzes()
                for (quiz in quizzes) {
                    // Просто создаем/обновляем без проверки
                    quizRepository.createQuiz(quiz).fold(
                        onSuccess = {
                            Log.d("QuizDataSeeder", "🔄 Викторина обновлена: ${quiz.title}")
                        },
                        onFailure = { error ->
                            Log.e("QuizDataSeeder", "Ошибка обновления: ${error.message}")
                        }
                    )
                    kotlinx.coroutines.delay(100)
                }
            } catch (e: Exception) {
                Log.e("QuizDataSeeder", "Ошибка принудительного обновления: ${e.message}")
            }
        }
    }


    // Добавьте эти методы в конец QuizDataSeeder.kt

    fun getAllCategories(): List<String> {
        return listOf(
            "География",
            "История",
            "Математика",
            "Фильмы и музыка",
            "Наука",
            "Спорт"
        )
    }

    fun getPopularQuizzes(limit: Int): List<Quiz> {
        return createSampleQuizzes().take(limit)
    }

    fun getQuizzesByCategory(category: String): List<Quiz> {
        return createSampleQuizzes().filter { it.categoryId == category }
    }

    fun searchQuizzes(query: String): List<Quiz> {
        val lowerQuery = query.lowercase()
        return createSampleQuizzes().filter {
            it.title.lowercase().contains(lowerQuery) ||
                    it.categoryId.lowercase().contains(lowerQuery) ||
                    it.description.lowercase().contains(lowerQuery)
        }
    }

    // В QuizDataSeeder.kt добавьте метод для принудительного обновления
    fun forceUpdateQuizzes(onComplete: (Boolean) -> Unit = {}) {
        scope.launch {
            try {
                val quizzes = createSampleQuizzes()
                for (quiz in quizzes) {
                    quizRepository.createQuiz(quiz).fold(
                        onSuccess = {
                            Log.d("QuizDataSeeder", "🔄 Викторина обновлена: ${quiz.title}")
                        },
                        onFailure = { error ->
                            Log.e("QuizDataSeeder", "Ошибка обновления: ${error.message}")
                        }
                    )
                    kotlinx.coroutines.delay(200)
                }
                onComplete(true)
            } catch (e: Exception) {
                Log.e("QuizDataSeeder", "Ошибка принудительного обновления: ${e.message}")
                onComplete(false)
            }
        }
    }
}