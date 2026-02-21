package com.haseeb.quranapp.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "surahs")
data class SurahEntity(
    @PrimaryKey val id: Int,
    val nameSimple: String,
    val nameArabic: String,
    val versesCount: Int
)
