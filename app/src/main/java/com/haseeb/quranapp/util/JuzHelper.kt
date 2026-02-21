package com.haseeb.quranapp.util

object JuzHelper {
    // Map of Juz ID to Start Verse Key ("Surah:Ayah")
    // This is a simplified map. ideally we would have exact start/end.
    // However, for assigning Juz numbers to Ayahs, we need to know the range.
    
    data class JuzStart(val juzId: Int, val surahId: Int, val ayahId: Int)

    val juzStarts = listOf(
        JuzStart(1, 1, 1),
        JuzStart(2, 2, 142),
        JuzStart(3, 2, 253),
        JuzStart(4, 3, 93),
        JuzStart(5, 4, 24),
        JuzStart(6, 4, 148),
        JuzStart(7, 5, 82),
        JuzStart(8, 6, 111),
        JuzStart(9, 7, 88),
        JuzStart(10, 8, 41),
        JuzStart(11, 9, 93),
        JuzStart(12, 11, 6),
        JuzStart(13, 12, 53),
        JuzStart(14, 15, 1),
        JuzStart(15, 17, 1),
        JuzStart(16, 18, 75),
        JuzStart(17, 21, 1),
        JuzStart(18, 23, 1),
        JuzStart(19, 25, 21),
        JuzStart(20, 27, 56),
        JuzStart(21, 29, 46),
        JuzStart(22, 33, 31),
        JuzStart(23, 36, 28),
        JuzStart(24, 39, 32),
        JuzStart(25, 41, 47),
        JuzStart(26, 46, 1),
        JuzStart(27, 51, 31),
        JuzStart(28, 58, 1),
        JuzStart(29, 67, 1),
        JuzStart(30, 78, 1)
    )

    fun getJuzForAyah(surahId: Int, ayahId: Int): Int {
        // Find the juz where (surahId, ayahId) >= JuzStart
        // Since the list is ordered, we can find the last one that matches.
        var currentJuz = 1
        for (juz in juzStarts) {
            if (surahId > juz.surahId || (surahId == juz.surahId && ayahId >= juz.ayahId)) {
                currentJuz = juz.juzId
            } else {
                break
            }
        }
        return currentJuz
    }
}
