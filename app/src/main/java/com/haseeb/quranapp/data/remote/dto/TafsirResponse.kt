package com.haseeb.quranapp.data.remote.dto

data class TafsirResponse(
    val tafsir: TafsirItemDto?
)

data class TafsirItemDto(
    val resource_id: Int,
    val resource_name: String?,
    val text: String,
    val verse_key: String
)
