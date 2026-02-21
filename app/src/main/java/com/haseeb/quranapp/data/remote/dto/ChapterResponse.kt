package com.haseeb.quranapp.data.remote.dto

data class ChapterResponse(
    val chapters: List<ChapterDto>
)

data class ChapterDto(
    val id: Int,
    val name_simple: String,
    val name_arabic: String,
    val verses_count: Int
)
