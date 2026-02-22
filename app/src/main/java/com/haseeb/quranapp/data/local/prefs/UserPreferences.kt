package com.haseeb.quranapp.data.local.prefs

import android.content.Context
import android.content.SharedPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserPreferences @Inject constructor(
    @ApplicationContext context: Context
) {
    private val prefs: SharedPreferences = context.getSharedPreferences("quran_prefs", Context.MODE_PRIVATE)

    fun registerListener(listener: SharedPreferences.OnSharedPreferenceChangeListener) {
        prefs.registerOnSharedPreferenceChangeListener(listener)
    }

    fun unregisterListener(listener: SharedPreferences.OnSharedPreferenceChangeListener) {
        prefs.unregisterOnSharedPreferenceChangeListener(listener)
    }

    // Current Reciter ID (Default 10: Saud ash-Shuraym)
    var reciterId: Int
        get() = prefs.getInt("reciter_id", 10)
        set(value) = prefs.edit().putInt("reciter_id", value).apply()

    // Current Translation ID (Default 54: Urdu Jalandhry)
    var translationId: Int
        get() = prefs.getInt("translation_id", 54)
        set(value) = prefs.edit().putInt("translation_id", value).apply()

    // Last Synced Translation ID to trigger re-syncs
    var lastSyncedTranslationId: Int
        get() = prefs.getInt("last_synced_translation_id", -1)
        set(value) = prefs.edit().putInt("last_synced_translation_id", value).apply()

    // Current Tafsir ID (Default 160: Urdu Tafsir Ibn Kathir)
    var tafsirId: Int
        get() = prefs.getInt("tafsir_id", 160)
        set(value) = prefs.edit().putInt("tafsir_id", value).apply()

    // Play with Translation Toggle (Default false)
    var playWithTranslation: Boolean
        get() = prefs.getBoolean("play_with_translation", false)
        set(value) = prefs.edit().putBoolean("play_with_translation", value).apply()

    // Bookmarked Surah ID (-1 if none)
    var bookmarkedSurahId: Int
        get() = prefs.getInt("bookmarked_surah_id", -1)
        set(value) = prefs.edit().putInt("bookmarked_surah_id", value).apply()

    // Bookmarked Ayah Number (-1 if none)
    var bookmarkedAyahNum: Int
        get() = prefs.getInt("bookmarked_ayah_num", -1)
        set(value) = prefs.edit().putInt("bookmarked_ayah_num", value).apply()
}
