package com.example.brainracer.data.utils

sealed class Result<out T> {
    data class Success<out T>(val data: T) : Result<T>()
    data class Error(val exception: Exception) : Result<Nothing>()

    companion object {
        fun <T> success(data: T): Result<T> = Success(data)
        fun error(exception: Exception): Result<Nothing> = Error(exception)
        fun error(message: String): Result<Nothing> = Error(Exception(message))
    }
}

// Расширения для удобства
fun <T> Result<T>.getOrNull(): T? = when (this) {
    is Result.Success -> data
    is Result.Error -> null
}

fun <T> Result<T>.getOrElse(defaultValue: T): T = when (this) {
    is Result.Success -> data
    is Result.Error -> defaultValue
}

fun <T> Result<T>.fold(
    onSuccess: (T) -> Unit,
    onFailure: (Exception) -> Unit
) {
    when (this) {
        is Result.Success -> onSuccess(data)
        is Result.Error -> onFailure(exception)
    }
}

fun <T, R> Result<T>.map(transform: (T) -> R): Result<R> = when (this) {
    is Result.Success -> Result.Success(transform(data))
    is Result.Error -> this
}