package com.example.brainracer.data.storage

import com.example.brainracer.data.utils.Result

/**
 * Контракт для работы с Evolution Object Storage.
 * Все методы возвращают [Result], никогда не бросают исключений наружу.
 */
interface EvolutionStorageRepository {

    suspend fun upload(
        bucket: String,
        key: String,
        bytes: ByteArray,
        mimeType: String,
        isPublic: Boolean = true
    ): Result<String>

    suspend fun download(bucket: String, key: String): Result<ByteArray>

    suspend fun listKeys(bucket: String, prefix: String): Result<List<String>>

    suspend fun delete(bucket: String, key: String): Result<Unit>

    suspend fun deleteWithPrefix(bucket: String, prefix: String): Result<Unit>

    suspend fun presignedUrl(
        bucket: String,
        key: String,
        expirationSecs: Long = 3600L
    ): Result<String>
}
