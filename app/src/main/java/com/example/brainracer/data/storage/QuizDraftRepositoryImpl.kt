package com.example.brainracer.data.storage

import android.util.Log
import com.example.brainracer.data.utils.Result
import com.example.brainracer.domain.entities.QuizDifficulty
import com.example.brainracer.ui.viewmodels.DraftQuestion
import com.example.brainracer.ui.viewmodels.QuizDraft
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import java.util.UUID

/**
 * Черновики в бакете [StorageConfig.BUCKET_DRAFTS], ключ `{userId}/{draftId}.json`.
 */
class QuizDraftRepositoryImpl(
    private val storageRepository: EvolutionStorageRepository = EvolutionStorageRepositoryImpl()
) : QuizDraftRepository {

    private val gson: Gson = GsonBuilder().serializeNulls().create()
    private val bucket = StorageConfig.BUCKET_DRAFTS

    override suspend fun saveDraft(userId: String, draft: QuizDraft): Result<Unit> {
        return try {
            val json = serializeDraft(draft)
            val bytes = json.toByteArray(Charsets.UTF_8)
            val key = StorageConfig.draftKey(userId, draft.id)
            when (val uploadResult = storageRepository.upload(
                bucket = bucket,
                key = key,
                bytes = bytes,
                mimeType = "application/json",
                isPublic = false
            )) {
                is Result.Success -> {
                    Log.d(TAG, "Draft saved: $key (${bytes.size / 1024}KB)")
                    Result.success(Unit)
                }
                is Result.Error -> uploadResult
            }
        } catch (e: Exception) {
            Log.e(TAG, "saveDraft(${draft.id}) failed", e)
            Result.error(e)
        }
    }

    override suspend fun loadDrafts(userId: String): Result<List<QuizDraft>> {
        return try {
            val prefix = "$userId/"
            val keysResult = storageRepository.listKeys(bucket, prefix)
            if (keysResult is Result.Error) return keysResult

            val keys = (keysResult as Result.Success).data.filter { it.endsWith(".json") }

            val drafts = keys.mapNotNull { key ->
                when (val dl = storageRepository.download(bucket, key)) {
                    is Result.Success -> {
                        try {
                            deserializeDraft(String(dl.data, Charsets.UTF_8))
                        } catch (parseEx: Exception) {
                            Log.w(TAG, "Failed to parse draft $key: ${parseEx.message}")
                            null
                        }
                    }
                    is Result.Error -> {
                        Log.w(TAG, "Failed to download draft $key: ${dl.exception.message}")
                        null
                    }
                }
            }.sortedByDescending { it.updatedAt }

            Result.success(drafts)
        } catch (e: Exception) {
            Log.e(TAG, "loadDrafts($userId) failed", e)
            Result.error(e)
        }
    }

    override suspend fun loadDraft(userId: String, draftId: String): Result<QuizDraft> {
        return try {
            val key = StorageConfig.draftKey(userId, draftId)
            when (val dl = storageRepository.download(bucket, key)) {
                is Result.Success -> {
                    val draft = deserializeDraft(String(dl.data, Charsets.UTF_8))
                    Result.success(draft)
                }
                is Result.Error -> dl
            }
        } catch (e: Exception) {
            Log.e(TAG, "loadDraft($userId, $draftId) failed", e)
            Result.error(e)
        }
    }

    override suspend fun deleteDraft(userId: String, draftId: String): Result<Unit> {
        val key = StorageConfig.draftKey(userId, draftId)
        return storageRepository.delete(bucket, key)
    }

    override suspend fun deleteAllDrafts(userId: String): Result<Unit> {
        return storageRepository.deleteWithPrefix(bucket, "$userId/")
    }

    private fun serializeDraft(draft: QuizDraft): String {
        val obj = mapOf(
            "id" to draft.id,
            "title" to draft.title,
            "description" to draft.description,
            "categoryId" to draft.categoryId,
            "difficulty" to draft.difficulty.name,
            "coverUrl" to draft.coverUrl,
            "timePerQuestion" to draft.timePerQuestion,
            "updatedAt" to draft.updatedAt,
            "questions" to draft.questions.map { q ->
                mapOf(
                    "id" to q.id,
                    "text" to q.text,
                    "options" to q.options,
                    "correctIndex" to q.correctIndex,
                    "points" to q.points,
                    "timeLimit" to q.timeLimit,
                    "imageUrl" to q.imageUrl,
                    "isGif" to q.isGif
                )
            }
        )
        return gson.toJson(obj)
    }

    private fun deserializeDraft(json: String): QuizDraft {
        val root = JsonParser.parseString(json).asJsonObject

        fun JsonObject.str(key: String, default: String = "") =
            if (has(key) && !get(key).isJsonNull) get(key).asString else default

        fun JsonObject.int(key: String, default: Int = 0) =
            if (has(key) && !get(key).isJsonNull) get(key).asInt else default

        fun JsonObject.long(key: String, default: Long = 0L) =
            if (has(key) && !get(key).isJsonNull) get(key).asLong else default

        fun JsonObject.bool(key: String, default: Boolean = false) =
            if (has(key) && !get(key).isJsonNull) get(key).asBoolean else default

        fun JsonObject.strOrNull(key: String) =
            if (has(key) && !get(key).isJsonNull) get(key).asString else null

        val questions = if (root.has("questions") && root.get("questions").isJsonArray) {
            root.getAsJsonArray("questions").mapNotNull { elem ->
                try {
                    val q = elem.asJsonObject
                    val opts = if (q.has("options") && q.get("options").isJsonArray) {
                        q.getAsJsonArray("options").map { it.asString }
                    } else {
                        listOf("", "", "", "")
                    }
                    DraftQuestion(
                        id = q.str("id", UUID.randomUUID().toString()),
                        text = q.str("text"),
                        options = opts,
                        correctIndex = q.int("correctIndex"),
                        points = q.int("points", 10),
                        timeLimit = q.int("timeLimit", 30),
                        imageUrl = q.strOrNull("imageUrl"),
                        isGif = q.bool("isGif")
                    )
                } catch (e: Exception) {
                    Log.w(TAG, "Skipping malformed question: ${e.message}")
                    null
                }
            }
        } else {
            emptyList()
        }

        val difficulty = try {
            QuizDifficulty.valueOf(root.str("difficulty", "MEDIUM"))
        } catch (_: Exception) {
            QuizDifficulty.MEDIUM
        }

        return QuizDraft(
            id = root.str("id", UUID.randomUUID().toString()),
            title = root.str("title"),
            description = root.str("description"),
            categoryId = root.str("categoryId", "Кастомные"),
            difficulty = difficulty,
            coverUrl = root.strOrNull("coverUrl"),
            timePerQuestion = root.int("timePerQuestion", 30),
            updatedAt = root.long("updatedAt"),
            questions = questions.ifEmpty { listOf(DraftQuestion()) }
        )
    }

    companion object {
        private const val TAG = "QuizDraftRepository"
    }
}
