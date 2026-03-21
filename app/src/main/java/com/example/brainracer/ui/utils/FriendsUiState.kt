package com.example.brainracer.ui.utils

import androidx.compose.runtime.Stable
import com.example.brainracer.domain.entities.User

@Stable
data class FriendsUiState(
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val friends: List<User> = emptyList(),
    val incomingRequests: List<FriendRequestUi> = emptyList(),
    val outgoingRequests: List<OutgoingRequestUi> = emptyList(),
    val searchResults: List<User> = emptyList(),
    val searchQuery: String = "",
    val selectedTab: Int = 0,
    val isSearching: Boolean = false
)

@Stable
data class FriendRequestUi(
    val id: String,
    val senderId: String,
    val senderName: String,
    val senderAvatarUrl: String?,
    val createdAt: String
)

@Stable
data class OutgoingRequestUi(
    val id: String,
    val receiverId: String,
    val receiverName: String,
    val receiverAvatarUrl: String?,
    val createdAt: String
)