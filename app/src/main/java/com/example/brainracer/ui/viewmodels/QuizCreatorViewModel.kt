package com.example.brainracer.ui.viewmodels

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.brainracer.data.repositories.QuizRepositoryImpl
import com.example.brainracer.data.repositories.UserRepositoryImpl
import com.example.brainracer.data.storage.EvolutionStorageRepositoryImpl
import com.example.brainracer.data.storage.QuizDraftRepositoryImpl
import com.example.brainracer.data.storage.StorageConfig
import com.example.brainracer.data.utils.ImageOptimizerUtil
import com.example.brainracer.data.utils.Result
import com.example.brainracer.ui.utils.ProfileAfterQuizRefresh
import com.example.brainracer.ui.utils.ModerationResult
import com.example.brainracer.ui.utils.QuizModeration
import com.example.brainracer.domain.entities.Question
import com.example.brainracer.domain.entities.QuestionType
import com.example.brainracer.domain.entities.Quiz
import com.example.brainracer.domain.entities.QuizDifficulty
import com.example.brainracer.domain.entities.QuizStats
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
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
    val isGif: Boolean         = false,
    val explanation: String    = ""        // подсказка/объяснение для экрана результатов
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
    val editingQuizId: String?        = null,
    val isLoading: Boolean            = false,
    val isSaving: Boolean             = false,
    val isPublishing: Boolean         = false,
    val publishSuccess: Boolean       = false,
    val error: String?                = null,
    val uploadingImageForQuestion: Int? = null,  // index вопроса, чья картинка грузится
    val uploadingCover: Boolean       = false,
    /** URL'ы старых картинок, которые надо удалить из bucket после успешной перезаписи/публикации. */
    val pendingDeletionUrls: List<String> = emptyList()
)

// ═════════════════════════════════════════════════════════════════════════════

class QuizCreatorViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(QuizCreatorUiState())
    val uiState: StateFlow<QuizCreatorUiState> = _uiState.asStateFlow()

    private val quizRepository = QuizRepositoryImpl()
    private val userRepository = UserRepositoryImpl()
    private val auth = FirebaseAuth.getInstance()
    private val storageRepository = EvolutionStorageRepositoryImpl()
    private val draftRepository = QuizDraftRepositoryImpl(storageRepository)

    private val userId get() = auth.currentUser?.uid ?: ""

    init { loadDrafts() }

    fun loadQuizForEdit(quizId: String) {
        if (quizId.isBlank()) return
        if (_uiState.value.editingQuizId == quizId) return
        _uiState.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            when (val res = quizRepository.getQuiz(quizId)) {
                is Result.Success -> {
                    val quiz = res.data
                    if (quiz.createdBy != userId) {
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                error = "Редактировать может только автор викторины"
                            )
                        }
                        return@launch
                    }
                    val draft = QuizDraft(
                        id = quiz.id,
                        title = quiz.title,
                        description = quiz.description,
                        categoryId = quiz.categoryId,
                        difficulty = quiz.difficulty,
                        coverUrl = quiz.imageUrl.takeIf { it.isNotBlank() },
                        timePerQuestion = quiz.timePerQuestion,
                        questions = quiz.questions.map { q ->
                            DraftQuestion(
                                id = q.id.ifBlank { UUID.randomUUID().toString() },
                                text = q.questionText,
                                options = if (q.options.size >= 2) q.options else listOf("", "", "", ""),
                                correctIndex = q.correctAnswerIndex.coerceIn(0, (q.options.lastIndex).coerceAtLeast(0)),
                                points = q.points,
                                timeLimit = q.timeLimit,
                                imageUrl = q.imageUrl ?: q.gifUrl,
                                isGif = !q.gifUrl.isNullOrBlank(),
                                explanation = q.explanation.orEmpty()
                            )
                        }.ifEmpty { listOf(DraftQuestion(timeLimit = quiz.timePerQuestion)) }
                    )
                    _uiState.update {
                        it.copy(
                            currentDraft = draft,
                            editingQuizId = quiz.id,
                            isLoading = false,
                            publishSuccess = false
                        )
                    }
                }
                is Result.Error -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = "Не удалось загрузить викторину для редактирования: ${res.exception.message}"
                        )
                    }
                }
            }
        }
    }

    // ── Редактирование мета-данных викторины ──────────────────────────────

    fun updateTitle(v: String)       = updateDraft { it.copy(title = v) }
    fun updateDescription(v: String) = updateDraft { it.copy(description = v) }
    fun updateCategory(v: String)    = updateDraft { it.copy(categoryId = v) }
    fun updateDifficulty(v: QuizDifficulty) = updateDraft { it.copy(difficulty = v) }
    fun updateTimePerQuestion(v: Int) = updateDraft { it.copy(timePerQuestion = v) }

    // ── Обложка ───────────────────────────────────────────────────────────

    fun setCoverUri(uri: Uri) {
        // Запоминаем старую обложку: её удалим после успешной загрузки новой
        // (как с фотографиями профиля — старые файлы не остаются в bucket).
        val prevUrl = _uiState.value.currentDraft.coverUrl
        if (!prevUrl.isNullOrBlank()) {
            _uiState.update { it.copy(pendingDeletionUrls = it.pendingDeletionUrls + prevUrl) }
        }
        updateDraft { it.copy(coverUri = uri, coverUrl = null) }
    }

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
                val key = StorageConfig.quizCoverKey(uid, UUID.randomUUID().toString(), ext)
                when (val uploadResult = storageRepository.upload(
                    bucket = StorageConfig.BUCKET_QUIZZES,
                    key = key,
                    bytes = optimized.bytes,
                    mimeType = optimized.mimeType,
                    isPublic = true
                )) {
                    is Result.Success -> {
                        updateDraft { it.copy(coverUrl = uploadResult.data) }
                        Log.d(TAG, "Cover uploaded: ${optimized.sizeKb}KB → $key")
                        flushPendingDeletions()
                    }
                    is Result.Error -> {
                        _uiState.update {
                            it.copy(error = "Ошибка загрузки обложки: ${uploadResult.exception.message}")
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "uploadCover failed", e)
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

    fun removeQuestion(index: Int) {
        val toRemoveUrl = _uiState.value.currentDraft.questions.getOrNull(index)?.imageUrl
        if (!toRemoveUrl.isNullOrBlank()) {
            _uiState.update { it.copy(pendingDeletionUrls = it.pendingDeletionUrls + toRemoveUrl) }
            viewModelScope.launch { flushPendingDeletions() }
        }
        updateDraft { draft ->
            if (draft.questions.size <= 1) return@updateDraft draft
            draft.copy(questions = draft.questions.toMutableList().also { it.removeAt(index) })
        }
    }

    /** Перетаскивание: вставляет вопрос в позицию [toIndex] после удаления с [fromIndex]. */
    fun moveQuestion(fromIndex: Int, toIndex: Int) = updateDraft { draft ->
        val qs = draft.questions.toMutableList()
        if (fromIndex !in qs.indices) return@updateDraft draft
        val bounded = toIndex.coerceIn(0, qs.lastIndex)
        if (bounded == fromIndex) return@updateDraft draft
        val item = qs.removeAt(fromIndex)
        qs.add(bounded.coerceIn(0, qs.size), item)
        draft.copy(questions = qs)
    }

    fun updateQuestionText(index: Int, text: String)  = updateQuestion(index) { it.copy(text = text) }
    fun updateQuestionExplanation(index: Int, text: String) = updateQuestion(index) { it.copy(explanation = text) }
    fun updateOption(index: Int, optIndex: Int, v: String) = updateQuestion(index) { q ->
        val opts = q.options.toMutableList().also { it[optIndex] = v }
        q.copy(options = opts)
    }
    fun setCorrectAnswer(qIndex: Int, ansIndex: Int) = updateQuestion(qIndex) { it.copy(correctIndex = ansIndex) }
    fun updatePoints(index: Int, pts: Int)           = updateQuestion(index) { it.copy(points = pts) }
    fun updateTimeLimit(index: Int, sec: Int)        = updateQuestion(index) { it.copy(timeLimit = sec) }
    fun setQuestionImageUri(index: Int, uri: Uri) {
        // Запоминаем старую картинку вопроса для удаления из bucket после успешной перезаписи.
        val prevUrl = _uiState.value.currentDraft.questions.getOrNull(index)?.imageUrl
        if (!prevUrl.isNullOrBlank()) {
            _uiState.update { it.copy(pendingDeletionUrls = it.pendingDeletionUrls + prevUrl) }
        }
        updateQuestion(index) { it.copy(imageUri = uri, imageUrl = null) }
    }

    /** Удалить картинку вопроса (и из bucket'а тоже). */
    fun clearQuestionImage(index: Int) {
        val prevUrl = _uiState.value.currentDraft.questions.getOrNull(index)?.imageUrl
        if (!prevUrl.isNullOrBlank()) {
            _uiState.update { it.copy(pendingDeletionUrls = it.pendingDeletionUrls + prevUrl) }
            // Удаляем сразу — UI уже не ссылается на этот URL.
            viewModelScope.launch { flushPendingDeletions() }
        }
        updateQuestion(index) { it.copy(imageUri = null, imageUrl = null, isGif = false) }
    }

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
                val ext = if (optimized.mimeType == "image/gif") "gif"
                else optimized.mimeType.substringAfter('/').ifBlank { "jpeg" }
                val key = StorageConfig.questionImageKey(uid, UUID.randomUUID().toString(), ext)
                when (val uploadResult = storageRepository.upload(
                    bucket = StorageConfig.BUCKET_QUIZZES,
                    key = key,
                    bytes = optimized.bytes,
                    mimeType = optimized.mimeType,
                    isPublic = true
                )) {
                    is Result.Success -> {
                        val isGif = optimized.mimeType == "image/gif"
                        updateQuestion(qIndex) {
                            it.copy(imageUrl = uploadResult.data, isGif = isGif)
                        }
                        Log.d(TAG, "Q$qIndex image uploaded: ${optimized.sizeKb}KB → $key")
                        flushPendingDeletions()
                    }
                    is Result.Error -> {
                        _uiState.update {
                            it.copy(error = "Ошибка загрузки картинки: ${uploadResult.exception.message}")
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "uploadQuestionImage(q=$qIndex) failed", e)
                _uiState.update { it.copy(error = "Ошибка загрузки картинки: ${e.message}") }
            }
            _uiState.update { it.copy(uploadingImageForQuestion = null) }
        }
    }

    // ── Черновики ─────────────────────────────────────────────────────────

    fun saveDraft() {
        val uid = userId
        if (uid.isBlank()) {
            _uiState.update { it.copy(error = "Войдите в аккаунт, чтобы сохранить черновик") }
            return
        }
        val draft = _uiState.value.currentDraft.copy(updatedAt = System.currentTimeMillis())
        _uiState.update { it.copy(isSaving = true) }
        viewModelScope.launch {
            when (val result = draftRepository.saveDraft(uid, draft)) {
                is Result.Success -> {
                    updateDraft { draft }
                    loadDrafts()
                    Log.d(TAG, "Draft saved: ${draft.id}")
                }
                is Result.Error -> {
                    _uiState.update {
                        it.copy(error = "Ошибка сохранения черновика: ${result.exception.message}")
                    }
                }
            }
            _uiState.update { it.copy(isSaving = false) }
        }
    }

    fun loadDraft(draft: QuizDraft) {
        _uiState.update { it.copy(currentDraft = draft) }
    }

    fun deleteDraft(draftId: String) {
        val uid = userId
        if (uid.isBlank()) return
        viewModelScope.launch {
            when (val result = draftRepository.deleteDraft(uid, draftId)) {
                is Result.Success -> loadDrafts()
                is Result.Error -> _uiState.update {
                    it.copy(error = "Ошибка удаления черновика: ${result.exception.message}")
                }
            }
        }
    }

    private fun loadDrafts() {
        val uid = userId
        if (uid.isBlank()) return
        viewModelScope.launch {
            when (val result = draftRepository.loadDrafts(uid)) {
                is Result.Success -> _uiState.update { it.copy(drafts = result.data) }
                is Result.Error -> Log.e(TAG, "loadDrafts error: ${result.exception.message}")
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
            ), editingQuizId = null)
        }
    }

    // ── Публикация ────────────────────────────────────────────────────────

    /**
     * Полная авто-модерация выполняется при КАЖДОЙ попытке публикации, в том числе
     * для черновиков, загруженных из раздела «Черновики». Так как правила модерации
     * (см. [QuizModeration]) могут обновляться, прохождение модерации в момент сохранения
     * черновика не гарантирует прохождение в момент публикации — поэтому проверка всегда
     * запускается заново на актуальном [QuizDraft] из стейта.
     */
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
        when (val moderation = QuizModeration.validateQuizDraft(draft)) {
            is ModerationResult.Blocked -> {
                _uiState.update {
                    it.copy(
                        error = "Публикация заблокирована: [${moderation.violation.category.localizedLabel()}] " +
                                "запрещённый контент в ${moderation.violation.location} (${moderation.violation.reason})"
                    )
                }
                return
            }
            ModerationResult.Allowed -> Unit
        }

        _uiState.update { it.copy(isPublishing = true) }
        viewModelScope.launch {
            try {
                val editingId = _uiState.value.editingQuizId
                val existingQuiz = if (!editingId.isNullOrBlank()) {
                    when (val q = quizRepository.getQuiz(editingId)) {
                        is Result.Success -> q.data
                        is Result.Error -> null
                    }
                } else null
                val quizId = existingQuiz?.id ?: "quiz_custom_${UUID.randomUUID().toString().take(8)}"
                val creatorNick = when (val u = userRepository.getUser(userId)) {
                    is Result.Success -> u.data.nickname.trim().takeIf { it.isNotBlank() }
                    else -> null
                } ?: existingQuiz?.creatorNickname ?: auth.currentUser?.displayName?.trim()?.takeIf { it.isNotBlank() }.orEmpty()
                val quiz = Quiz(
                    id              = quizId,
                    title           = draft.title,
                    description     = draft.description,
                    categoryId      = draft.categoryId,
                    difficulty      = draft.difficulty,
                    isPublic        = existingQuiz?.isPublic ?: true,
                    createdBy       = existingQuiz?.createdBy ?: userId,
                    creatorNickname = creatorNick,
                    imageUrl        = draft.coverUrl ?: "",
                    timePerQuestion = draft.timePerQuestion,
                    createdAt       = existingQuiz?.createdAt ?: Timestamp.now(),
                    stats           = existingQuiz?.stats ?: QuizStats(),
                    questions       = draft.questions.mapIndexed { _, q ->
                        Question(
                            id                 = q.id,
                            questionText       = q.text,
                            questionType       = QuestionType.MULTIPLE_CHOICE,
                            options            = q.options.filter { it.isNotBlank() },
                            correctAnswerIndex = q.correctIndex,
                            points             = q.points,
                            timeLimit          = q.timeLimit,
                            imageUrl           = if (!q.isGif) q.imageUrl else null,
                            gifUrl             = if (q.isGif) q.imageUrl else null,
                            explanation        = q.explanation.trim().ifBlank { null }
                        )
                    }
                )

                // Удаляем картинки/обложку, которые остались от прошлой версии и больше не используются.
                if (existingQuiz != null) {
                    val newCover = draft.coverUrl
                    val oldCover = existingQuiz.imageUrl.ifBlank { null }
                    if (!oldCover.isNullOrBlank() && oldCover != newCover) {
                        _uiState.update { it.copy(pendingDeletionUrls = it.pendingDeletionUrls + oldCover) }
                    }
                    val newImageUrls = draft.questions.mapNotNull { it.imageUrl }.toSet()
                    existingQuiz.questions.forEach { oldQ ->
                        val oldImg = (oldQ.imageUrl ?: oldQ.gifUrl).orEmpty()
                        if (oldImg.isNotBlank() && oldImg !in newImageUrls) {
                            _uiState.update { it.copy(pendingDeletionUrls = it.pendingDeletionUrls + oldImg) }
                        }
                    }
                }

                when (val r = if (existingQuiz != null) quizRepository.updateQuiz(quiz) else quizRepository.createQuiz(quiz)) {
                    is Result.Success -> {
                        draftRepository.deleteDraft(userId, draft.id)
                        ProfileAfterQuizRefresh.notify(userId)
                        flushPendingDeletions()
                        _uiState.update {
                            it.copy(
                                isPublishing = false,
                                publishSuccess = true,
                                currentDraft = QuizDraft(),
                                editingQuizId = null
                            )
                        }
                        loadDrafts()
                        onSuccess()
                    }
                    is Result.Error -> {
                        _uiState.update {
                            it.copy(
                                isPublishing = false,
                                error = "Ошибка ${if (existingQuiz != null) "обновления" else "публикации"}: ${r.exception.message}"
                            )
                        }
                    }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isPublishing = false, error = "Ошибка: ${e.message}") }
            }
        }
    }

    // ── Новый черновик ────────────────────────────────────────────────────

    fun newDraft() {
        _uiState.update { it.copy(currentDraft = QuizDraft(), publishSuccess = false, editingQuizId = null) }
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

    /**
     * Удаляет накопленные «осиротевшие» URL'ы из bucket'а (старые обложки/картинки
     * вопросов после перезаписи или удаления). По аналогии с фотографиями профиля —
     * лишних файлов не остаётся.
     */
    private suspend fun flushPendingDeletions() {
        val urls = _uiState.value.pendingDeletionUrls
        if (urls.isEmpty()) return
        _uiState.update { it.copy(pendingDeletionUrls = emptyList()) }
        urls.distinct().forEach { url -> deleteStorageObjectByUrl(url) }
    }

    private suspend fun deleteStorageObjectByUrl(url: String?) {
        if (url.isNullOrBlank()) return
        val key = StorageConfig.extractObjectKeyPublic(url) ?: return
        val bucket = StorageConfig.bucketForKeyPublic(key) ?: return
        when (val res = storageRepository.delete(bucket, key)) {
            is Result.Success -> Log.d(TAG, "Deleted stale object: $bucket/$key")
            is Result.Error -> Log.w(TAG, "Failed to delete $bucket/$key: ${res.exception.message}")
        }
    }

    companion object {
        private const val TAG = "QuizCreatorViewModel"
    }
}