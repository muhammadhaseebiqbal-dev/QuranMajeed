package com.haseeb.quranapp.ui.reader

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.haseeb.quranapp.data.local.entity.AyahEntity
import com.haseeb.quranapp.domain.repository.QuranRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ReaderViewModel @Inject constructor(
    private val repository: QuranRepository,
    savedStateHandle: SavedStateHandle,
    private val userPrefs: com.haseeb.quranapp.data.local.prefs.UserPreferences
) : ViewModel() {

    private val _ayahs = MutableStateFlow<List<AyahEntity>>(emptyList())
    val ayahs: StateFlow<List<AyahEntity>> = _ayahs.asStateFlow()

    init {
        val surahId = savedStateHandle.get<Int>("surahId")
        if (surahId != null && surahId != -1) {
            fetchVerses(surahId)
        } else {
            val juzId = savedStateHandle.get<Int>("juzId")
            if (juzId != null && juzId != -1) {
                fetchVersesByJuz(juzId)
            }
        }
    }

    private fun fetchVerses(surahId: Int) {
        viewModelScope.launch {
            repository.getAyahsBySurah(surahId).collectLatest { list ->
                _ayahs.value = list
            }
        }
    }

    private fun fetchVersesByJuz(juzId: Int) {
        viewModelScope.launch {
            repository.getAyahsByJuz(juzId).collectLatest { list ->
                _ayahs.value = list
            }
        }
    }

    private val _tafsirState = MutableStateFlow<String?>(null)
    val tafsirState: StateFlow<String?> = _tafsirState.asStateFlow()

    fun fetchTafsir(surahId: Int, ayahNum: Int) {
        viewModelScope.launch {
            _tafsirState.value = "Loading Tafsir..."
            val verseKey = "$surahId:$ayahNum"
            repository.getTafsir(verseKey).fold(
                onSuccess = { _tafsirState.value = it },
                onFailure = { _tafsirState.value = "Failed to load Tafsir: ${it.message}" }
            )
        }
    }

    fun clearTafsir() {
        _tafsirState.value = null
    }

    private val _bookmarkedSurahId = MutableStateFlow(userPrefs.bookmarkedSurahId)
    val bookmarkedSurahId: StateFlow<Int> = _bookmarkedSurahId.asStateFlow()

    private val _bookmarkedAyahNum = MutableStateFlow(userPrefs.bookmarkedAyahNum)
    val bookmarkedAyahNum: StateFlow<Int> = _bookmarkedAyahNum.asStateFlow()

    fun toggleBookmark(surahId: Int, ayahNum: Int) {
        if (_bookmarkedSurahId.value == surahId && _bookmarkedAyahNum.value == ayahNum) {
            // Un-bookmark
            userPrefs.bookmarkedSurahId = -1
            userPrefs.bookmarkedAyahNum = -1
            _bookmarkedSurahId.value = -1
            _bookmarkedAyahNum.value = -1
        } else {
            // Bookmark
            userPrefs.bookmarkedSurahId = surahId
            userPrefs.bookmarkedAyahNum = ayahNum
            _bookmarkedSurahId.value = surahId
            _bookmarkedAyahNum.value = ayahNum
        }
    }
}
