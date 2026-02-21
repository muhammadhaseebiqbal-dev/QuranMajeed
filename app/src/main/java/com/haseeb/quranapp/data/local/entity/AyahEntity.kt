package com.haseeb.quranapp.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "ayahs")
data class AyahEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val surahId: Int,
    val ayahNumber: Int,
    val textUthmani: String, // Arabic text
    val textTranslation: String?, // English/Urdu translation
    val pageNumber: Int,
    val juzzyNumber: Int
)
