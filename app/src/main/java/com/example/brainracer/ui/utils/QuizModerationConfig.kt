package com.example.brainracer.ui.utils

object QuizModerationConfig {
    val contentKeywordRules: List<ModerationKeywordRule> = listOf(
        "наркотик","закладк","кокаин","героин","амфетамин","мефедрон","лсд","спайс","соль","передоз",
        "drug","dealer","weed","cannabis","cocain","heroin","meth","fentanyl","ecstasy","ketamin"
    ).map { ModerationKeywordRule(it, ModerationCategory.DRUGS) } + listOf(
        "террор","теракт","бомб","взрывчат","детонатор","тротил","огнестрел","пистолет","автомат","гранат","снайпер",
        "суицид","самоубийств","селфхарм","порез","повесься","выпрыгни","синий кит",
        "terror","bomb","explosive","molotov","firearm","assault rifle","glock","handgun","mass shooting","school shooting",
        "suicid","selfharm","kill yourself","blue whale challenge","rape"
    ).map { ModerationKeywordRule(it, ModerationCategory.VIOLENCE) } + listOf(
        "расизм","расист","нацизм","нацист","фашизм","экстремизм","антисемит","ниггер","чурк","хач","свастик","гитлер",
        "racist","nazi","extremism","nigger","white power","swastika","hitler","kkk","klan"
    ).map { ModerationKeywordRule(it, ModerationCategory.HATE) } + listOf(
        "казино","рулетк","ставк","букмекер","тотализатор","лохотрон","скам","фишинг","обнал","mlm","pyramid scheme",
        "casino","poker","blackjack","betting","scam","phishing","money laundering","get rich quick"
    ).map { ModerationKeywordRule(it, ModerationCategory.SCAM) }
    val contentRegexRules: List<ModerationRegexRule> = listOf(
        ModerationRegexRule(Regex("""\b(как\s+(сделать|изготовить)\s+бомб[ауы]?)\b"""), ModerationCategory.VIOLENCE),
        ModerationRegexRule(Regex("""\b(где\s+(найти|купить|заказать)\s+(наркот[аиу]?|закладк[уие]?|соль|скорость|гашиш|кокаин|героин))\b"""), ModerationCategory.DRUGS),
        ModerationRegexRule(Regex("""\b(инструкци[яию]\s+(по|как)\s+(совершить\s+(с[ау]ицид|убийство)|повеситься|взорвать(ся)?|зарезать))\b"""), ModerationCategory.VIOLENCE),
        ModerationRegexRule(Regex("""\b(купить\s+(огнестрел|оружие|пистолет|автомат|гранату))\b"""), ModerationCategory.VIOLENCE),
        ModerationRegexRule(Regex("""\b(how\s+to\s+(make|build)\s+(a\s+)?bomb)\b"""), ModerationCategory.VIOLENCE),
        ModerationRegexRule(Regex("""\b(where\s+(to|can\s+i)\s+(buy|get|find)\s+(drugs?|weed|meth|coke|heroin))\b"""), ModerationCategory.DRUGS),
        ModerationRegexRule(Regex("""\b(how\s+to\s+commit\s+suicide)\b"""), ModerationCategory.VIOLENCE),
        ModerationRegexRule(Regex("""\b(how\s+to\s+(join|become)\s+(isis|al-qaeda|terrorist|jihad))\b"""), ModerationCategory.VIOLENCE),
        ModerationRegexRule(Regex("""\b(leaked\s+(schoolgirl|child|teen|nude|onlyfans))\b"""), ModerationCategory.VIOLENCE),
        ModerationRegexRule(Regex("""\b(child\s+(porn|pornography|nude|model))\b"""), ModerationCategory.VIOLENCE)
    )
    val usernameKeywordRules: List<ModerationKeywordRule> = listOf(
        "admin","админ","moderator","support","official","staff","quizmaster"
    ).map { ModerationKeywordRule(it, ModerationCategory.IMPERSONATION) } + listOf(
        "казино","casino","скам","scam","фишинг","phishing","mlm","pyramid scheme"
    ).map { ModerationKeywordRule(it, ModerationCategory.SCAM) } + listOf(
        "нацист","nazi","расист","racist","ниггер","nigger","kkk","klan","свастик","swastika"
    ).map { ModerationKeywordRule(it, ModerationCategory.HATE) } + listOf(
        "террорист","terrorist","убийц","маньяк","rape","изнасилова","педофил","pedophil"
    ).map { ModerationKeywordRule(it, ModerationCategory.VIOLENCE) } + listOf(
        "наркотик","drug","dealer","кокаин","heroin","meth","амфетамин"
    ).map { ModerationKeywordRule(it, ModerationCategory.DRUGS) }
    val usernameRegexRules: List<ModerationRegexRule> = listOf(
        ModerationRegexRule(Regex("""\b(admin|moderator|support|quizmaster|official|staff)\b"""), ModerationCategory.IMPERSONATION),
        ModerationRegexRule(Regex("""\b(f[\s\.\-_]*u[\s\.\-_]*c[\s\.\-_]*k)\b"""), ModerationCategory.VIOLENCE),
        ModerationRegexRule(Regex("""\b(s[\s\.\-_]*h[\s\.\-_]*i[\s\.\-_]*t)\b"""), ModerationCategory.VIOLENCE),
        ModerationRegexRule(Regex("""\b(n[\s\.\-_]*i[\s\.\-_]*g[\s\.\-_]*g[\s\.\-_]*e[\s\.\-_]*r)\b"""), ModerationCategory.HATE),
        ModerationRegexRule(Regex("""\b(p[\s\.\-_]*e[\s\.\-_]*d[\s\.\-_]*o)\b"""), ModerationCategory.VIOLENCE),
        ModerationRegexRule(Regex("""^[0-9\W]+$"""), ModerationCategory.IMPERSONATION)
    )
}
