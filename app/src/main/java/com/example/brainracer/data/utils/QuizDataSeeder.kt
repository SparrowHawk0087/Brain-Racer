package com.example.brainracer.data.utils

import android.util.Log
import com.example.brainracer.data.repositories.QuizRepositoryImpl
import com.example.brainracer.domain.entities.Question
import com.example.brainracer.domain.entities.QuestionType
import com.example.brainracer.domain.entities.Quiz
import com.example.brainracer.domain.entities.QuizDifficulty
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.UUID

object QuizDataSeeder {

    // добавляем репозиторий и scope
    private val quizRepository = QuizRepositoryImpl()
    private val scope = CoroutineScope(Dispatchers.IO)

    // добавляем функцию seedQuizzes для заполнения базы данных
    fun seedQuizzes() {
        scope.launch {
            try {
                val quizzes = createSampleQuizzes()
                quizzes.forEach { quiz ->
                    quizRepository.createQuiz(quiz).fold(
                        onSuccess = {
                            Log.d("QuizDataSeeder", "Quiz created: ${quiz.title}")
                        },
                        onFailure = { error ->
                            Log.e("QuizDataSeeder", "Error creating quiz: ${error.message}")
                        }
                    )
                }
            } catch (e: Exception) {
                Log.e("QuizDataSeeder", "Error seeding quizzes: $e")
            }
        }
    }

    // добавляем вспомогательную функцию createSampleQuizzes для создания примеров
    private fun createSampleQuizzes(): List<Quiz> {
        return listOf(
            createPlantQuiz(),
            createFoodQuiz(),
            createScienceQuiz(),
            createGeometryQuiz()
        )
    }

    // добавляем функции для создания разных типов викторин
    private fun createPlantQuiz(): Quiz {
        return Quiz(
            id = UUID.randomUUID().toString(),
            title = "Мир растений",
            description = "Увлекательная викторина о самых необычных и удивительных растениях" +
                    " нашей планеты",
            category = "Биология",
            difficulty = QuizDifficulty.MEDIUM,
            tags = listOf("растения", "биология", "природа", "наука"),
            questions = listOf(
                Question(
                    id = UUID.randomUUID().toString(),
                    questionText = "Какое растение способно десятилетиями расти под землёй," +
                            " прежде чем показаться на поверхности?",
                    questionType = QuestionType.MULTIPLE_CHOICE,
                    options = listOf(
                        "Саговник",
                        "Рябчик",
                        "Подснежник",
                        "Орхидея-призрак"
                    ),
                    correctAnswerIndex = 3,
                    explanation = "Орхидея-призрак — Может жить как подземный сапрофит до 15 лет," +
                            " прежде чем сформирует цветонос.",
                    points = 10
            ),
                Question(
                    id = UUID.randomUUID().toString(),
                    questionText = "Какое растение способно выжить в пустыне, годами сохраняя влагу в своих листьях?",
                    questionType = QuestionType.MULTIPLE_CHOICE,
                    options = listOf(
                        "Алоэ вера",
                        "Толстянка",
                        "Вельвичия",
                        "Кактус"
                    ),
                    correctAnswerIndex = 2,
                    explanation = "Вельвичия — это растение пустыни Намиб может жить до 2000 лет, получая влагу из туманов.",
                    points = 10
                ),
                Question(
                    id = UUID.randomUUID().toString(),
                    questionText = "Какое растение умеет «охотиться» на насекомых, переваривая их с помощью специальных ферментов?",
                    questionType = QuestionType.MULTIPLE_CHOICE,
                    options = listOf(
                        "Росянка",
                        "Мимоза стыдливая",
                        "Комнатная герань",
                        "Папоротник"
                    ),
                    correctAnswerIndex = 0,
                    explanation = "Росянка — хищное растение, которое ловит насекомых липкими капельками на листьях.",
                    points = 10
                ),
                Question(
                    id = UUID.randomUUID().toString(),
                    questionText = "Какое дерево считается священным во многих культурах и может жить тысячи лет?",
                    questionType = QuestionType.MULTIPLE_CHOICE,
                    options = listOf(
                        "Дуб",
                        "Секвойя",
                        "Баобаб",
                        "Баньян"
                    ),
                    correctAnswerIndex = 3,
                    explanation = "Баньян — священное дерево в Индии, способное образовывать целые рощи благодаря воздушным корням.",
                    points = 10
                ),
                Question(
                    id = UUID.randomUUID().toString(),
                    questionText = "Какое растение известно своим ярким ароматом и используется в парфюмерии и кулинарии?",
                    questionType = QuestionType.MULTIPLE_CHOICE,
                    options = listOf(
                        "Лаванда",
                        "Жасмин",
                        "Роза",
                        "Все перечисленные"
                    ),
                    correctAnswerIndex = 3,
                    explanation = "Лаванда, жасмин и роза широко используются и в парфюмерии, и в кулинарии.",
                    points = 10
                )
            ),
            createdBy = "system",
            isPublic = true
        )
    }

    private fun createFoodQuiz(): Quiz {
        return Quiz(
            id = UUID.randomUUID().toString(),
            title = "Мир растений",
            description = "Увлекательная викторина о самых необычных и удивительных растениях нашей планеты",
            category = "Биология",
            difficulty = QuizDifficulty.MEDIUM,
            tags = listOf("растения", "биология", "природа", "наука"),
            questions = listOf(
                Question(
                    id = UUID.randomUUID().toString(),
                    questionText = "Какое растение способно десятилетиями расти под землёй, прежде чем показаться на поверхности?",
                    questionType = QuestionType.MULTIPLE_CHOICE,
                    options = listOf(
                        "Саговник",
                        "Рябчик",
                        "Подснежник",
                        "Орхидея-призрак"
                    ),
                    correctAnswerIndex = 3,
                    explanation = "Орхидея-призрак может жить как подземный сапрофит до 15 лет, прежде чем сформирует цветонос.",
                    points = 10
                ),
                Question(
                    id = UUID.randomUUID().toString(),
                    questionText = "Какое растение способно выжить в пустыне, годами сохраняя влагу в своих листьях?",
                    questionType = QuestionType.MULTIPLE_CHOICE,
                    options = listOf(
                        "Алоэ вера",
                        "Толстянка",
                        "Вельвичия",
                        "Кактус"
                    ),
                    correctAnswerIndex = 2,
                    explanation = "Вельвичия — это растение пустыни Намиб может жить до 2000 лет, получая влагу из туманов.",
                    points = 10
                ),
                Question(
                    id = UUID.randomUUID().toString(),
                    questionText = "Какое растение умеет «охотиться» на насекомых, переваривая их с помощью специальных ферментов?",
                    questionType = QuestionType.MULTIPLE_CHOICE,
                    options = listOf(
                        "Росянка",
                        "Мимоза стыдливая",
                        "Комнатная герань",
                        "Папоротник"
                    ),
                    correctAnswerIndex = 0,
                    explanation = "Росянка — хищное растение, которое ловит насекомых липкими капельками на листьях.",
                    points = 10
                ),
                Question(
                    id = UUID.randomUUID().toString(),
                    questionText = "Какое дерево считается священным во многих культурах и может жить тысячи лет?",
                    questionType = QuestionType.MULTIPLE_CHOICE,
                    options = listOf(
                        "Дуб",
                        "Секвойя",
                        "Баобаб",
                        "Баньян"
                    ),
                    correctAnswerIndex = 3,
                    explanation = "Баньян — священное дерево в Индии, способное образовывать целые рощи благодаря воздушным корням.",
                    points = 10
                ),
                Question(
                    id = UUID.randomUUID().toString(),
                    questionText = "Какое растение известно своим ярким ароматом и используется в парфюмерии и кулинарии?",
                    questionType = QuestionType.MULTIPLE_CHOICE,
                    options = listOf(
                        "Лаванда",
                        "Жасмин",
                        "Роза",
                        "Все перечисленные"
                    ),
                    correctAnswerIndex = 3,
                    explanation = "Лаванда, жасмин и роза широко используются и в парфюмерии, и в кулинарии.",
                    points = 10
                )
            ),
            createdBy = "system",
            isPublic = true
        )
    }

    private fun createScienceQuiz(): Quiz {
        return Quiz(
            id = UUID.randomUUID().toString(),
            title = "Наука: Физика и химия",
            description = "Интересные вопросы о законах физики, химических элементах и научных явлениях",
            category = "Наука",
            difficulty = QuizDifficulty.HARD,
            tags = listOf("физика", "химия", "наука", "элементы"),
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
                    points = 10
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
                    points = 10
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
                    points = 15
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
                    points = 15
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
                    points = 10
                )
            ),
            createdBy = "system",
            isPublic = true
        )
    }

    private fun createGeometryQuiz(): Quiz {
        return Quiz(
            id = UUID.randomUUID().toString(),
            title = "Геометрия",
            description = "Удивительные геометрические формы в природе, архитектуре и повседневных предметах",
            category = "Математика",
            difficulty = QuizDifficulty.MEDIUM,
            tags = listOf("геометрия", "математика", "фигуры", "симметрия"),
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
                        points = 10
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
                        points = 15
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
                        points = 15
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
                        points = 10
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
                        points = 10
                    )
            ),
                createdBy = "system",
                isPublic = true
        )
    }
}
