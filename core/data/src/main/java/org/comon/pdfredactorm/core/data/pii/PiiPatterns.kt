package org.comon.pdfredactorm.core.data.pii

import org.comon.pdfredactorm.core.model.RedactionType

object PiiPatterns {
    data class PiiPattern(
        val type: RedactionType,
        val regex: Regex,
        val description: String
    )

    val patterns = listOf(
        // 주민번호 (RRN): 6자리-7자리 또는 13자리
        PiiPattern(
            type = RedactionType.RRN,
            regex = Regex("""\d{6}[-\s]?\d{7}"""),
            description = "Resident Registration Number (주민번호)"
        ),

        // 전화번호 (010으로 시작): 010-####-#### 또는 010########
        PiiPattern(
            type = RedactionType.PHONE_NUMBER,
            regex = Regex("""010[-\s]?\d{4}[-\s]?\d{4}"""),
            description = "Mobile Phone Number (개인전화번호)"
        ),

        // 이메일
        PiiPattern(
            type = RedactionType.EMAIL,
            regex = Regex("""[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}"""),
            description = "Email Address (이메일)"
        ),

        // 생년월일: YYYY-MM-DD, YYYY.MM.DD, YYYYMMDD
        PiiPattern(
            type = RedactionType.BIRTH_DATE,
            regex = Regex("""(19|20)\d{2}[-./\s]?(0[1-9]|1[0-2])[-./\s]?(0[1-9]|[12]\d|3[01])"""),
            description = "Date of Birth (생년월일)"
        ),

        // 주소: 한국 주소 패턴 (시/도/구/군/읍/면/동/로/길 포함)
        PiiPattern(
            type = RedactionType.ADDRESS,
            regex = Regex("""(서울|부산|대구|인천|광주|대전|울산|세종|경기|강원|충북|충남|전북|전남|경북|경남|제주)[^\n]{0,50}(시|구|군|읍|면|동|로|길)[\s\d-]*"""),
            description = "Physical Address (주소)"
        )
    )

    fun detectAll(text: String): List<Pair<RedactionType, MatchResult>> {
        val results = mutableListOf<Pair<RedactionType, MatchResult>>()

        patterns.forEach { pattern ->
            pattern.regex.findAll(text).forEach { match ->
                results.add(Pair(pattern.type, match))
            }
        }

        return results
    }
}
