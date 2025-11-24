package org.comon.pdfredactorm.domain.model

data class RedactionMask(
    val id: String,
    val pageIndex: Int,
    val x: Float,
    val y: Float,
    val width: Float,
    val height: Float,
    val type: RedactionType = RedactionType.MANUAL
)

enum class RedactionType {
    MANUAL,
    PHONE_NUMBER,
    EMAIL,
    RRN, // Resident Registration Number
    BIRTH_DATE, // Date of Birth
    ADDRESS // Physical Address
}
