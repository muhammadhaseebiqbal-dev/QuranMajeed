package com.haseeb.quranapp.ui.audio

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

@Composable
fun AudioControlBar(
    viewModel: AudioViewModel = hiltViewModel(),
    surahId: Int
) {
    val isPlaying by viewModel.isPlaying.collectAsState()

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(80.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        tonalElevation = 8.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Now Playing Surah $surahId",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )

            FloatingActionButton(
                onClick = { 
                    if (!isPlaying) viewModel.playSurah(surahId) else viewModel.togglePlayPause() 
                },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                // Placeholder for Pause icon since we only imported default icons
                Icon(
                    imageVector = Icons.Default.PlayArrow, 
                    contentDescription = if (isPlaying) "Pause" else "Play"
                )
            }
        }
    }
}
