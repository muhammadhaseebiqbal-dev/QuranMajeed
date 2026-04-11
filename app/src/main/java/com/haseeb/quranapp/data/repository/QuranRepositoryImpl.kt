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

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import com.google.gson.Gson
import com.haseeb.quranapp.data.remote.dto.QuranResponse

class QuranRepositoryImpl @Inject constructor(
    private val api: QuranApiService,
    private val dao: QuranDao,
    private val userPrefs: com.haseeb.quranapp.data.local.prefs.UserPreferences,
    private val downloadManager: com.haseeb.quranapp.data.download.SurahDownloadManager,
    @ApplicationContext private val context: Context
) : QuranRepository {

    private var cachedTafsirId: Int = -1
    private var cachedTafsirMap: Map<String, String>? = null

    private suspend fun getTafsirFromAssetSafely(tafsirId: Int, verseKey: String): String? = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        synchronized(this@QuranRepositoryImpl) {
            if (cachedTafsirId == tafsirId && cachedTafsirMap != null) {
                return@synchronized cachedTafsirMap?.get(verseKey)
            }
            try {
                val fileName = "tafsir_$tafsirId.json"
                val inputStream = context.assets.open(fileName)
                val jsonString = inputStream.bufferedReader().use { it.readText() }
                
                val type = object : com.google.gson.reflect.TypeToken<Map<String, Map<String, String>>>() {}.type
                val result: Map<String, Map<String, String>> = Gson().fromJson(jsonString, type)
                
                cachedTafsirMap = result["tafsirs"]
                cachedTafsirId = tafsirId
                
                return@synchronized cachedTafsirMap?.get(verseKey)
            } catch (e: Exception) {
                Log.e("QuranRepo", "Failed to load tafsir $tafsirId from assets", e)
                return@synchronized null
            }
        }
    }

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
            val resourceId = userPrefs.tafsirId
            
            // Check local bundled assets
            val text = getTafsirFromAssetSafely(resourceId, verseKey)
            if (text != null) {
                return Result.success(text)
            }
            
            Result.failure(Exception("No bundled tafsir found for $verseKey"))
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
                // Initial fetch from pre-packaged asset to avoid OOM
                Log.d("QuranRepo", "Loading Uthmani verses from assets...")
                val inputStream = context.assets.open("uthmani_verses.json")
                val jsonString = inputStream.bufferedReader().use { it.readText() }
                val response = Gson().fromJson(jsonString, QuranResponse::class.java)
                
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

            // 3. Sync Translation - Repair/Update
            val firstAyah = dao.getAyahsBySurah(1).firstOrNull()?.firstOrNull()
            
            // Force check if translation is missing, length is 0, or user changed translation preference
            if (firstAyah?.textTranslation.isNullOrEmpty() || userPrefs.lastSyncedTranslationId != userPrefs.translationId) {
                val translationId = userPrefs.translationId
                Log.d("QuranRepo", "Syncing Translation... ID: $translationId")
                
                try {
                    val fileName = "translation_$translationId.json"
                    val inputStream = context.assets.open(fileName)
                    val jsonString = inputStream.bufferedReader().use { it.readText() }
                    val translationResponse = Gson().fromJson(jsonString, com.haseeb.quranapp.data.remote.dto.TranslationResponse::class.java)
                    val translationsList = translationResponse.translations ?: emptyList()

                    if (translationsList.isNotEmpty()) {
                        val allAyahs = dao.getAllAyahs()
                        if (allAyahs.size == translationsList.size) {
                            val updatedAyahs = allAyahs.zip(translationsList).mapNotNull { (ayah, translationDto) ->
                                val newTrans = translationDto.text
                                if (newTrans != ayah.textTranslation) {
                                    ayah.copy(textTranslation = newTrans)
                                } else {
                                    null
                                }
                            }
                            if (updatedAyahs.isNotEmpty()) {
                                Log.d("QuranRepo", "Updating ${updatedAyahs.size} translations from bundled asset...")
                                dao.updateAyahs(updatedAyahs)
                                userPrefs.lastSyncedTranslationId = translationId
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.e("QuranRepo", "Translation sync failed from assets", e)
                }
            }
            
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("QuranRepo", "Sync failed", e)
            Result.failure(e)
        }
    }
}
