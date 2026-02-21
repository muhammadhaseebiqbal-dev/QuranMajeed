package com.haseeb.quranapp.ui.audio

import androidx.compose.runtime.Composable
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel

// Temporary workaround to access AudioViewModel in purely composable context if needed
// or just use hiltViewModel() in the Composables.
object AudioUtils {
    @Composable
    fun getViewModel(): AudioViewModel = hiltViewModel()
}
