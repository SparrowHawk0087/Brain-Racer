package com.example.brainracer.ui.utils

import com.example.brainracer.R
import com.example.brainracer.ui.viewmodels.QuizDraft
import com.google.firebase.FirebaseApp

data class ModerationViolation(
    val location: String,
    val reason: String,
    val category: ModerationCategory
)

enum class ModerationCategory(val fallbackLabel: String, val stringResId: Int) {
    DRUGS("Наркотики", R.string.moderation_category_drugs),
    VIOLENCE("Насилие", R.string.moderation_category_violence),
    HATE("Язык вражды", R.string.moderation_category_hate),
    SCAM("Мошенничество", R.string.moderation_category_scam),
    IMPERSONATION("Имитация роли", R.string.moderation_category_impersonation);

    fun localizedLabel(): String = runCatching {
        FirebaseApp.getInstance().applicationContext.getString(stringResId)
    }.getOrDefault(fallbackLabel)
}

data class ModerationKeywordRule(
    val keyword: String,
    val category: ModerationCategory
)

data class ModerationRegexRule(
    val pattern: Regex,
    val category: ModerationCategory
)

sealed interface ModerationResult {
    data object Allowed : ModerationResult
    data class Blocked(val violation: ModerationViolation) : ModerationResult
}

object QuizModeration {
    fun validateQuizDraft(draft: QuizDraft): ModerationResult {
        checkText(draft.title, "названии викторины")?.let { return ModerationResult.Blocked(it) }
        checkText(draft.description, "описании викторины")?.let { return ModerationResult.Blocked(it) }

        draft.questions.forEachIndexed { qIndex, q ->
            val questionIdx = qIndex + 1
            checkText(q.text, "вопросе $questionIdx")?.let { return ModerationResult.Blocked(it) }
            q.options.forEachIndexed { oIndex, opt ->
                checkText(opt, "варианте ${oIndex + 1} вопроса $questionIdx")
                    ?.let { return ModerationResult.Blocked(it) }
            }
            checkText(q.explanation, "подсказке к вопросу $questionIdx")
                ?.let { return ModerationResult.Blocked(it) }
        }

        return ModerationResult.Allowed
    }

    private fun checkText(text: String, location: String): ModerationViolation? {
        if (text.isBlank()) return null
        val normalized = normalizeForRegex(text)
        val collapsed = collapseForContains(normalized)
        val byKeyword = QuizModerationConfig.contentKeywordRules.firstOrNull { rule ->
            val keyCollapsed = collapseForContains(normalizeForRegex(rule.keyword))
            keyCollapsed.isNotBlank() && collapsed.contains(keyCollapsed)
        }
        if (byKeyword != null) {
            return ModerationViolation(
                location = location,
                reason = "обнаружена запрещенная тема",
                category = byKeyword.category
            )
        }
        val byRegex = QuizModerationConfig.contentRegexRules.firstOrNull { it.pattern.containsMatchIn(normalized) }
        if (byRegex != null) {
            return ModerationViolation(
                location = location,
                reason = "обнаружен недопустимый контент",
                category = byRegex.category
            )
        }
        return null
    }

    fun validateUsername(username: String): ModerationResult {
        if (username.isBlank()) return ModerationResult.Allowed
        val normalized = normalizeForRegex(username)
        val collapsed = collapseForContains(normalized)

        val byKeyword = QuizModerationConfig.usernameKeywordRules.firstOrNull { rule ->
            val keyCollapsed = collapseForContains(normalizeForRegex(rule.keyword))
            keyCollapsed.isNotBlank() && collapsed.contains(keyCollapsed)
        }
        if (byKeyword != null) {
            return ModerationResult.Blocked(
                ModerationViolation(
                    location = "имени пользователя",
                    reason = "обнаружено запрещенное слово",
                    category = byKeyword.category
                )
            )
        }

        val byRegex = QuizModerationConfig.usernameRegexRules.firstOrNull {
            it.pattern.containsMatchIn(normalized) || it.pattern.containsMatchIn(collapsed)
        }
        if (byRegex != null) {
            return ModerationResult.Blocked(
                ModerationViolation(
                    location = "имени пользователя",
                    reason = "обнаружен недопустимый шаблон",
                    category = byRegex.category
                )
            )
        }
        return ModerationResult.Allowed
    }

    private fun normalizeForRegex(raw: String): String {
        val lowered = raw.lowercase()
        val mapped = buildString(lowered.length) {
            for (ch in lowered) {
                append(
                    when (ch) {
                        '0' -> 'o'
                        '1' -> 'i'
                        '3' -> 'e'
                        '4' -> 'a'
                        '5' -> 's'
                        '6' -> 'b'
                        '7' -> 't'
                        '8' -> 'b'
                        '@' -> 'a'
                        '$' -> 's'
                        '(' -> 'c'
                        'a' -> 'а'
                        'e' -> 'е'
                        'o' -> 'о'
                        'p' -> 'р'
                        'c' -> 'с'
                        'x' -> 'х'
                        'y' -> 'у'
                        'k' -> 'к'
                        'm' -> 'м'
                        't' -> 'т'
                        'b' -> 'в'
                        else -> ch
                    }
                )
            }
        }
        return mapped
            .replace(Regex("""[^\p{L}\p{N}\s]"""), " ")
            .replace(Regex("""\s+"""), " ")
            .trim()
    }

    private fun collapseForContains(text: String): String =
        text.replace(Regex("""[^\p{L}\p{N}]"""), "")
}
