package com.example.brainracer.data.storage

import com.example.brainracer.BuildConfig

/**
 * Конфигурация Evolution Object Storage (совместим с AWS S3).
 *
 * Endpoint: https://s3.cloud.ru
 * Region:   ru-central-1
 *
 * Ключи доступа не хранятся в коде — они инжектируются через BuildConfig:
 *   BuildConfig.S3_ACCESS_KEY
 *   BuildConfig.S3_SECRET_KEY
 *
 * В local.properties (файл в .gitignore), для Cloud.ru Evolution:
 *   s3.access.key={tenantId}:{keyId}
 *     — tenantId: консоль → Хранение данных → Object Storage, над списком бакетов;
 *     — keyId: Key ID (логин) созданного ключа (через двоеточие, без пробелов).
 *   s3.secret.key={keySecret} — Key Secret (пароль).
 *   s3.avatar.public.base={baseUrl без слэша на конце} — публичный базовый URL аватаров.
 *   s3.quiz.public.base={baseUrl без слэша на конце}   — публичный базовый URL обложек/картинок викторин.
 *     Cloud.ru различает два формата (см. «Модели адресации»):
 *       Глобальное название → https://global.s3.cloud.ru/{глобальное_имя}
 *       Доменное имя       → https://{доменное_имя}.s3.cloud.ru
 *     Имена API-бакетов (BUCKET_*) на это НЕ влияют — они используются только в SDK.
 *   Только Key ID без tenant → S3 отвечает ошибкой про Access Key Id.
 *
 * Подпись SigV4: регион в scope должен быть ru-central-1 (см. документацию Cloud.ru).
 * В aws-android-sdk нет ru-central-1 в enum Regions — клиент S3 должен задавать регион
 * подписи явно (см. EvolutionStorageRepositoryImpl.setEndpoint(endpoint, "s3", REGION)).
 *
 * Структура бакетов (создайте в консоли cloud.ru):
 *   brainracer-avatars  — публичный на чтение, хранит аватары пользователей
 *   brainracer-quizzes  — публичный на чтение, хранит обложки и картинки вопросов
 *   brainracer-drafts   — приватный, хранит черновики викторин в формате JSON
 */
object StorageConfig {

    /** Endpoint Evolution Object Storage. */
    const val ENDPOINT = "https://s3.cloud.ru"

    /** Регион Evolution (для подписи AWS Signature V4). */
    const val REGION = "ru-central-1"

    /** Аватары пользователей (public-read). */
    const val BUCKET_AVATARS = "brainracer-avatars"

    /** Обложки и изображения к вопросам викторин (public-read). */
    const val BUCKET_QUIZZES = "brainracer-quizzes"

    /** Черновики викторин в JSON (private). */
    const val BUCKET_DRAFTS = "brainracer-drafts"

    /**
     * Ключ аватара: avatars/{userId}.{ext}
     * Один файл на пользователя — каждый upload перезаписывает предыдущий.
     */
    fun avatarKey(userId: String, ext: String) = "avatars/$userId.$ext"

    /** Ключ обложки: covers/{userId}/{uuid}.{ext} */
    fun quizCoverKey(userId: String, uuid: String, ext: String) =
        "covers/$userId/$uuid.$ext"

    /** Ключ изображения к вопросу: questions/{userId}/{uuid}.{ext} */
    fun questionImageKey(userId: String, uuid: String, ext: String) =
        "questions/$userId/$uuid.$ext"

    /** Ключ черновика: {userId}/{draftId}.json */
    fun draftKey(userId: String, draftId: String) = "$userId/$draftId.json"

    /**
     * Публичный URL для анонимного GET (браузер, Coil).
     * Cloud.ru: для анонимных запросов нужен Virtual-Hosted-Style
     * `https://{bucket}.s3.cloud.ru/{key}`, а не path-style `https://s3.cloud.ru/{bucket}/{key}`.
     * См. «Модели адресации» в документации Object Storage.
     */
    fun publicUrl(bucket: String, key: String): String {
        val k = key.trim().trimStart('/')
        return "https://$bucket.s3.cloud.ru/$k"
    }

    /** Базовый URL для анонимного GET аватаров (без хвостового '/'). */
    val avatarPublicBaseUrl: String
        get() = BuildConfig.S3_AVATAR_PUBLIC_BASE_URL.trim().trimEnd('/')

    /** Базовый URL для анонимного GET обложек/картинок викторин (без хвостового '/'). */
    val quizPublicBaseUrl: String
        get() = BuildConfig.S3_QUIZ_PUBLIC_BASE_URL.trim().trimEnd('/')

    /**
     * Публичный URL для произвольного публичного бакета (нужен SDK после putObject).
     * UI всегда должен использовать [resolvePublicUrlForCoil] — на случай, если в
     * Firestore сохранён старый URL.
     */
    fun publicUrlForBucket(bucket: String, key: String): String {
        val k = key.trim().trimStart('/')
        val base = when (bucket) {
            BUCKET_AVATARS -> avatarPublicBaseUrl.ifEmpty { "https://$BUCKET_AVATARS.s3.cloud.ru" }
            BUCKET_QUIZZES -> quizPublicBaseUrl.ifEmpty { "https://$BUCKET_QUIZZES.s3.cloud.ru" }
            else -> "https://$bucket.s3.cloud.ru"
        }
        return "$base/$k"
    }

    /**
     * Публичный URL аватара для Coil / браузера. Зависит только от
     * [avatarPublicBaseUrl] — не от имени API-бакета.
     */
    fun avatarPublicUrl(key: String): String = publicUrlForBucket(BUCKET_AVATARS, key)

    /** Публичный URL обложки/картинки вопроса викторины. */
    fun quizPublicUrl(key: String): String = publicUrlForBucket(BUCKET_QUIZZES, key)

    /**
     * Извлекает ключ объекта (без query) из любого ранее сохранённого публичного URL Cloud.ru:
     *   https://s3.cloud.ru/<bucket>/<key>            (path-style по базовому имени)
     *   https://global.s3.cloud.ru/<global>/<key>     (path-style по глобальному имени)
     *   https://<host>.s3.cloud.ru/<key>              (virtual-hosted по доменному / базовому имени)
     */
    // Публичная обёртка для [extractObjectKey] (для безопасного удаления старых объектов по URL)
    fun extractObjectKeyPublic(url: String): String? = extractObjectKey(url)

    // Публичная обёртка для [bucketForKey]
    fun bucketForKeyPublic(key: String): String? = bucketForKey(key)

    private fun extractObjectKey(url: String): String? {
        val u = url.trim().substringBefore('?').substringBefore('#')
        if (u.isEmpty()) return null
        val parts = listOf(
            "https://s3.cloud.ru/",
            "https://global.s3.cloud.ru/",
        )
        for (p in parts) {
            if (u.startsWith(p)) {
                val rest = u.removePrefix(p).trimStart('/')
                val slash = rest.indexOf('/')
                if (slash <= 0 || slash >= rest.length - 1) return null
                return rest.substring(slash + 1)
            }
        }
        val vhost = Regex("^https://([^/]+)\\.s3(?:-website)?\\.cloud\\.ru/(.+)$")
        vhost.matchEntire(u)?.let { m ->
            return m.groupValues[2]
        }
        return null
    }

    /** Извлекает query string `?...` (или пустую строку), сохраняя cache-buster `?v=`. */
    private fun extractQuery(url: String): String {
        val q = url.indexOf('?')
        if (q < 0) return ""
        val frag = url.indexOf('#', q + 1)
        return if (frag < 0) url.substring(q) else url.substring(q, frag)
    }

    /**
     * Публичный бакет, к которому относится ключ объекта. По первому сегменту:
     *   avatars/...   → BUCKET_AVATARS
     *   covers/...    → BUCKET_QUIZZES
     *   questions/... → BUCKET_QUIZZES
     */
    private fun bucketForKey(key: String): String? = when {
        key.startsWith("avatars/")   -> BUCKET_AVATARS
        key.startsWith("covers/")    -> BUCKET_QUIZZES
        key.startsWith("questions/") -> BUCKET_QUIZZES
        else -> null
    }

    /**
     * Универсальный resolve публичного URL для Coil: извлекает ключ из любого ранее
     * сохранённого формата, определяет бакет по префиксу ключа и склеивает с актуальным
     * базовым URL из BuildConfig. Query (включая `?v=`) сохраняется.
     */
    fun resolvePublicUrlForCoil(url: String): String {
        val u = url.trim()
        if (u.isEmpty()) return u
        val key = extractObjectKey(u) ?: return u
        val bucket = bucketForKey(key) ?: return u
        return publicUrlForBucket(bucket, key) + extractQuery(u)
    }

    /** Совместимость со старыми вызовами; делегирует в [resolvePublicUrlForCoil]. */
    fun resolveAvatarUrlForCoil(url: String): String = resolvePublicUrlForCoil(url)

    /**
     * Добавляет cache-buster `?v={millis}` к URL, чтобы при перезаписи объекта
     * по тому же ключу клиенты гарантированно перечитали новую версию.
     */
    fun appendVersion(url: String, version: Long = System.currentTimeMillis()): String {
        if (url.isBlank()) return url
        val sep = if (url.contains('?')) '&' else '?'
        val withoutFragment = url.substringBefore('#')
        val fragment = url.substringAfter('#', missingDelimiterValue = "")
            .let { if (it.isEmpty()) "" else "#$it" }
        return "$withoutFragment${sep}v=$version$fragment"
    }
}