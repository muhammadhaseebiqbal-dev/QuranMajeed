package com.haseeb.quranapp.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.haseeb.quranapp.data.local.prefs.UserPreferences
import com.haseeb.quranapp.domain.repository.QuranRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SelectionOption(val id: Int, val name: String)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val userPrefs: UserPreferences,
    private val repository: QuranRepository
) : ViewModel() {

    private val _selectedReciterId = MutableStateFlow(userPrefs.reciterId)
    val selectedReciterId: StateFlow<Int> = _selectedReciterId.asStateFlow()

    private val _selectedTranslationId = MutableStateFlow(userPrefs.translationId.takeIf { it != 131 } ?: 54)
    val selectedTranslationId: StateFlow<Int> = _selectedTranslationId.asStateFlow()

    private val _selectedTafsirId = MutableStateFlow(userPrefs.tafsirId)
    val selectedTafsirId: StateFlow<Int> = _selectedTafsirId.asStateFlow()

    // Predefined popular options based on API testing
    val reciterOptions = listOf(
        SelectionOption(10, "Saud ash-Shuraym (Arabic) (Default)"),
        SelectionOption(7, "Mishari Rashid Al-Afasy (Arabic)"),
        SelectionOption(2, "AbdulBaset AbdulSamad (Arabic)"),
        SelectionOption(3, "Abdur-Rahman as-Sudais (Arabic)"),
        SelectionOption(4, "Abu Bakr al-Shatri (Arabic)")
    )

    val translationOptions = listOf(
        SelectionOption(54, "Urdu (Jalandhry) (Default)"),
        SelectionOption(97, "Urdu (Tahir ul Qadri)"),
        SelectionOption(20, "English (Saheeh International)"),
        SelectionOption(85, "English (Pickthall)")
    )

    val tafsirOptions = listOf(
        SelectionOption(160, "Urdu (Tafsir Ibn Kathir) (Default)"),
        SelectionOption(159, "Urdu (Bayan ul Quran)"),
        SelectionOption(169, "English (Tafsir Ibn Kathir)"),
        SelectionOption(16, "Arabic (Tafsir Muyassar)")
    )

    fun updateReciter(id: Int) {
        userPrefs.reciterId = id
        _selectedReciterId.value = id
    }

    fun updateTranslation(id: Int) {
        userPrefs.translationId = id
        _selectedTranslationId.value = id
        
        // Trigger background sync for the new translation
        viewModelScope.launch {
            repository.syncQuranData()
        }
    }

    fun updateTafsir(id: Int) {
        userPrefs.tafsirId = id
        _selectedTafsirId.value = id
    }
}
