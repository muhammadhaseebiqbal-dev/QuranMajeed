package com.haseeb.quranapp.ui.audio

import android.content.Context
import android.os.Bundle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.haseeb.quranapp.data.local.prefs.UserPreferences
import com.haseeb.quranapp.domain.repository.QuranRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AudioViewModel @Inject constructor(
    val player: ExoPlayer,
    private val api: com.haseeb.quranapp.data.remote.api.QuranApiService,
    private val userPrefs: UserPreferences,
    private val repository: QuranRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _currentSurahId = MutableStateFlow<Int?>(null)
    val currentSurahId: StateFlow<Int?> = _currentSurahId.asStateFlow()
    
    // Karaoke Sync
    private val _currentAyahIndex = MutableStateFlow(0)
    val currentAyahIndex: StateFlow<Int> = _currentAyahIndex.asStateFlow()
    
    // Playback Progress
    private val _duration = MutableStateFlow(0L)
    val duration: StateFlow<Long> = _duration.asStateFlow()
    
    private val _currentPosition = MutableStateFlow(0L)
    val currentPosition: StateFlow<Long> = _currentPosition.asStateFlow()

    init {
        player.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                _isPlaying.value = isPlaying
            }
            
            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                super.onMediaItemTransition(mediaItem, reason)
                if (player.mediaItemCount > 1) {
                     _currentAyahIndex.value = player.currentMediaItemIndex
                }
                
                mediaItem?.mediaMetadata?.extras?.getInt("ayahIndex")?.let {
                    _currentAyahIndex.value = it
                }
                
                mediaItem?.mediaMetadata?.extras?.getInt("surahId")?.let {
                    _currentSurahId.value = it
                }
            }

            override fun onEvents(player: Player, events: Player.Events) {
                 if (events.contains(Player.EVENT_PLAYBACK_STATE_CHANGED) || events.contains(Player.EVENT_IS_PLAYING_CHANGED)) {
                     _duration.value = player.duration.coerceAtLeast(0L)
                 }
            }
        })
        
        // Polling for progress (Very fast for fluid seekbar)
        viewModelScope.launch {
            while (true) {
                if (_isPlaying.value) {
                    _currentPosition.value = player.currentPosition
                }
                kotlinx.coroutines.delay(50)
            }
        }
        
        // Restore state on init if playing
        if (player.isPlaying || player.mediaItemCount > 0) {
             _isPlaying.value = player.isPlaying
             if (player.mediaItemCount > 1) {
                 _currentAyahIndex.value = player.currentMediaItemIndex
             }
             player.currentMediaItem?.mediaMetadata?.extras?.getInt("surahId")?.let {
                 _currentSurahId.value = it
             }
        }
    }

    private val _playWithTranslation = MutableStateFlow(userPrefs.playWithTranslation)
    val playWithTranslation: StateFlow<Boolean> = _playWithTranslation.asStateFlow()

    fun togglePlayWithTranslation(enabled: Boolean) {
        userPrefs.playWithTranslation = enabled
        _playWithTranslation.value = enabled
        
        // If a Surah is currently active, immediately reload playback to reflect the toggle
        val activeSurahId = _currentSurahId.value
        if (activeSurahId != null) {
            val safeAyahIndex = if (_currentAyahIndex.value >= 0) _currentAyahIndex.value else 0
            playSurah(activeSurahId, safeAyahIndex)
        }
    }

    fun playSurah(surahId: Int, startAyahIndex: Int = 0) {
        viewModelScope.launch {
            _currentSurahId.value = surahId
            
            try {
                // Fetch Ayah entities to get the Global IDs required for Translation Audio
                val surahAyahs = repository.getAyahsBySurah(surahId).firstOrNull() ?: emptyList()

                val reciterId = userPrefs.reciterId
                val response = api.getRecitationByChapter(reciterId, surahId)
                val audioFiles = response.audio_files

                if (audioFiles.isNotEmpty()) {
                    val mediaItems = mutableListOf<MediaItem>()
                    val langCode = if (userPrefs.translationId in listOf(54, 97, 831, 158)) "ur.khan" else "en.walk"

                    for (i in audioFiles.indices) {
                        val file = audioFiles[i]
                        val globalAyahId = surahAyahs.getOrNull(i)?.id ?: 1

                        val metadata = MediaMetadata.Builder()
                            .setExtras(Bundle().apply { 
                                putInt("surahId", surahId)
                                putInt("ayahIndex", i) 
                            })
                            .setTitle("Ayah ${i + 1}")
                            .build()
                            
                        val fullUrl = if (file.url.startsWith("http")) file.url
                            else if (file.url.startsWith("//")) "https:" + file.url
                            else "https://audio.qurancdn.com/" + file.url
                            
                        mediaItems.add(
                            MediaItem.Builder()
                                .setUri(fullUrl)
                                .setMediaMetadata(metadata)
                                .build()
                        )

                        if (_playWithTranslation.value) {
                            val tMetadata = MediaMetadata.Builder()
                                .setExtras(Bundle().apply { 
                                    putInt("surahId", surahId)
                                    putInt("ayahIndex", i) 
                                })
                                .setTitle("Ayah ${i + 1} Translation")
                                .build()
                            
                            val bitrate = if (langCode == "ur.khan") "64" else "192"
                            val tFullUrl = "https://cdn.islamic.network/quran/audio/$bitrate/$langCode/$globalAyahId.mp3"
                            
                            mediaItems.add(
                                MediaItem.Builder()
                                    .setUri(tFullUrl)
                                    .setMediaMetadata(tMetadata)
                                    .build()
                            )
                        }
                    }

                    if (startAyahIndex > 0) {
                        val targetIndex = if (_playWithTranslation.value) startAyahIndex * 2 else startAyahIndex
                        if (targetIndex < mediaItems.size) {
                            player.setMediaItems(mediaItems, targetIndex, 0L)
                        } else {
                            player.setMediaItems(mediaItems)
                        }
                    } else {
                        player.setMediaItems(mediaItems)
                    }

                    player.prepare()
                    player.play()
                } else {
                    playFallback(surahId)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                playFallback(surahId)
            }
        }
    }

    private fun playFallback(surahId: Int) {
         val formattedId = String.format("%03d", surahId)
         val url = "https://download.quranicaudio.com/quran/mishari_rashid_al_3afaasee/$formattedId.mp3"
         
         val metadata = androidx.media3.common.MediaMetadata.Builder()
            .setExtras(android.os.Bundle().apply { putInt("surahId", surahId) })
            .setTitle("Surah $surahId")
            .build()
            
         val mediaItem = MediaItem.Builder()
            .setUri(url)
            .setMediaMetadata(metadata)
            .build()
            
         player.setMediaItem(mediaItem)
         player.prepare()
         player.play()
    }

    fun togglePlayPause() {
        if (_isPlaying.value) {
            player.pause()
            _isPlaying.value = false
        } else {
            player.play()
            _isPlaying.value = true
        }
    }
    
    fun seekTo(position: Long) {
        player.seekTo(position)
    }
    
    fun skipToNext() {
        if (_playWithTranslation.value) {
            val nextIndex = player.currentMediaItemIndex + (2 - player.currentMediaItemIndex % 2)
            if (nextIndex < player.mediaItemCount) {
                player.seekToDefaultPosition(nextIndex)
            }
        } else if (player.hasNextMediaItem()) {
            player.seekToNextMediaItem()
        }
    }
    
    fun skipToPrevious() {
        if (_playWithTranslation.value) {
            var prevIndex = player.currentMediaItemIndex - (player.currentMediaItemIndex % 2) - 2
            if (prevIndex < 0) prevIndex = 0
            if (prevIndex < player.mediaItemCount) {
                player.seekToDefaultPosition(prevIndex)
            }
        } else if (player.hasPreviousMediaItem()) {
             player.seekToPreviousMediaItem()
        }
    }

    fun seekToAyah(ayahIndex: Int) {
        val targetIndex = if (_playWithTranslation.value) {
            ayahIndex * 2 // Interleaved with translations
        } else {
            ayahIndex
        }
        
        if (targetIndex >= 0 && targetIndex < player.mediaItemCount) {
            player.seekToDefaultPosition(targetIndex)
            if (!isPlaying.value) {
                player.play()
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
    }
}
