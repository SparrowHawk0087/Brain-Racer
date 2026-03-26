package com.example.brainracer.data.local

import android.content.Context
import com.example.brainracer.domain.entities.Quiz
import com.google.firebase.Timestamp
import com.google.gson.GsonBuilder
import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.google.gson.JsonSerializationContext
import com.google.gson.JsonSerializer
import java.io.File
import java.lang.reflect.Type
import java.util.concurrent.TimeUnit

/**
 * Файловый кэш викторины для прохождения без сети после хотя бы одной успешной загрузки.
 * Записи старше [defaultTtlMillis] при чтении и при сохранении удаляются ([purgeExpired]).
 */
object QuizOfflineCache {

    /** Срок жизни кэша по умолчанию (7 суток). */
    val defaultTtlMillis: Long = TimeUnit.DAYS.toMillis(7)

    private var appCtx: Context? = null

    fun init(context: Context) {
        appCtx = context.applicationContext
        purgeExpired()
    }

    fun applicationContextOrNull(): Context? = appCtx

    private val gson by lazy {
        GsonBuilder()
            .registerTypeAdapter(Timestamp::class.java, TimestampTypeAdapter())
            .create()
    }

    private fun cacheDir(): File? {
        val base = appCtx?.filesDir ?: return null
        return File(base, "quiz_offline_cache").apply { mkdirs() }
    }

    private data class QuizCacheEnvelope(
        val savedAtMillis: Long,
        val quiz: Quiz
    )

    fun save(quiz: Quiz) {
        val dir = cacheDir() ?: return
        try {
            val envelope = QuizCacheEnvelope(System.currentTimeMillis(), quiz)
            File(dir, "${quiz.id}.json").writeText(gson.toJson(envelope))
            purgeExpired()
        } catch (_: Exception) {
            // игнорируем сбои записи кэша
        }
    }

    /**
     * Удаляет файлы кэша старше [ttlMillis] (по метке в JSON или по [File.lastModified] для старого формата).
     */
    fun purgeExpired(ttlMillis: Long = defaultTtlMillis) {
        val dir = cacheDir() ?: return
        val now = System.currentTimeMillis()
        dir.listFiles()?.forEach { file ->
            if (!file.isFile || !file.name.endsWith(".json")) return@forEach
            try {
                val text = file.readText()
                val savedAt = readSavedAtMillis(text, file.lastModified())
                if (now - savedAt > ttlMillis) file.delete()
            } catch (_: Exception) {
                file.delete()
            }
        }
    }

    private fun readSavedAtMillis(json: String, fileLastModified: Long): Long {
        return try {
            val root = JsonParser.parseString(json).asJsonObject
            if (root.has("savedAtMillis") && root.get("savedAtMillis").isJsonPrimitive) {
                root.get("savedAtMillis").asLong
            } else {
                fileLastModified
            }
        } catch (_: Exception) {
            fileLastModified
        }
    }

    fun load(quizId: String, ttlMillis: Long = defaultTtlMillis): Quiz? {
        val dir = cacheDir() ?: return null
        return try {
            val f = File(dir, "$quizId.json")
            if (!f.exists()) return null
            val text = f.readText()
            val savedAt = readSavedAtMillis(text, f.lastModified())
            if (System.currentTimeMillis() - savedAt > ttlMillis) {
                f.delete()
                return null
            }
            parseQuizFromCacheFile(text)
        } catch (_: Exception) {
            null
        }
    }

    /** Старый формат: в корне лежит сериализованный [Quiz]; новый — обёртка с полем quiz. */
    private fun parseQuizFromCacheFile(text: String): Quiz? {
        val root = JsonParser.parseString(text).asJsonObject
        return if (root.has("quiz") && root.get("quiz").isJsonObject) {
            gson.fromJson(text, QuizCacheEnvelope::class.java)?.quiz
        } else {
            gson.fromJson(text, Quiz::class.java)
        }
    }

    private class TimestampTypeAdapter : JsonSerializer<Timestamp>, JsonDeserializer<Timestamp> {
        override fun serialize(
            src: Timestamp?,
            typeOfSrc: Type?,
            context: JsonSerializationContext?
        ): JsonElement {
            if (src == null) return JsonObject()
            return JsonObject().apply {
                addProperty("seconds", src.seconds)
                addProperty("nanoseconds", src.nanoseconds)
            }
        }

        override fun deserialize(
            json: JsonElement?,
            typeOfT: Type?,
            context: JsonDeserializationContext?
        ): Timestamp? {
            if (json == null || !json.isJsonObject) return null
            val o = json.asJsonObject
            val sec = o.get("seconds")?.asLong ?: return null
            val nano = o.get("nanoseconds")?.asInt ?: 0
            return Timestamp(sec, nano)
        }
    }
}
