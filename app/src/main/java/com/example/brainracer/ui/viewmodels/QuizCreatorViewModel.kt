package com.example.brainracer.ui.viewmodels

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.brainracer.data.repositories.QuizRepositoryImpl
import com.example.brainracer.data.repositories.UserRepositoryImpl
import com.example.brainracer.data.utils.ImageOptimizerUtil
import com.example.brainracer.data.utils.Result
import com.example.brainracer.ui.utils.ProfileAfterQuizRefresh
import com.example.brainracer.domain.entities.Question
import com.example.brainracer.domain.entities.QuestionType
import com.example.brainracer.domain.entities.Quiz
import com.example.brainracer.domain.entities.QuizDifficulty
import com.example.brainracer.domain.entities.QuizStats
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageException
import com.google.firebase.storage.StorageMetadata
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.UUID

// ── Модель черновика вопроса ──────────────────────────────────────────────

data class DraftQuestion(
    val id: String             = UUID.randomUUID().toString(),
    val text: String           = "",
    val options: List<String>  = listOf("", "", "", ""),
    val correctIndex: Int      = 0,
    val points: Int            = 10,
    val timeLimit: Int         = 30,
    val imageUri: Uri?         = null,     // локальный URI до загрузки
    val imageUrl: String?      = null,     // URL после загрузки в Storage
    val isGif: Boolean         = false
)

// ── Модель черновика викторины ────────────────────────────────────────────

data class QuizDraft(
    val id: String              = UUID.randomUUID().toString(),
    val title: String           = "",
    val description: String     = "",
    val categoryId: String      = "Кастомные",
    val difficulty: QuizDifficulty = QuizDifficulty.MEDIUM,
    val coverUri: Uri?          = null,
    val coverUrl: String?       = null,
    val timePerQuestion: Int    = 30,
    val questions: List<DraftQuestion> = listOf(DraftQuestion()),
    val updatedAt: Long         = System.currentTimeMillis()
)

// ── Шаблоны ───────────────────────────────────────────────────────────────

data class QuizTemplate(
    val id: String,
    val title: String,
    val description: String,
    val emoji: String,
    val categoryId: String,
    val difficulty: QuizDifficulty,
    val timePerQuestion: Int,
    val questionCount: Int,
    val defaultPoints: Int
)

val quizTemplates = listOf(
    QuizTemplate(
        id              = "tpl_blitz",
        title           = "Блиц-раунд",
        description     = "Быстрые вопросы с минимальным временем на ответ",
        emoji           = "⚡",
        categoryId      = "Кастомные",
        difficulty      = QuizDifficulty.MEDIUM,
        timePerQuestion = 10,
        questionCount   = 10,
        defaultPoints   = 15
    ),
    QuizTemplate(
        id              = "tpl_classic",
        title           = "Классика",
        description     = "Стандартный формат с удобным временем",
        emoji           = "📚",
        categoryId      = "Кастомные",
        difficulty      = QuizDifficulty.MEDIUM,
        timePerQuestion = 30,
        questionCount   = 10,
        defaultPoints   = 10
    ),
    QuizTemplate(
        id              = "tpl_expert",
        title           = "Для знатоков",
        description     = "Сложные вопросы и больше времени на размышление",
        emoji           = "🧠",
        categoryId      = "Кастомные",
        difficulty      = QuizDifficulty.HARD,
        timePerQuestion = 45,
        questionCount   = 8,
        defaultPoints   = 20
    ),
    QuizTemplate(
        id              = "tpl_kids",
        title           = "Для детей",
        description     = "Лёгкие вопросы с картинками и длинным таймером",
        emoji           = "🎈",
        categoryId      = "Кастомные",
        difficulty      = QuizDifficulty.EASY,
        timePerQuestion = 60,
        questionCount   = 8,
        defaultPoints   = 5
    ),
    QuizTemplate(
        id              = "tpl_speed",
        title           = "Скоростной",
        description     = "Максимум вопросов за минимум времени",
        emoji           = "🏎️",
        categoryId      = "Кастомные",
        difficulty      = QuizDifficulty.MEDIUM,
        timePerQuestion = 8,
        questionCount   = 15,
        defaultPoints   = 10
    ),
    QuizTemplate(
        id              = "tpl_trivia",
        title           = "Тривиа",
        description     = "Широкий кругозор на разные темы",
        emoji           = "🌍",
        categoryId      = "Кастомные",
        difficulty      = QuizDifficulty.MEDIUM,
        timePerQuestion = 25,
        questionCount   = 12,
        defaultPoints   = 10
    ),
)

// ── UI State ──────────────────────────────────────────────────────────────

data class QuizCreatorUiState(
    val currentDraft: QuizDraft       = QuizDraft(),
    val drafts: List<QuizDraft>       = emptyList(),
    val isLoading: Boolean            = false,
    val isSaving: Boolean             = false,
    val isPublishing: Boolean         = false,
    val publishSuccess: Boolean       = false,
    val error: String?                = null,
    val uploadingImageForQuestion: Int? = null,  // index вопроса, чья картинка грузится
    val uploadingCover: Boolean       = false
)

// ═════════════════════════════════════════════════════════════════════════════

class QuizCreatorViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(QuizCreatorUiState())
    val uiState: StateFlow<QuizCreatorUiState> = _uiState.asStateFlow()

    private val quizRepository = QuizRepositoryImpl()
    private val userRepository = UserRepositoryImpl()
    private val auth           = FirebaseAuth.getInstance()
    private val firestore      = FirebaseFirestore.getInstance()
    private val storage        = FirebaseStorage.getInstance()

    private val userId get() = auth.currentUser?.uid ?: ""

    private fun logStorageFailure(op: String, e: Exception) {
        if (e is StorageException) {
            Log.e("QuizCreator", "$op StorageException errorCode=${e.errorCode} message=${e.message}", e)
        } else {
            Log.e("QuizCreator", "$op failed: ${e.message}", e)
        }
    }

    init { loadDrafts() }

    // ── Редактирование мета-данных викторины ──────────────────────────────

    fun updateTitle(v: String)       = updateDraft { it.copy(title = v) }
    fun updateDescription(v: String) = updateDraft { it.copy(description = v) }
    fun updateCategory(v: String)    = updateDraft { it.copy(categoryId = v) }
    fun updateDifficulty(v: QuizDifficulty) = updateDraft { it.copy(difficulty = v) }
    fun updateTimePerQuestion(v: Int) = updateDraft { it.copy(timePerQuestion = v) }

    // ── Обложка ───────────────────────────────────────────────────────────

    fun setCoverUri(uri: Uri) = updateDraft { it.copy(coverUri = uri, coverUrl = null) }

    fun uploadCover(context: Context) {
        val uri = _uiState.value.currentDraft.coverUri ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(uploadingCover = true) }
            val uid = auth.currentUser?.uid
            if (uid.isNullOrBlank()) {
                _uiState.update {
                    it.copy(
                        uploadingCover = false,
                        error = "Войдите в аккаунт, чтобы загрузить обложку"
                    )
                }
                return@launch
            }
            try {
                val optimized = ImageOptimizerUtil.optimize(context, uri, isCover = true)
                val ext = optimized.mimeType.substringAfter('/').ifBlank { "jpeg" }
                val path = "quiz_covers/$uid/${UUID.randomUUID()}.$ext"
                val ref = storage.reference.child(path)
                val metadata = StorageMetadata.Builder()
                    .setContentType(optimized.mimeType)
                    .build()
                ref.putBytes(optimized.bytes, metadata).await()
                val url = ref.downloadUrl.await().toString()
                updateDraft { it.copy(coverUrl = url) }
                Log.d("Creator", "Cover uploaded: ${optimized.sizeKb}KB path=$path")
            } catch (e: Exception) {
                logStorageFailure("uploadCover", e)
                _uiState.update { it.copy(error = "Ошибка загрузки обложки: ${e.message}") }
            }
            _uiState.update { it.copy(uploadingCover = false) }
        }
    }

    // ── Работа с вопросами ────────────────────────────────────────────────

    fun addQuestion() = updateDraft { draft ->
        draft.copy(questions = draft.questions + DraftQuestion(
            timeLimit = draft.timePerQuestion
        ))
    }

    fun removeQuestion(index: Int) = updateDraft { draft ->
        if (draft.questions.size <= 1) return@updateDraft draft
        draft.copy(questions = draft.questions.toMutableList().also { it.removeAt(index) })
    }

    fun updateQuestionText(index: Int, text: String)  = updateQuestion(index) { it.copy(text = text) }
    fun updateOption(index: Int, optIndex: Int, v: String) = updateQuestion(index) { q ->
        val opts = q.options.toMutableList().also { it[optIndex] = v }
        q.copy(options = opts)
    }
    fun setCorrectAnswer(qIndex: Int, ansIndex: Int) = updateQuestion(qIndex) { it.copy(correctIndex = ansIndex) }
    fun updatePoints(index: Int, pts: Int)           = updateQuestion(index) { it.copy(points = pts) }
    fun updateTimeLimit(index: Int, sec: Int)        = updateQuestion(index) { it.copy(timeLimit = sec) }
    fun setQuestionImageUri(index: Int, uri: Uri)    = updateQuestion(index) { it.copy(imageUri = uri, imageUrl = null) }

    fun addOptionToQuestion(index: Int) = updateQuestion(index) { q ->
        if (q.options.size >= 6) return@updateQuestion q
        q.copy(options = q.options + "")
    }

    fun removeOptionFromQuestion(qIndex: Int, optIndex: Int) = updateQuestion(qIndex) { q ->
        if (q.options.size <= 2) return@updateQuestion q
        val newOpts = q.options.toMutableList().also { it.removeAt(optIndex) }
        val newCorrect = when {
            q.correctIndex == optIndex && optIndex >= newOpts.size -> newOpts.lastIndex
            q.correctIndex > optIndex -> q.correctIndex - 1
            else -> q.correctIndex
        }
        q.copy(options = newOpts, correctIndex = newCorrect)
    }

    fun uploadQuestionImage(context: Context, qIndex: Int) {
        val uri = _uiState.value.currentDraft.questions.getOrNull(qIndex)?.imageUri ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(uploadingImageForQuestion = qIndex) }
            val uid = auth.currentUser?.uid
            if (uid.isNullOrBlank()) {
                _uiState.update {
                    it.copy(
                        uploadingImageForQuestion = null,
                        error = "Войдите в аккаунт, чтобы загрузить изображение к вопросу"
                    )
                }
                return@launch
            }
            try {
                val optimized = ImageOptimizerUtil.optimize(context, uri, isCover = false)
                val ext = if (optimized.mimeType == "image/gif") "gif" else optimized.mimeType.substringAfter('/').ifBlank { "jpeg" }
                val path = "quiz_images/$uid/${UUID.randomUUID()}.$ext"
                val ref = storage.reference.child(path)
                val metadata = StorageMetadata.Builder()
                    .setContentType(optimized.mimeType)
                    .build()
                ref.putBytes(optimized.bytes, metadata).await()
                val url = ref.downloadUrl.await().toString()
                updateQuestion(qIndex) { it.copy(imageUrl = url, isGif = optimized.mimeType == "image/gif") }
                Log.d("Creator", "Q$qIndex image uploaded: ${optimized.sizeKb}KB path=$path")
            } catch (e: Exception) {
                logStorageFailure("uploadQuestionImage(q=$qIndex)", e)
                _uiState.update { it.copy(error = "Ошибка загрузки картинки: ${e.message}") }
            }
            _uiState.update { it.copy(uploadingImageForQuestion = null) }
        }
    }

    // ── Черновики ─────────────────────────────────────────────────────────

    fun saveDraft() {
        val draft = _uiState.value.currentDraft.copy(updatedAt = System.currentTimeMillis())
        _uiState.update { it.copy(isSaving = true) }
        viewModelScope.launch {
            try {
                val data = mapOf(
                    "id"          to draft.id,
                    "title"       to draft.title,
                    "description" to draft.description,
                    "categoryId"  to draft.categoryId,
                    "difficulty"  to draft.difficulty.name,
                    "coverUrl"    to draft.coverUrl,
                    "timePerQuestion" to draft.timePerQuestion,
                    "updatedAt"   to draft.updatedAt,
                    "questions"   to draft.questions.map { q ->
                        mapOf(
                            "id"           to q.id,
                            "text"         to q.text,
                            "options"      to q.options,
                            "correctIndex" to q.correctIndex,
                            "points"       to q.points,
                            "timeLimit"    to q.timeLimit,
                            "imageUrl"     to q.imageUrl,
                            "isGif"        to q.isGif
                        )
                    }
                )
                firestore.collection("users").document(userId)
                    .collection("drafts").document(draft.id)
                    .set(data).await()
                updateDraft { draft }
                loadDrafts()
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "Ошибка сохранения: ${e.message}") }
            }
            _uiState.update { it.copy(isSaving = false) }
        }
    }

    fun loadDraft(draft: QuizDraft) {
        _uiState.update { it.copy(currentDraft = draft) }
    }

    fun deleteDraft(draftId: String) {
        viewModelScope.launch {
            try {
                firestore.collection("users").document(userId)
                    .collection("drafts").document(draftId)
                    .delete().await()
                loadDrafts()
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "Ошибка удаления: ${e.message}") }
            }
        }
    }

    private fun loadDrafts() {
        if (userId.isBlank()) return
        viewModelScope.launch {
            try {
                val snap = firestore.collection("users").document(userId)
                    .collection("drafts")
                    .orderBy("updatedAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
                    .get().await()
                val drafts = snap.documents.mapNotNull { doc ->
                    try {
                        @Suppress("UNCHECKED_CAST")
                        val qs = (doc.get("questions") as? List<Map<String, Any>>) ?: emptyList()
                        QuizDraft(
                            id              = doc.getString("id") ?: doc.id,
                            title           = doc.getString("title") ?: "",
                            description     = doc.getString("description") ?: "",
                            categoryId      = doc.getString("categoryId") ?: "Кастомные",
                            difficulty      = try { QuizDifficulty.valueOf(doc.getString("difficulty") ?: "MEDIUM") } catch (_: Exception) { QuizDifficulty.MEDIUM },
                            coverUrl        = doc.getString("coverUrl"),
                            timePerQuestion = (doc.getLong("timePerQuestion") ?: 30).toInt(),
                            updatedAt       = doc.getLong("updatedAt") ?: 0L,
                            questions       = qs.map { q ->
                                @Suppress("UNCHECKED_CAST")
                                DraftQuestion(
                                    id           = q["id"] as? String ?: UUID.randomUUID().toString(),
                                    text         = q["text"] as? String ?: "",
                                    options      = (q["options"] as? List<String>) ?: listOf("", "", "", ""),
                                    correctIndex = (q["correctIndex"] as? Long)?.toInt() ?: 0,
                                    points       = (q["points"] as? Long)?.toInt() ?: 10,
                                    timeLimit    = (q["timeLimit"] as? Long)?.toInt() ?: 30,
                                    imageUrl     = q["imageUrl"] as? String,
                                    isGif        = q["isGif"] as? Boolean ?: false
                                )
                            }
                        )
                    } catch (_: Exception) { null }
                }
                _uiState.update { it.copy(drafts = drafts) }
            } catch (e: Exception) {
                Log.e("Creator", "loadDrafts error: ${e.message}")
            }
        }
    }

    // ── Применить шаблон ──────────────────────────────────────────────────

    fun applyTemplate(template: QuizTemplate) {
        val questions = List(template.questionCount) {
            DraftQuestion(
                points    = template.defaultPoints,
                timeLimit = template.timePerQuestion
            )
        }
        _uiState.update {
            it.copy(currentDraft = QuizDraft(
                categoryId      = template.categoryId,
                difficulty      = template.difficulty,
                timePerQuestion = template.timePerQuestion,
                questions       = questions
            ))
        }
    }

    // ── Публикация ────────────────────────────────────────────────────────

    fun publish(onSuccess: () -> Unit) {
        val draft = _uiState.value.currentDraft
        if (draft.title.isBlank()) {
            _uiState.update { it.copy(error = "Введите название викторины") }
            return
        }
        val invalidQ = draft.questions.indexOfFirst { q ->
            q.text.isBlank() || q.options.any { it.isBlank() }
        }
        if (invalidQ != -1) {
            _uiState.update { it.copy(error = "Заполните вопрос ${invalidQ + 1} и все варианты ответов") }
            return
        }

        _uiState.update { it.copy(isPublishing = true) }
        viewModelScope.launch {
            try {
                val quizId = "quiz_custom_${UUID.randomUUID().toString().take(8)}"
                val creatorNick = when (val u = userRepository.getUser(userId)) {
                    is Result.Success -> u.data.nickname.trim().takeIf { it.isNotBlank() }
                    else -> null
                } ?: auth.currentUser?.displayName?.trim()?.takeIf { it.isNotBlank() }.orEmpty()
                val quiz = Quiz(
                    id              = quizId,
                    title           = draft.title,
                    description     = draft.description,
                    categoryId      = draft.categoryId,
                    difficulty      = draft.difficulty,
                    isPublic        = true,
                    createdBy       = userId,
                    creatorNickname = creatorNick,
                    imageUrl        = draft.coverUrl ?: "",
                    timePerQuestion = draft.timePerQuestion,
                    createdAt       = Timestamp.now(),
                    stats           = QuizStats(),
                    questions       = draft.questions.mapIndexed { i, q ->
                        Question(
                            id                 = q.id,
                            questionText       = q.text,
                            questionType       = QuestionType.MULTIPLE_CHOICE,
                            options            = q.options.filter { it.isNotBlank() },
                            correctAnswerIndex = q.correctIndex,
                            points             = q.points,
                            timeLimit          = q.timeLimit,
                            imageUrl           = if (!q.isGif) q.imageUrl else null,
                            gifUrl             = if (q.isGif) q.imageUrl else null
                        )
                    }
                )
                when (val r = quizRepository.createQuiz(quiz)) {
                    is Result.Success -> {
                        // Удаляем черновик после публикации
                        try {
                            firestore.collection("users").document(userId)
                                .collection("drafts").document(draft.id).delete().await()
                        } catch (_: Exception) {}
                        ProfileAfterQuizRefresh.notify(userId)
                        _uiState.update { it.copy(isPublishing = false, publishSuccess = true, currentDraft = QuizDraft()) }
                        loadDrafts()
                        onSuccess()
                    }
                    is Result.Error -> {
                        _uiState.update { it.copy(isPublishing = false, error = "Ошибка публикации: ${r.exception.message}") }
                    }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isPublishing = false, error = "Ошибка: ${e.message}") }
            }
        }
    }

    // ── Новый черновик ────────────────────────────────────────────────────

    fun newDraft() {
        _uiState.update { it.copy(currentDraft = QuizDraft(), publishSuccess = false) }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    // ── Внутренние хелперы ────────────────────────────────────────────────

    private fun updateDraft(transform: (QuizDraft) -> QuizDraft) {
        _uiState.update { it.copy(currentDraft = transform(it.currentDraft)) }
    }

    private fun updateQuestion(index: Int, transform: (DraftQuestion) -> DraftQuestion) {
        updateDraft { draft ->
            val qs = draft.questions.toMutableList()
            if (index in qs.indices) qs[index] = transform(qs[index])
            draft.copy(questions = qs)
        }
    }
}