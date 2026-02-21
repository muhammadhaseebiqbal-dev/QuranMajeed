package com.haseeb.quranapp.service

import android.content.Intent
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class AudioService : MediaSessionService() {

    @Inject
    lateinit var player: ExoPlayer

    private var mediaSession: MediaSession? = null

    override fun onCreate() {
        super.onCreate()
        mediaSession = MediaSession.Builder(this, player).build()
        
        // Create notification channel
        val channelId = "audio_channel"
        val channelName = "Quran Audio"
        val importance = android.app.NotificationManager.IMPORTANCE_LOW
        val channel = android.app.NotificationChannel(channelId, channelName, importance)
        val notificationManager = getSystemService(android.app.NotificationManager::class.java)
        notificationManager.createNotificationChannel(channel)

        // Create notification
        val notification = android.app.Notification.Builder(this, channelId)
            .setContentTitle("Quran App")
            .setContentText("Audio Service Running")
            .setSmallIcon(android.R.drawable.ic_media_play)
            .build()

        startForeground(1, notification)
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
        return mediaSession
    }

    override fun onDestroy() {
        mediaSession?.run {
            player.release()
            release()
            mediaSession = null
        }
        super.onDestroy()
    }
}
