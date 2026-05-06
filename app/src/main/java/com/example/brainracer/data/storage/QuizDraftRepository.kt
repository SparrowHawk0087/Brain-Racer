package com.example.brainracer.data.storage

import com.example.brainracer.data.utils.Result
import com.example.brainracer.ui.viewmodels.QuizDraft

/**
 * Черновики викторин как JSON в приватном бакете Evolution Object Storage.
 */
interface QuizDraftRepository {

    suspend fun saveDraft(userId: String, draft: QuizDraft): Result<Unit>

    suspend fun loadDrafts(userId: String): Result<List<QuizDraft>>

    suspend fun loadDraft(userId: String, draftId: String): Result<QuizDraft>

    suspend fun deleteDraft(userId: String, draftId: String): Result<Unit>

    suspend fun deleteAllDrafts(userId: String): Result<Unit>
}
