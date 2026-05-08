package com.example.brainracer.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.brainracer.data.repositories.ChallengeRepositoryImpl
import com.example.brainracer.data.repositories.QuizRepositoryImpl
import com.example.brainracer.data.repositories.UserRepositoryImpl
import com.example.brainracer.data.utils.Result
import com.example.brainracer.data.utils.fold
import com.example.brainracer.domain.entities.FriendRequest
import com.example.brainracer.domain.entities.FriendshipStatus
import com.example.brainracer.domain.entities.User
import com.example.brainracer.ui.utils.FriendRequestUi
import com.example.brainracer.ui.utils.FriendsUiState
import com.example.brainracer.ui.utils.OutgoingRequestUi
import com.example.brainracer.ui.utils.toQuizItem
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class FriendsViewModel: ViewModel() {
    private val _uiState = MutableStateFlow(FriendsUiState())
    val uiState: StateFlow<FriendsUiState> = _uiState.asStateFlow()

    private val userRepository = UserRepositoryImpl()
    private val quizRepository = QuizRepositoryImpl()
    private val challengeRepository = ChallengeRepositoryImpl()
    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()
    private var activeViewedUserId: String? = null
    private var friendsLoadToken: Long = 0L

    init {
        loadFriends()
        loadFriendRequests()
    }

    fun loadForUser(viewedUserId: String) {
        val myId = auth.currentUser?.uid.orEmpty()
        val normalized = viewedUserId.ifBlank { myId }
        activeViewedUserId = normalized
        loadFriends(normalized)
        if (normalized == myId) {
            loadFriendRequests()
        } else {
            _uiState.update {
                it.copy(
                    incomingRequests = emptyList(),
                    outgoingRequests = emptyList()
                )
            }
        }
    }

    fun isOwnFriendsList(viewedUserId: String): Boolean {
        val myId = auth.currentUser?.uid.orEmpty()
        return viewedUserId.isBlank() || viewedUserId == myId
    }

    // Загружаем друзей
    fun loadFriends(targetUserId: String? = null) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            val userId = targetUserId?.takeIf { it.isNotBlank() }
                ?: activeViewedUserId?.takeIf { it.isNotBlank() }
                ?: auth.currentUser?.uid ?: run {
                    _uiState.update {
                        it.copy(isLoading = false, errorMessage = "Пользователь не авторизован")
                    }
                    return@launch
                }
            activeViewedUserId = userId
            val requestToken = ++friendsLoadToken

            try {
                val userDoc = firestore.collection("users").document(userId).get().await()
                val friendIds = userDoc.get("friends") as? List<String> ?: emptyList()
                if (!isLatestFriendsLoad(requestToken, userId)) return@launch

                if (friendIds.isEmpty()) {
                    _uiState.update { it.copy(isLoading = false, friends = emptyList()) }
                    return@launch
                }

                val friends = friendIds.mapNotNull { friendId ->
                    firestore.collection("users").document(friendId).get().await()
                        .toObject(User::class.java)?.copy(id = friendId)
                }
                if (!isLatestFriendsLoad(requestToken, userId)) return@launch

                _uiState.update {
                    it.copy(isLoading = false, friends = friends, errorMessage = null)
                }
            } catch (e: Exception) {
                if (!isLatestFriendsLoad(requestToken, userId)) return@launch
                _uiState.update {
                    it.copy(isLoading = false, errorMessage = "Ошибка загрузки друзей: ${e.message}")
                }
            }
        }
    }

    private fun isLatestFriendsLoad(token: Long, userId: String): Boolean {
        return token == friendsLoadToken && activeViewedUserId == userId
    }

    // Загружаем запросы
    fun loadFriendRequests() {
        viewModelScope.launch {
            val userId = auth.currentUser?.uid ?: return@launch

            try {
                // Входящие запросы
                // orderBy в Firebase не требует составного индекса.
                // Сортировка выполняется на клиенте через sortedByDescending
                val incomingSnapshot = firestore.collection("friend_requests")
                    .whereEqualTo("receiverId", userId)
                    .whereEqualTo("status", FriendshipStatus.PENDING.name)
                    // ← .orderBy(...) удалён
                    .get()
                    .await()

                val incomingRequests = incomingSnapshot.documents
                    .mapNotNull { doc ->
                        val request = doc.toObject(FriendRequest::class.java)
                            ?: return@mapNotNull null

                        val senderDoc = firestore.collection("users")
                            .document(request.senderId)
                            .get()
                            .await()
                        val sender = senderDoc.toObject(User::class.java)

                        FriendRequestUi(
                            id = doc.id,
                            senderId = request.senderId,
                            senderName = sender?.nickname ?: "Пользователь",
                            senderAvatarUrl = sender?.avatarUrl,
                            createdAt = request.createdAt.toString()
                        )
                    }
                    // Сортировка на клиенте: новые запросы отображаются первыми.
                    // Timestamp реализует Comparable, поэтому seconds доступен напрямую.
                    .sortedByDescending { it.createdAt }

                // Исходящие запросы
                // Та же логика: убираем orderBy, сортируем после маппинга.
                val outgoingSnapshot = firestore.collection("friend_requests")
                    .whereEqualTo("senderId", userId)
                    .whereEqualTo("status", FriendshipStatus.PENDING.name)
                    // ← .orderBy(...) удалён
                    .get()
                    .await()

                val outgoingRequests = outgoingSnapshot.documents
                    .mapNotNull { doc ->
                        val request = doc.toObject(FriendRequest::class.java)
                            ?: return@mapNotNull null

                        val receiverDoc = firestore.collection("users")
                            .document(request.receiverId)
                            .get()
                            .await()
                        val receiver = receiverDoc.toObject(User::class.java)

                        OutgoingRequestUi(
                            id = doc.id,
                            receiverId = request.receiverId,
                            receiverName = receiver?.nickname ?: "Пользователь",
                            receiverAvatarUrl = receiver?.avatarUrl,
                            createdAt = request.createdAt.toString()
                        )
                    }
                    // сортировка на клиенте
                    .sortedByDescending { it.createdAt }

                _uiState.update {
                    it.copy(
                        incomingRequests = incomingRequests,
                        outgoingRequests = outgoingRequests
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(errorMessage = "Ошибка загрузки запросов: ${e.message}")
                }
            }
        }
    }

    // Поиск пользователя
    fun searchUsers(query: String) {
        _uiState.update { it.copy(searchQuery = query, isSearching = query.isNotEmpty()) }

        if (query.isBlank()) {
            _uiState.update { it.copy(searchResults = emptyList(), isSearching = false) }
            return
        }

        viewModelScope.launch {
            try {
                val result = userRepository.searchUsers(query)
                result.fold(
                    onSuccess = { users ->
                        val currentUserId = auth.currentUser?.uid
                        val filtered = users.filter { it.id != currentUserId }
                        _uiState.update { it.copy(searchResults = filtered) }
                    },
                    onFailure = { error ->
                        _uiState.update { it.copy(errorMessage = error.message) }
                    }
                )
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = e.message) }
            }
        }
    }

    // Отправка запроса в друзья
    fun sendFriendRequest(receiverId: String) {
        viewModelScope.launch {
            val senderId = auth.currentUser?.uid ?: return@launch

            try {
                val senderDoc = firestore.collection("users").document(senderId).get().await()
                val friendIds = senderDoc.get("friends") as? List<String> ?: emptyList()

                if (friendIds.contains(receiverId)) {
                    _uiState.update { it.copy(errorMessage = "Пользователь уже в друзьях") }
                    return@launch
                }

                val existingRequest = firestore.collection("friend_requests")
                    .whereEqualTo("senderId", senderId)
                    .whereEqualTo("receiverId", receiverId)
                    .whereEqualTo("status", FriendshipStatus.PENDING.name)
                    .get()
                    .await()

                if (!existingRequest.isEmpty) {
                    _uiState.update { it.copy(errorMessage = "Запрос уже отправлен") }
                    return@launch
                }

                val requestRef = firestore.collection("friend_requests").document()
                val request = FriendRequest(
                    id = requestRef.id,
                    senderId = senderId,
                    receiverId = receiverId,
                    status = FriendshipStatus.PENDING,
                    createdAt = com.google.firebase.Timestamp.now(),
                    updatedAt = com.google.firebase.Timestamp.now()
                )

                requestRef.set(request).await()
                _uiState.update { it.copy(errorMessage = null) }
                loadFriendRequests()
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = "Ошибка: ${e.message}") }
            }
        }
    }

    // Принятие запроса
    fun acceptFriendRequest(requestId: String, senderId: String) {
        viewModelScope.launch {
            val receiverId = auth.currentUser?.uid ?: return@launch

            try {
                firestore.runTransaction { transaction ->
                    val requestRef = firestore.collection("friend_requests").document(requestId)
                    val senderRef  = firestore.collection("users").document(senderId)
                    val receiverRef = firestore.collection("users").document(receiverId)

                    transaction.update(requestRef, "status", FriendshipStatus.ACCEPTED.name)
                    transaction.update(senderRef,  "friends", com.google.firebase.firestore.FieldValue.arrayUnion(receiverId))
                    transaction.update(receiverRef, "friends", com.google.firebase.firestore.FieldValue.arrayUnion(senderId))

                    null
                }.await()

                loadFriends()
                loadFriendRequests()
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = "Ошибка: ${e.message}") }
            }
        }
    }

    // Отклонение запроса
    fun declineFriendRequest(requestId: String) {
        viewModelScope.launch {
            try {
                firestore.collection("friend_requests")
                    .document(requestId)
                    .update("status", FriendshipStatus.BLOCKED.name)
                    .await()
                loadFriendRequests()
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = "Ошибка: ${e.message}") }
            }
        }
    }

    // Отмена исходящего запроса в друзья
    fun cancelOutgoingRequest(requestId: String) {
        viewModelScope.launch {
            try {
                firestore.collection("friend_requests")
                    .document(requestId)
                    .delete()
                    .await()
                loadFriendRequests()
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = "Ошибка: ${e.message}") }
            }
        }
    }

    // Удаление друга
    fun removeFriend(friendId: String) {
        viewModelScope.launch {
            val userId = auth.currentUser?.uid ?: return@launch

            try {
                firestore.runTransaction { transaction ->
                    val userRef   = firestore.collection("users").document(userId)
                    val friendRef = firestore.collection("users").document(friendId)

                    transaction.update(userRef,   "friends", com.google.firebase.firestore.FieldValue.arrayRemove(friendId))
                    transaction.update(friendRef, "friends", com.google.firebase.firestore.FieldValue.arrayRemove(userId))

                    null
                }.await()
                loadFriends()
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = "Ошибка: ${e.message}") }
            }
        }
    }

    fun setPreselectedChallengeQuiz(quizId: String?) {
        if (quizId.isNullOrBlank()) {
            _uiState.update {
                it.copy(
                    preselectChallengeQuizId = null,
                    preselectChallengeQuizTitle = null,
                    preselectChallengeQuizLoading = false
                )
            }
            return
        }
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    preselectChallengeQuizId = quizId,
                    preselectChallengeQuizTitle = null,
                    preselectChallengeQuizLoading = true
                )
            }
            when (val r = quizRepository.getQuiz(quizId)) {
                is Result.Success ->
                    _uiState.update {
                        it.copy(
                            preselectChallengeQuizTitle = r.data.title,
                            preselectChallengeQuizLoading = false
                        )
                    }
                is Result.Error ->
                    _uiState.update {
                        it.copy(
                            preselectChallengeQuizTitle = "Викторина",
                            preselectChallengeQuizLoading = false
                        )
                    }
            }
        }
    }

    fun loadChallengePickerQuizzes() {
        viewModelScope.launch {
            _uiState.update { it.copy(challengePickerLoading = true) }
            when (val r = quizRepository.getPopularQuizzes(limit = 80)) {
                is Result.Success -> {
                    val items = r.data.map { it.toQuizItem() }
                    _uiState.update {
                        it.copy(challengePickerQuizzes = items, challengePickerLoading = false)
                    }
                }
                is Result.Error -> {
                    _uiState.update {
                        it.copy(
                            challengePickerLoading = false,
                            errorMessage           = "Не удалось загрузить викторины: ${r.exception.message}"
                        )
                    }
                }
            }
        }
    }

    // Вызов другу
    fun sendChallenge(friendId: String, quizId: String, quizTitle: String) {
        viewModelScope.launch {
            val challengerId = auth.currentUser?.uid ?: return@launch

            try {
                val challenge = com.example.brainracer.domain.entities.Challenge(
                    quizId = quizId,
                    quizTitle = quizTitle,
                    challengerUserId = challengerId,
                    challengedUserId = friendId,
                    status = com.example.brainracer.domain.entities.ChallengeStatus.PENDING
                )

                challengeRepository.createChallenge(challenge).fold(
                    onSuccess = {
                        _uiState.update {
                            it.copy(
                                errorMessage         = null,
                                challengeSentMessage = "Вызов отправлен"
                            )
                        }
                    },
                    onFailure = { error ->
                        _uiState.update { it.copy(errorMessage = error.message) }
                    }
                )
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = "Ошибка: ${e.message}") }
            }
        }
    }

    fun consumeChallengeSentMessage() {
        _uiState.update { it.copy(challengeSentMessage = null) }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    fun updateSelectedTab(tabIndex: Int) {
        _uiState.update { it.copy(selectedTab = tabIndex) }
    }

    fun clearSearch() {
        _uiState.update { it.copy(searchQuery = "", searchResults = emptyList(), isSearching = false) }
    }
}