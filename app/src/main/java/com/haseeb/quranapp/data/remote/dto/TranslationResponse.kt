package com.haseeb.quranapp.data.remote.dto

data class TranslationResponse(
    val translations: List<TranslationItemDto>?
)

data class TranslationItemDto(
    val resource_id: Int,
    val text: String
)
