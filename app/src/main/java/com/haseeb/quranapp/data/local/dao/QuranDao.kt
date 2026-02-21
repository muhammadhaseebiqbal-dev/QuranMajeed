package com.haseeb.quranapp.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.haseeb.quranapp.data.local.entity.AyahEntity
import com.haseeb.quranapp.data.local.entity.SurahEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface QuranDao {
    @Query("SELECT * FROM ayahs WHERE surahId = :surahId ORDER BY ayahNumber ASC")
    fun getAyahsBySurah(surahId: Int): Flow<List<AyahEntity>>

    @Query("SELECT * FROM ayahs WHERE juzzyNumber = :juzId ORDER BY surahId, ayahNumber ASC")
    fun getAyahsByJuz(juzId: Int): Flow<List<AyahEntity>>

    @Query("SELECT * FROM ayahs WHERE textTranslation LIKE '%' || :query || '%' OR textUthmani LIKE '%' || :query || '%'")
    suspend fun searchAyahs(query: String): List<AyahEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAyahs(ayahs: List<AyahEntity>)

    @Query("UPDATE ayahs SET textTranslation = :translation WHERE surahId = :surahId AND ayahNumber = :ayahNumber")
    suspend fun updateAyahTranslation(surahId: Int, ayahNumber: Int, translation: String)
    
    
    @Query("SELECT COUNT(*) FROM ayahs")
    suspend fun getAyahCount(): Int

    // Surah Operations
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSurahs(surahs: List<SurahEntity>)

    @Query("SELECT * FROM surahs ORDER BY id ASC")
    fun getAllSurahs(): Flow<List<SurahEntity>>

    @Query("SELECT COUNT(*) FROM surahs")
    suspend fun getSurahCount(): Int

    // Batch Operations
    @Query("SELECT * FROM ayahs")
    suspend fun getAllAyahs(): List<AyahEntity>

    @androidx.room.Update
    suspend fun updateAyahs(ayahs: List<AyahEntity>)
}
