package com.haseeb.quranapp.ui.audio

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

import ir.mahozad.multiplatform.wavyslider.material3.WavySlider

@Composable
fun AudioPlayerSheet(
    viewModel: AudioViewModel = hiltViewModel(),
    surahId: Int,
    isExpanded: Boolean = false
) {
    val isPlaying by viewModel.isPlaying.collectAsState()
    val duration by viewModel.duration.collectAsState()
    val position by viewModel.currentPosition.collectAsState()
    val currentAyahIndex by viewModel.currentAyahIndex.collectAsState()
    val playingSurahId by viewModel.currentSurahId.collectAsState()

    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        // --- COMPACT HEADER ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = 56.dp)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Surah $surahId",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Ayah ${currentAyahIndex + 1}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            // Compact Play/Pause - Use opacity so it doesn't change Row height
            IconButton(
                onClick = { 
                    if (playingSurahId == surahId) viewModel.togglePlayPause() 
                    else viewModel.playSurah(surahId)
                },
                modifier = Modifier.alpha(if (isExpanded) 0f else 1f)
            ) {
                Icon(
                    imageVector = if (isPlaying && playingSurahId == surahId) Icons.Default.Pause else Icons.Default.PlayArrow, 
                    contentDescription = if (isPlaying) "Pause" else "Play",
                    modifier = Modifier.size(32.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
        
        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
        
        // --- EXPANDED CONTENT ---
        // By always keeping the layout here instead of wrapping in 'if (isExpanded)', 
        // the BottomSheet can accurately calculate the total height,
        // making the drag handle smooth and completely avoiding snaps/hooks!
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                // When collapsed, we can make it zero alpha so clicks don't register out of bound
                .alpha(if (isExpanded) 1f else 0f)
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            // Seekbar
            WavySlider(
                value = if (duration > 0) position.toFloat() / duration.toFloat() else 0f,
                onValueChange = { 
                     val newPos = (it * duration.toFloat()).toLong()
                     viewModel.seekTo(newPos)
                },
                modifier = Modifier.fillMaxWidth()
            )
            
            // Time Labels
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = formatTime(position),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = formatTime(duration),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Play with Translation Toggle
            val playWithTranslation by viewModel.playWithTranslation.collectAsState()
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                shape = MaterialTheme.shapes.medium
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Play with Translation",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Recite translation after each Ayah",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = playWithTranslation,
                        onCheckedChange = { viewModel.togglePlayWithTranslation(it) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = MaterialTheme.colorScheme.primary,
                            checkedTrackColor = MaterialTheme.colorScheme.primaryContainer,
                            uncheckedThumbColor = MaterialTheme.colorScheme.outline,
                            uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Controls
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { viewModel.skipToPrevious() }) {
                    Icon(Icons.Default.SkipPrevious, contentDescription = "Previous", modifier = Modifier.size(32.dp))
                }

                FilledIconButton(
                    onClick = { 
                        if (playingSurahId == surahId) {
                            viewModel.togglePlayPause()
                        } else {
                            viewModel.playSurah(surahId)
                        }
                    },
                    modifier = Modifier.size(64.dp)
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow, 
                        contentDescription = if (isPlaying) "Pause" else "Play",
                        modifier = Modifier.size(32.dp)
                    )
                }

                IconButton(onClick = { viewModel.skipToNext() }) {
                    Icon(Icons.Default.SkipNext, contentDescription = "Next", modifier = Modifier.size(32.dp))
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

fun formatTime(ms: Long): String {
    val totalSeconds = ms / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format("%02d:%02d", minutes, seconds)
}
