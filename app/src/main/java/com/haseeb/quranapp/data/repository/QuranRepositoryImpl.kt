package com.haseeb.quranapp.data.repository

import android.util.Log
import com.haseeb.quranapp.data.local.dao.QuranDao
import com.haseeb.quranapp.data.local.entity.AyahEntity
import com.haseeb.quranapp.data.local.entity.SurahEntity
import com.haseeb.quranapp.data.remote.api.QuranApiService
import com.haseeb.quranapp.domain.repository.QuranRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import javax.inject.Inject

class QuranRepositoryImpl @Inject constructor(
    private val api: QuranApiService,
    private val dao: QuranDao,
    private val userPrefs: com.haseeb.quranapp.data.local.prefs.UserPreferences
) : QuranRepository {

    override fun getAllSurahs(): Flow<List<SurahEntity>> {
        return dao.getAllSurahs()
    }

    override fun getAyahsBySurah(surahId: Int): Flow<List<AyahEntity>> {
        return dao.getAyahsBySurah(surahId)
    }

    override fun getAyahsByJuz(juzId: Int): Flow<List<AyahEntity>> {
        return dao.getAyahsByJuz(juzId)
    }

    override suspend fun searchAyahs(query: String): List<AyahEntity> {
        return dao.searchAyahs(query)
    }

    override suspend fun getTafsir(verseKey: String): Result<String> {
        return try {
            // Use user-configured Tafsir ID
            val resourceId = userPrefs.tafsirId 
            val response = api.getTafsir(resourceId, verseKey)
            val text = response.tafsir?.text
            if (text != null) Result.success(text) else Result.failure(Exception("No tafsir found"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun syncQuranData(): Result<Unit> {
        return try {
            // 1. Sync Surahs
            val surahCount = dao.getSurahCount()
            if (surahCount == 0) {
                val chaptersResponse = api.getChapters()
                val surahEntities = chaptersResponse.chapters.map { dto ->
                    SurahEntity(
                        id = dto.id,
                        nameSimple = dto.name_simple,
                        nameArabic = dto.name_arabic,
                        versesCount = dto.verses_count
                    )
                }
                dao.insertSurahs(surahEntities)
            }

            // 2. Sync Ayahs (and Repair Juz Data)
            val ayahCount = dao.getAyahCount()
            
            if (ayahCount == 0) {
                // Initial fetch
                val response = api.getUthmaniVerses(limit = 6236)
                val entities = response.verses?.map { dto ->
                    val parts = dto.verse_key.split(":")
                    val surahId = parts.getOrNull(0)?.toIntOrNull() ?: 1
                    val ayahNum = parts.getOrNull(1)?.toIntOrNull() ?: 1
                    val juzNum = com.haseeb.quranapp.util.JuzHelper.getJuzForAyah(surahId, ayahNum)

                    AyahEntity(
                        surahId = surahId,
                        ayahNumber = ayahNum,
                        textUthmani = dto.text_uthmani,
                        textTranslation = null,
                        pageNumber = 0, 
                        juzzyNumber = juzNum
                    )
                } ?: emptyList()

                if (entities.isNotEmpty()) {
                    dao.insertAyahs(entities)
                }
            } else {
                // REPAIR: Check if Juz data is missing (e.g. juzzyNumber is 0)
                // We'll check the first ayah of Juz 2 (Surah 2, Ayah 142)
                val checkAyah = dao.getAyahsBySurah(2).firstOrNull()?.find { it.ayahNumber == 142 }
                if (checkAyah != null && checkAyah.juzzyNumber == 0) {
                     Log.d("QuranRepo", "Repairing Juz Data...")
                     val allAyahs = dao.getAllAyahs()
                     val updatedAyahs = allAyahs.map { ayah ->
                         val juzNum = com.haseeb.quranapp.util.JuzHelper.getJuzForAyah(ayah.surahId, ayah.ayahNumber)
                         ayah.copy(juzzyNumber = juzNum)
                     }
                     dao.updateAyahs(updatedAyahs)
                }
            }

            // 3. Sync Translation (Urdu) - Repair/Update
            val firstAyah = dao.getAyahsBySurah(1).firstOrNull()?.firstOrNull()
            
            // Force check if translation is missing, length is 0, or user changed translation preference
            if (firstAyah?.textTranslation.isNullOrEmpty() || userPrefs.lastSyncedTranslationId != userPrefs.translationId) {
                Log.d("QuranRepo", "Syncing Translation... ID: ${userPrefs.translationId}")
                try {
                    val translationResponse = api.getTranslation(userPrefs.translationId)
                    val translationsList = translationResponse.translations ?: emptyList()

                    if (translationsList.isNotEmpty()) {
                        val allAyahs = dao.getAllAyahs()
                        
                        // Assuming the API returns 6236 translations exactly in sequence
                        if (allAyahs.size == translationsList.size) {
                            val updatedAyahs = allAyahs.zip(translationsList).mapNotNull { (ayah, translationDto) ->
                                val newTrans = translationDto.text
                                // Only update if different or missing
                                if (newTrans != ayah.textTranslation) {
                                    ayah.copy(textTranslation = newTrans)
                                } else {
                                    null
                                }
                            }
                            
                            if (updatedAyahs.isNotEmpty()) {
                                // Split into chunks to avoid SQLite limits if necessary
                                Log.d("QuranRepo", "Updating ${updatedAyahs.size} translations...")
                                dao.updateAyahs(updatedAyahs)
                                
                                // Save that we've successfully synced this ID
                                userPrefs.lastSyncedTranslationId = userPrefs.translationId
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.e("QuranRepo", "Translation sync failed", e)
                }
            }
            
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("QuranRepo", "Sync failed", e)
            Result.failure(e)
        }
    }
}
