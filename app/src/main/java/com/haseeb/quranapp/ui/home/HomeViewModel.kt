package com.haseeb.quranapp.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.haseeb.quranapp.data.local.entity.SurahEntity
import com.haseeb.quranapp.domain.repository.QuranRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repository: QuranRepository,
    private val userPrefs: com.haseeb.quranapp.data.local.prefs.UserPreferences
) : ViewModel() {

    private val _surahs = MutableStateFlow<List<SurahEntity>>(emptyList())
    val surahs: StateFlow<List<SurahEntity>> = _surahs.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _bookmarkedSurahId = MutableStateFlow(userPrefs.bookmarkedSurahId)
    val bookmarkedSurahId: StateFlow<Int> = _bookmarkedSurahId.asStateFlow()

    private val _bookmarkedAyahNum = MutableStateFlow(userPrefs.bookmarkedAyahNum)
    val bookmarkedAyahNum: StateFlow<Int> = _bookmarkedAyahNum.asStateFlow()

    fun refreshBookmarks() {
        _bookmarkedSurahId.value = userPrefs.bookmarkedSurahId
        _bookmarkedAyahNum.value = userPrefs.bookmarkedAyahNum
    }

    init {
        fetchSurahs()
        syncData()
    }

    private fun fetchSurahs() {
        viewModelScope.launch {
            repository.getAllSurahs().collectLatest { list: List<SurahEntity> ->
                _surahs.value = list
            }
        }
    }

    private fun syncData() {
        viewModelScope.launch {
            _isLoading.value = true
            val result = repository.syncQuranData()
            result.onFailure {
                _error.value = "Failed to sync data: ${it.message}"
            }
            _isLoading.value = false
        }
    }
}
