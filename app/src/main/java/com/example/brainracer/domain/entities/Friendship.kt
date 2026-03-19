package com.example.brainracer.domain.entities

import com.google.firebase.Timestamp

// ─────────────────────────────────────────────────────────────────────────────
//  Статус дружбы / запроса
// ─────────────────────────────────────────────────────────────────────────────
//
// Enum остаётся без изменений — Firebase хранит его как строку ("PENDING" и т.д.)
// и автоматически конвертирует обратно при toObject(), если имена совпадают.

enum class FriendshipStatus {
    PENDING,   // запрос отправлен, ждёт ответа
    ACCEPTED,  // запрос принят, дружба установлена
    BLOCKED    // запрос отклонён
}

// ─────────────────────────────────────────────────────────────────────────────
//  Сущность запроса в друзья (хранится в коллекции friend_requests)
// ─────────────────────────────────────────────────────────────────────────────
//
// Почему у всех полей есть значения по умолчанию?
// Firebase создаёт объект через рефлексию, вызывая конструктор БЕЗ аргументов.
// В Kotlin data class такой конструктор появляется автоматически только тогда,
// когда каждое поле имеет дефолтное значение. Без этого toObject() бросит
// исключение "no suitable constructor found".
//
// Почему Timestamp, а не LocalDateTime?
// Firestore не знает о java.time.LocalDateTime. Зато он нативно поддерживает
// com.google.firebase.Timestamp — именно это значение возвращает serverTimestamp()
// и именно с ним работают индексы на скриншоте (поле createdAt).

data class FriendRequest(
    val id: String = "",
    val senderId: String = "",
    val receiverId: String = "",

    // Статус хранится как строка ("PENDING", "ACCEPTED", "BLOCKED").
    // При чтении из Firestore Firebase конвертирует строку обратно в enum
    // автоматически, потому что тип поля совпадает с enum-классом.
    val status: FriendshipStatus = FriendshipStatus.PENDING,

    // Timestamp.now() возвращает серверное время. Firebase умеет сериализовать
    // этот тип в поле типа "timestamp" в Firestore и читать его обратно.
    val createdAt: Timestamp = Timestamp.now(),
    val updatedAt: Timestamp = Timestamp.now()
)

// ─────────────────────────────────────────────────────────────────────────────
//  Сущность дружбы (опциональная — можно использовать для денормализации)
// ─────────────────────────────────────────────────────────────────────────────
//
// В текущей архитектуре проекта список друзей хранится прямо в документе
// пользователя (поле friends: List<String> с uid-ами). Сущность Friendship
// может понадобиться, если вы захотите хранить отдельную коллекцию friendships
// с метаданными (например, с какого момента дружат).
//
// LocalDateTime заменён на Timestamp по той же причине, что и в FriendRequest.

data class Friendship(
    val id: String = "",
    val user1Id: String = "",
    val user2Id: String = "",
    val status: FriendshipStatus = FriendshipStatus.ACCEPTED,
    val createdAt: Timestamp = Timestamp.now(),
    val updatedAt: Timestamp = Timestamp.now()
)