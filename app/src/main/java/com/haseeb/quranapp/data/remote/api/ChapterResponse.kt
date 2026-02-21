package com.haseeb.quranapp.data.remote.api

data class ChapterResponse(
    val chapters: List<ChapterDto>
)

data class ChapterDto(
    val id: Int,
    val revelation_place: String,
    val revelation_order: Int,
    val bismillah_pre: Boolean,
    val name_simple: String,
    val name_complex: String,
    val name_arabic: String,
    val verses_count: Int,
    val pages: List<Int>,
    val translated_name: TranslatedName
)

data class TranslatedName(
    val language_name: String,
    val name: String
)
