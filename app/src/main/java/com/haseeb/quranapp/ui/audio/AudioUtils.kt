package com.haseeb.quranapp.ui.audio

import android.content.Context
import android.content.ContextWrapper
import androidx.activity.ComponentActivity
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel

object AudioUtils {
    @Composable
    fun getViewModel(): AudioViewModel {
        val context = LocalContext.current
        val activity = context.getActivity() ?: error("AudioViewModel needs ComponentActivity")
        return hiltViewModel(activity)
    }

    private fun Context.getActivity(): ComponentActivity? = when (this) {
        is ComponentActivity -> this
        is ContextWrapper -> baseContext.getActivity()
        else -> null
    }
}
