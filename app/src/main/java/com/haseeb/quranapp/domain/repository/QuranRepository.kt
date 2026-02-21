package com.haseeb.quranapp.domain.repository

import com.haseeb.quranapp.data.local.entity.AyahEntity
import com.haseeb.quranapp.data.local.entity.SurahEntity
import kotlinx.coroutines.flow.Flow

interface QuranRepository {
    fun getAllSurahs(): Flow<List<SurahEntity>>
    fun getAyahsBySurah(surahId: Int): Flow<List<AyahEntity>>
    fun getAyahsByJuz(juzId: Int): Flow<List<AyahEntity>>
    suspend fun syncQuranData(): Result<Unit>
    suspend fun searchAyahs(query: String): List<AyahEntity>
    suspend fun getTafsir(verseKey: String): Result<String>
}
