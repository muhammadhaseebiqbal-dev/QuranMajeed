package com.haseeb.quranapp.data.remote.api

import com.haseeb.quranapp.data.remote.dto.QuranResponse
import retrofit2.http.GET
import retrofit2.http.Query

interface QuranApiService {
    @GET("v4/quran/verses/uthmani")
    suspend fun getUthmaniVerses(
        @Query("limit") limit: Int = 10000 
    ): QuranResponse

    @GET("v4/chapters")
    suspend fun getChapters(): ChapterResponse

    // Fetch generic resource URL
    @GET("v4/quran/translations/{resourceId}")
    suspend fun getTranslation(
        @retrofit2.http.Path("resourceId") resourceId: Int
    ): com.haseeb.quranapp.data.remote.dto.TranslationResponse

    @GET("v4/tafsirs/{resourceId}/by_ayah/{verseKey}")
    suspend fun getTafsir(
        @retrofit2.http.Path("resourceId") resourceId: Int,
        @retrofit2.http.Path("verseKey") verseKey: String
    ): com.haseeb.quranapp.data.remote.dto.TafsirResponse

    // Fetch Audio Files (Verse by Verse) for a Chapter
    @GET("v4/recitations/{recitationId}/by_chapter/{chapterId}")
    suspend fun getRecitationByChapter(
        @retrofit2.http.Path("recitationId") recitationId: Int,
        @retrofit2.http.Path("chapterId") chapterId: Int,
        @Query("per_page") perPage: Int = 300
    ): com.haseeb.quranapp.data.remote.dto.RecitationResponse
}
