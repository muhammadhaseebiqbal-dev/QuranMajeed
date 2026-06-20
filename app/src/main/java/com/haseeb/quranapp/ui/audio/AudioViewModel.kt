package com.haseeb.quranapp.ui.audio

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import android.content.ComponentName
import androidx.core.content.ContextCompat
import com.haseeb.quranapp.data.local.prefs.UserPreferences
import com.haseeb.quranapp.domain.repository.QuranRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AudioViewModel @Inject constructor(
    private val api: com.haseeb.quranapp.data.remote.api.QuranApiService,
    private val userPrefs: UserPreferences,
    private val repository: QuranRepository,
    private val downloadManager: com.haseeb.quranapp.data.download.SurahDownloadManager,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private var player: Player? = null

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _currentSurahId = MutableStateFlow<Int?>(null)
    val currentSurahId: StateFlow<Int?> = _currentSurahId.asStateFlow()
    
    private val _currentAyahIndex = MutableStateFlow(0)
    val currentAyahIndex: StateFlow<Int> = _currentAyahIndex.asStateFlow()
    
    private val _duration = MutableStateFlow(0L)
    val duration: StateFlow<Long> = _duration.asStateFlow()
    
    private val _currentPosition = MutableStateFlow(0L)
    val currentPosition: StateFlow<Long> = _currentPosition.asStateFlow()

    private var _isSeeking = false

    private val _playWithTranslation = MutableStateFlow(userPrefs.playWithTranslation)
    val playWithTranslation: StateFlow<Boolean> = _playWithTranslation.asStateFlow()

    private val prefListener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
        if (key == "translation_id" || key == "reciter_id") {
            val activeSurahId = _currentSurahId.value
            if (activeSurahId != null) {
                val safeAyahIndex = if (_currentAyahIndex.value >= 0) _currentAyahIndex.value else 0
                val wasPlaying = _isPlaying.value
                playSurah(activeSurahId, safeAyahIndex, wasPlaying)
            }
        }
    }

    init {
        userPrefs.registerListener(prefListener)
        
        // Connect to the MediaSessionService using MediaController
        val sessionToken = SessionToken(context, ComponentName(context, com.haseeb.quranapp.service.AudioService::class.java))
        val controllerFuture = MediaController.Builder(context, sessionToken).buildAsync()
        
        controllerFuture.addListener({
            val controller = controllerFuture.get()
            player = controller
            setupPlayerListener(controller)
            
            // Restore state
            if (controller.isPlaying || controller.mediaItemCount > 0) {
                 _isPlaying.value = controller.isPlaying
                 if (controller.mediaItemCount > 1) {
                     _currentAyahIndex.value = controller.currentMediaItemIndex
                 }
                 controller.currentMediaItem?.mediaMetadata?.extras?.getInt("surahId")?.let {
                     _currentSurahId.value = it
                 }
                 _duration.value = controller.duration.coerceAtLeast(0L)
            }
        }, ContextCompat.getMainExecutor(context))
        
        viewModelScope.launch {
            while (true) {
                if (_isPlaying.value && !_isSeeking) {
                    player?.let { p ->
                        _currentPosition.value = p.currentPosition
                        _duration.value = p.duration.coerceAtLeast(0L)
                    }
                }
                kotlinx.coroutines.delay(100)
            }
        }
    }

    private fun setupPlayerListener(p: Player) {
        p.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                _isPlaying.value = isPlaying
            }
            
            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                super.onMediaItemTransition(mediaItem, reason)
                if (p.mediaItemCount > 1) {
                     _currentAyahIndex.value = p.currentMediaItemIndex
                }
                mediaItem?.mediaMetadata?.extras?.getInt("ayahIndex")?.let { _currentAyahIndex.value = it }
                mediaItem?.mediaMetadata?.extras?.getInt("surahId")?.let { _currentSurahId.value = it }
            }

            override fun onEvents(p: Player, events: Player.Events) {
                 if (events.contains(Player.EVENT_PLAYBACK_STATE_CHANGED) || events.contains(Player.EVENT_IS_PLAYING_CHANGED)) {
                     _duration.value = p.duration.coerceAtLeast(0L)
                 }
            }
        })
    }

    fun togglePlayWithTranslation(enabled: Boolean) {
        userPrefs.playWithTranslation = enabled
        _playWithTranslation.value = enabled
        val activeSurahId = _currentSurahId.value
        if (activeSurahId != null) {
            val safeAyahIndex = if (_currentAyahIndex.value >= 0) _currentAyahIndex.value else 0
            val wasPlaying = _isPlaying.value
            playSurah(activeSurahId, safeAyahIndex, wasPlaying)
        }
    }

    fun playSurah(surahId: Int, startAyahIndex: Int = 0, autoPlay: Boolean = true) {
        viewModelScope.launch {
            _currentSurahId.value = surahId
            val langCode = when (userPrefs.translationId) {
                54, 97 -> "ur.khan"
                20, 85, 131 -> "en.walk"
                else -> "en.walk"
            }

            val surahAyahCounts = intArrayOf(
                7, 286, 200, 176, 120, 165, 206, 75, 129, 109, 123, 111, 43, 52, 99, 128, 111, 110, 98, 135,
                112, 78, 118, 64, 77, 227, 93, 88, 69, 60, 34, 30, 73, 54, 45, 83, 182, 88, 75, 85, 54, 53,
                89, 59, 37, 35, 38, 29, 18, 45, 60, 49, 62, 55, 78, 96, 29, 22, 24, 13, 14, 11, 11, 18, 12,
                12, 30, 52, 52, 44, 28, 28, 20, 56, 40, 31, 50, 40, 46, 42, 29, 19, 36, 25, 22, 17, 19, 26,
                30, 20, 15, 21, 11, 8, 8, 19, 5, 8, 8, 11, 11, 8, 3, 9, 5, 4, 7, 3, 6, 3, 5, 4, 5, 6
            )
            val ayahCount = surahAyahCounts.getOrElse(surahId - 1) { 7 }

            if (downloadManager.isSurahDownloaded(surahId)) {
                val mediaItems = mutableListOf<MediaItem>()
                for (i in 0 until ayahCount) {
                    val globalAyahId = getGlobalAyahId(surahId, i)
                    val metadata = MediaMetadata.Builder().setExtras(Bundle().apply { putInt("surahId", surahId); putInt("ayahIndex", i) }).setTitle("Surah  - Ayah ").setArtist("Quran Majeed").build()
                    val cachedFile = downloadManager.getCachedArabicAudio(globalAyahId)
                    if (cachedFile != null) mediaItems.add(MediaItem.Builder().setUri(android.net.Uri.fromFile(cachedFile)).setMediaMetadata(metadata).build())
                    if (_playWithTranslation.value) {
                        val tMetadata = MediaMetadata.Builder().setExtras(Bundle().apply { putInt("surahId", surahId); putInt("ayahIndex", i) }).setTitle("Ayah  Translation").build()
                        val cachedTrans = downloadManager.getCachedTranslationAudio(langCode, globalAyahId)
                        val transUri = if (cachedTrans != null) android.net.Uri.fromFile(cachedTrans) else android.net.Uri.parse("https://cdn.islamic.network/quran/audio/192//.mp3")
                        mediaItems.add(MediaItem.Builder().setUri(transUri).setMediaMetadata(tMetadata).build())
                    }
                }
                loadMediaItems(surahId, mediaItems, startAyahIndex, autoPlay)
                return@launch
            }

            try {
                val response = api.getRecitationByChapter(userPrefs.reciterId, surahId)
                val audioFiles = response.audio_files
                if (audioFiles.isNotEmpty()) {
                    val mediaItems = mutableListOf<MediaItem>()
                    for (i in audioFiles.indices) {
                        val globalAyahId = getGlobalAyahId(surahId, i)
                        val metadata = MediaMetadata.Builder().setExtras(Bundle().apply { putInt("surahId", surahId); putInt("ayahIndex", i) }).setTitle("Surah  - Ayah ").setArtist("Quran Majeed").build()
                        val fullUrl = if (audioFiles[i].url.startsWith("http")) audioFiles[i].url else if (audioFiles[i].url.startsWith("//")) "https:" + audioFiles[i].url else "https://audio.qurancdn.com/" + audioFiles[i].url
                        mediaItems.add(MediaItem.Builder().setUri(fullUrl).setMediaMetadata(metadata).build())
                        if (_playWithTranslation.value) {
                            val tMetadata = MediaMetadata.Builder().setExtras(Bundle().apply { putInt("surahId", surahId); putInt("ayahIndex", i) }).setTitle("Ayah  Translation").build()
                            val bitrate = if (langCode == "ur.khan") "64" else "192"
                            mediaItems.add(MediaItem.Builder().setUri("https://cdn.islamic.network/quran/audio///.mp3").setMediaMetadata(tMetadata).build())
                        }
                    }
                    loadMediaItems(surahId, mediaItems, startAyahIndex, autoPlay)
                } else {
                    playFallback(surahId)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                playFallback(surahId)
            }
        }
    }

    private fun loadMediaItems(surahId: Int, mediaItems: List<MediaItem>, startAyahIndex: Int, autoPlay: Boolean) {
        player?.let { p ->
            if (startAyahIndex > 0) {
                val targetIndex = if (_playWithTranslation.value) startAyahIndex * 2 else startAyahIndex
                if (targetIndex < mediaItems.size) {
                    p.setMediaItems(mediaItems, targetIndex, 0L)
                } else {
                    p.setMediaItems(mediaItems)
                }
            } else {
                p.setMediaItems(mediaItems)
            }
            p.prepare()
            if (autoPlay) p.play()
        }
    }

    private fun playFallback(surahId: Int) {
         val formattedId = String.format("%03d", surahId)
         val url = "https://download.quranicaudio.com/quran/mishari_rashid_al_3afaasee/.mp3"
         val metadata = MediaMetadata.Builder().setExtras(Bundle().apply { putInt("surahId", surahId) }).setTitle("Surah ").build()
         val mediaItem = MediaItem.Builder().setUri(url).setMediaMetadata(metadata).build()
         player?.let { p ->
             p.setMediaItem(mediaItem)
             p.prepare()
             p.play()
         }
    }

    fun togglePlayPause() {
        player?.let { p ->
            if (_isPlaying.value) {
                p.pause()
            } else {
                p.play()
            }
        }
    }
    
    fun seekTo(position: Long) {
        _isSeeking = true
        _currentPosition.value = position
        player?.seekTo(position)
    }

    fun onSeekFinished() {
        _isSeeking = false
    }
    
    fun skipToNext() {
        player?.let { p ->
            if (_playWithTranslation.value) {
                val nextIndex = p.currentMediaItemIndex + (2 - p.currentMediaItemIndex % 2)
                if (nextIndex < p.mediaItemCount) p.seekToDefaultPosition(nextIndex)
            } else if (p.hasNextMediaItem()) {
                p.seekToNextMediaItem()
            }
        }
    }
    
    fun skipToPrevious() {
        player?.let { p ->
            if (_playWithTranslation.value) {
                var prevIndex = p.currentMediaItemIndex - (p.currentMediaItemIndex % 2) - 2
                if (prevIndex < 0) prevIndex = 0
                if (prevIndex < p.mediaItemCount) p.seekToDefaultPosition(prevIndex)
            } else if (p.hasPreviousMediaItem()) {
                 p.seekToPreviousMediaItem()
            }
        }
    }

    fun seekToAyah(ayahIndex: Int) {
        val targetIndex = if (_playWithTranslation.value) ayahIndex * 2 else ayahIndex
        player?.let { p ->
            if (targetIndex >= 0 && targetIndex < p.mediaItemCount) {
                p.seekToDefaultPosition(targetIndex)
                if (!isPlaying.value) p.play()
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        userPrefs.unregisterListener(prefListener)
        // If we want to release the controller
        if (player is MediaController) {
            (player as MediaController).release()
        }
    }

    private fun getGlobalAyahId(surahId: Int, ayahIndex: Int): Int {
        val surahAyahCounts = intArrayOf(
            7, 286, 200, 176, 120, 165, 206, 75, 129, 109, 123, 111, 43, 52, 99, 128, 111, 110, 98, 135,
            112, 78, 118, 64, 77, 227, 93, 88, 69, 60, 34, 30, 73, 54, 45, 83, 182, 88, 75, 85, 54, 53,
            89, 59, 37, 35, 38, 29, 18, 45, 60, 49, 62, 55, 78, 96, 29, 22, 24, 13, 14, 11, 11, 18, 12,
            12, 30, 52, 52, 44, 28, 28, 20, 56, 40, 31, 50, 40, 46, 42, 29, 19, 36, 25, 22, 17, 19, 26,
            30, 20, 15, 21, 11, 8, 8, 19, 5, 8, 8, 11, 11, 8, 3, 9, 5, 4, 7, 3, 6, 3, 5, 4, 5, 6
        )
        var globalId = 0
        for (i in 0 until (surahId - 1)) globalId += surahAyahCounts[i]
        return globalId + ayahIndex + 1
    }
}
